
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This visitor class generates Jasmin code for the parse tree it is visiting
 * @author morin
 *
 */
public class CMMJasminVisitor implements CMMVisitor<Integer, List<String>> {

	static int BOOLEAN = 0;
	static int NUMBER = 1;
	static int STRING = 2;
	static int FUNCTION = 3;

	static String[] t2a = { "i", "f", "a", "XXX" };
	static String[] t2A = { "I", "F", "Ljava/lang/String;", "XXX" };
	
	static String[] header = {
		"; Begin standard header",
		".class public a",
		".super java/lang/Object",
		".method public <init>()V",
		"  aload_0\n",
		"  invokenonvirtual java/lang/Object/<init>()V",
		"  return",
		".end method",
		".method public static main([Ljava/lang/String;)V",
		"  invokestatic a/main()F",
		"  return",
		".end method",
		"; End standard header" };
	
	
	static String[] trailer = {
		"; Begin standard trailer",
		"",
		".method public static print(F)F",
		"   .limit stack 2",
		"   .limit locals 1",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   fload 0",
		"   invokestatic java/lang/Float/toString(F)Ljava/lang/String;",
		"   invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V",
		"   fload 0",
		"   freturn",
		".end method",
		"",
		".method public static print(Ljava/lang/String;)Ljava/lang/String;",
		"   .limit stack 2",
		"   .limit locals 2",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   aload 0",
		"   invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V",
		"   aload 0",
		"   areturn",
		".end method",
		"",
		".method public static print(I)I",
		"   .limit stack 5",
		"   .limit locals 2",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   iload 0",
		"   ifeq false_label",
		"   ldc \"true\"",
		"   goto print_it",
		"false_label:",
		"   ldc \"false\"",
		"print_it:",
		"   invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V",
		"   iload 0",
		"   ireturn",
		".end method",
		"",
		"",
		".method public static println(F)F",
		"   .limit stack 2",
		"   .limit locals 1",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   fload 0",
		"   invokestatic java/lang/Float/toString(F)Ljava/lang/String;",
		"   invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V",
		"   fload 0",
		"   freturn",
		".end method",
		"",
		".method public static println(Ljava/lang/String;)Ljava/lang/String;",
		"   .limit stack 2",
		"   .limit locals 2",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   aload 0",
		"   invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V",
		"   aload 0",
		"   areturn",
		".end method",
		"",
		".method public static println(I)I",
		"   .limit stack 5",
		"   .limit locals 2",
		"   getstatic java/lang/System/out Ljava/io/PrintStream;",
		"   iload 0",
		"   ifeq false_label",
		"   ldc \"true\"",
		"   goto print_it",
		"false_label:",
		"   ldc \"false\"",
		"print_it:",
		"   invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V",
		"   iload 0",
		"   ireturn",
		".end method",
		"",
		"; End of standard trailer"	};

	protected class Data {
		int location;
		int type;
		String name;
		public Data(String name, int type, int location) {
			this.name = name;
			this.type = type;
			this.location = location;
		}
	}
	
	int uniq;
	
	protected String getLabel() {
		return "label" + uniq++;
	}
	
	protected int s2t(String type) {
		if (type.equals("number_t"))
			return NUMBER;
		if (type.equals("string_t"))
			return STRING;
		if (type.equals("boolean_t"))
			return BOOLEAN;
		return -1;
	}
	
	protected class StackFrame {
		Map<String, Data> map;
		protected int offset;
		
		public StackFrame() {
			map = new HashMap<String, Data>();
			offset = 0;
		}

		public StackFrame(StackFrame parent) {
			this();
			offset = parent.offset;
		}

		public void addVariable(String name, int type) {
			map.put(name, new Data(name, type, offset++));
		}
		
		public void addPseudoVariable(String name, int type) {
			map.put(name, new Data(name, type, -1));			
		}
		
		public void addFunction(String sig, String fullName, int retType) {
			map.put(sig, new Data(fullName, retType, -1));
		}
		
		public Data lookup(String name) {
			return map.containsKey(name) ? map.get(name) : null;
		}
	}
	
	protected Stack<StackFrame> frames;
	
	
	protected  Data lookup(String name) {
		Data data;
		for (StackFrame f : frames) {
			if ((data = f.lookup(name)) != null) 
				return data;
		}
		return null;
	}
	
	public CMMJasminVisitor() {
		frames = new Stack<StackFrame>();
		frames.push(new StackFrame());
		frames.peek().addFunction("print(I)", "print(I)I", BOOLEAN);
		frames.peek().addFunction("print(F)", "print(F)F", NUMBER);
		frames.peek().addFunction("print(Ljava/lang/String;)", "print(Ljava/lang/String;)Ljava/lang/String;", STRING);
		frames.peek().addFunction("println(I)", "println(I)I", BOOLEAN);
		frames.peek().addFunction("println(F)", "println(F)F", NUMBER);
		frames.peek().addFunction("println(Ljava/lang/String;)", "println(Ljava/lang/String;)Ljava/lang/String;", STRING);
	}
	
	public Integer visit(CMMASTNode node, List<String> output) {
		return null;
	}

	// Parameter -> Type id
	public Integer visit(CMMASTParameterNode node, List<String> output) {
		String name = node.getChild(1).getValue();
		String sType = node.getChild(0).getChild(0).getName();
		int type = s2t(sType);
		frames.peek().addVariable(name, type);
		return null;
	}

	// Sum -> Term ((plus|minus) Term)*  [>1]
	public Integer visit(CMMASTSumNode node, List<String> output) {
		node.getChild(0).accept(this, output);
		for (int i = 1; i < node.numChildren(); i += 2) {
			node.getChild(i+1).accept(this, output);
			String op = node.getChild(i).getName();
			if (op.equals("plus")) {
				output.add("  fadd");
			} else if (op.equals("minus")) {
				output.add("  fsub");
			} else {
				throw new RuntimeException("Unknown operator:" + op);
			}
		}
		return NUMBER;
	}

	public Integer visit(CMMASTLogicalNode node, List<String> output) {
		node.getChild(0).accept(this, output);
		for (int i = 1; i < node.numChildren(); i += 2) {
			node.getChild(i+1).accept(this, output);
			String op = node.getChild(i).getName();
			if (op.equals("and")) {
				output.add("  iand");
			} else if (op.equals("or")) {
				output.add("  ior");
			} else {
				throw new RuntimeException("Unknown operator:" + op);
			}
		}
		return BOOLEAN;
	}

	public Integer visit(CMMASTComparisonNode node, List<String> output) {
		node.getChild(0).accept(this, output);
		for (int i = 1; i < node.numChildren(); i += 2) {
			node.getChild(i+1).accept(this, output);
			String op = node.getChild(i).getName();
			String label1 = getLabel();
			String label2 = getLabel();
			output.add("  fcmpl");
			output.add("  if" + op + " " + label1);
			output.add("  ldc 0");
			output.add("  goto " + label2);
			output.add(label1 + ":");
			output.add("  ldc 1");
			output.add(label2 + ":");
		}
		return BOOLEAN;
	}

	public Integer visit(CMMASTTermNode node, List<String> output) {
		// TODO Auto-generated method stub
		return NUMBER;
	}

	public Integer visit(CMMASTExpNode node, List<String> output) {
		// TODO Auto-generated method stub
		return NUMBER;
	}

	public Integer visit(CMMASTConditionNode node, List<String> output) {
		Integer t = node.getChild(1).accept(this, output);
		if (t != BOOLEAN) 
			throw new RuntimeException("Condition not evaluating to boolean");
		return null;
	}


	/**
	 * Recursively visit all the children of a node
	 * @param node
	 * @param output
	 */
	protected Integer visitChildren(CMMASTNode node, List<String> output) {
		Integer t = null;
		for (int i = 0; i < node.numChildren(); i++) {
			Integer t2 = node.getChild(i).accept(this, output);
			if (t2 != null) t = t2;
		}
		return t;
	}
	
	public Integer visit(CMMASTSimpleStatementNode node, List<String> output) {
		return visitChildren(node, output);
	}

	public Integer visit(CMMASTConstantNode node, List<String> output) {
		return visitChildren(node, output);
	}


	public Integer visit(CMMASTParameterListNode node, List<String> output) {
		visitChildren(node, output);
		return null;
	}

	public Integer visit(CMMASTArgumentListNode node, List<String> output) {
		throw new RuntimeException("Internet error - evaluating argument list");
	}

	public Integer visit(CMMASTElementNode node, List<String> output) {
		return visitChildren(node, output);
	}

	public Integer visit(CMMASTExpressionListNode node, List<String> output) {
		visitChildren(node, output);
		return null;
	}

	public Integer visit(CMMASTElementPlusNode node, List<String> output) {
		if (node.numChildren() == 1) { // just an identifier
			return node.getChild(0).accept(this, output); 
		} else { // a function call
			String fname = node.getChild(0).getValue();
			CMMASTNode argList = node.getChild(1);
			String sig = "";
			for (int i = 1; i < argList.numChildren()-1; i += 2) {
				int t = argList.getChild(i).accept(this, output);
				sig += t2A[t];
			}
			fname += "(" + sig + ")";
			Data f = lookup(fname);
			if (f == null)
				throw new RuntimeException("Attempt to call non-existent function: " + fname);
			output.add("  invokestatic a/" + f.name);
			// TODO: figure out the return type here
			return f.type;
		}
	}

	// WhileLoop -> while Condition Block
	public Integer visit(CMMASTWhileLoopNode node, List<String> output) {
		// TODO
		throw new UnsupportedOperationException("while loops not yet implemented");
		// return null
	}

	public Integer visit(CMMASTDoLoopNode node, List<String> output) {
		throw new UnsupportedOperationException("do-while loops not yet implemented");
		// return null
	}

	public Integer visit(CMMASTReturnStatementNode node, List<String> output) {
		node.getChild(1).accept(this, output);
		Data r = lookup("22retval");
		output.add("  " + t2a[r.type] + "return");
		return null;
	}

	/**
	 * Extract a Jasmin method signature from a ParameterList node
	 */
	// ParameterList -> lparen (Parameter (listsep Parameter)*)? rparen
	// Parameter -> Type id
	protected String getSignature(CMMASTParameterListNode node) {
		String sig = "";
		for (int i = 1; i < node.numChildren()-1; i += 2) {
			int it = s2t(node.getChild(i).getChild(0).getChild(0).getName());
			sig += t2A[it];
		}
		return sig;
	}
	
	// FunctionDefinition -> Type id ParameterList Block
	public Integer visit(CMMASTFunctionDefinitionNode node, List<String> output) {
		String fname = node.getChild(1).getValue();
		String stype = node.getChild(0).getChild(0).getName();
		int itype = s2t(stype);
		// build method signature
		String sig = fname + "(" 
			+ getSignature((CMMASTParameterListNode)node.getChild(2)) + ")";
		String fullName = sig + t2A[itype];
		output.add("\n.method public static " + fullName);
		output.add(".limit stack 50");
		output.add(".limit locals 50");
		frames.peek().addFunction(sig, fullName, itype);
		frames.push(new StackFrame());
		frames.peek().addPseudoVariable("22retval", itype);
		node.getChild(2).accept(this, output);   // parameter list
		node.getChild(3).accept(this, output);   // block
		frames.pop();
		output.add(".end method");
		return null;
	}

	public Integer visit(CMMASTAssignmentNode node, List<String> output) {
		if (node.numChildren() == 1) {  // not really an assignment
			return visitChildren(node, output);
		} else {
			CMMASTNode n = node.getChild(0);  // Element
			if (!n.getName().equals("Element") || n.numChildren() != 1)
				throw new RuntimeException("Assigning to non-lvalue");
			n = n.getChild(0);   // ElementPlus
			if (!n.getName().equals("ElementPlus") || n.numChildren() != 1) 
				throw new RuntimeException("Assigning to non-lvalue");
			n = n.getChild(0);   // Token
			if (!n.getName().equals("id"))
				throw new RuntimeException("Assigning to non-lvalue");
			String id = n.getValue();
			Data data = lookup(id);
			if (data == null)
				throw new RuntimeException("Assigning to undeclared variable " + id);
			node.getChild(2).accept(this, output);
			output.add("  dup");
			output.add("  " + t2a[data.type] + "store " + data.location + "   ; " + id);
			return data.type;
		}
	}

	public Integer visit(CMMASTStatementNode node, List<String> output) {
		if (visitChildren(node, output) != null)
			output.add("  pop");
		return null;
	}

	@Override
	public Integer visit(CMMASTTypeNode node, List<String> output) {
		throw new RuntimeException("Internal error: visiting Type node");
	}

	public Integer visit(CMMASTIfStatementNode node, List<String> output) {
		throw new UnsupportedOperationException("if statements are not yet implemented");
		// return null
	}

	// Declaration -> Type Identifier (listsep Identifier)* eol
	public Integer visit(CMMASTDeclarationNode node, List<String> output) {
		CMMASTNode type = node.getChild(0);
		String stype = type.getChild(0).getName();
		int itype = s2t(stype);
		for (int i = 1; i < node.numChildren(); i += 2)
			frames.peek().addVariable(node.getChild(i).getValue(), itype);
		return null;
	}

	public Integer visit(CMMASTProgramNode node, List<String> output) {
		output.addAll(Arrays.asList(header));
		visitChildren(node, output);
		output.addAll(Arrays.asList(trailer));
		return null;
	}

	public Integer visit(CMMASTBlockNode node, List<String> output) {
		frames.push(new StackFrame(frames.peek()));
		visitChildren(node, output);
		frames.pop();
		return null;
	}

	public Integer visit(CMMASTToken node, List<String> output) {
		if (node.getName().equals("number")) {
			output.add("  ldc " + Float.valueOf(node.getValue()));
			return NUMBER;
		} else if (node.getName().equals("string")) {
			output.add("  ldc " + node.getValue());
			return STRING;
		} else if (node.getName().equals("boolean")) {
			output.add("  ldc " + (node.getValue().equals("true")));
			return BOOLEAN;
		} else if (node.getName().equals("id")) {
			Data data = lookup(node.getValue());
			output.add("  " + t2a[data.type] + "load " + data.location + "   ;" + node.getValue());
			return data.type;
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Reader r = null;
		if (args.length == 0) {
			r = new InputStreamReader(System.in);
		} else {
			try {
				r = new FileReader(args[0]);
			} catch (IOException e) {
				System.err.println("Error occurred while opening input file " + args[0]);
				System.err.println(e);
				System.exit(-1);
			}
		}
		CMMTokenizer t = new CMMTokenizer(r);
		CMMParser p = new CMMParser(t);
		CMMASTNode n = null;
		System.out.print("Parsing...");
		System.out.flush();
		try {
			n = p.parse();
		} catch (CMMTokenizerException e) {
			System.err.println("A tokenizer exception occured:" + e);
			System.exit(-1);
		} catch (CMMParserException e) {
			System.err.println("A parse exception occured:" + e);
			System.exit(-1);
		}
		System.out.print("compiling...");
		System.out.flush();
		CMMJasminVisitor v = new CMMJasminVisitor();
		List<String> output = new ArrayList<String>();
		n.accept(v, output);
		try {
			PrintStream os = new PrintStream(new FileOutputStream("a.j"));
			for (String l : output) {
				os.println(l);
			}
		} catch (IOException e) {
			System.err.println(e);
		}
		System.out.println("done\nOutput written to a.j");
	}
}
