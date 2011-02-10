
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
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
public class CMMJasminVisitor implements CMMVisitor<List<String>, List<String>> {

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
		
		public void addFunction(String name, String sig) {
			map.put(name, new Data(sig, FUNCTION, 0));
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
	}
	
	public List<String> visit(CMMASTNode node, List<String> output) {
		return output;
	}

	// Parameter -> Type id
	public List<String> visit(CMMASTParameterNode node, List<String> output) {
		String name = node.getChild(1).getValue();
		String sType = node.getChild(0).getChild(0).getName();
		int type = s2t(sType);
		frames.peek().addVariable(name, type);
		return output;
	}

	// Sum -> Term ((plus|minus) Term)*  [>1]
	public List<String> visit(CMMASTSumNode node, List<String> output) {
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
		return output;
	}

	public List<String> visit(CMMASTLogicalNode node, List<String> output) {
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
		return output;
	}

	public List<String> visit(CMMASTComparisonNode node, List<String> output) {
		node.getChild(0).accept(this, output);
		for (int i = 1; i < node.numChildren(); i += 2) {
			node.getChild(i+1).accept(this, output);
			String op = node.getChild(i).getName();
			String label1 = "label1";
			String label2 = "label2";
			output.add("  fcmpl");
			output.add("  if" + op + " " + label1);
			output.add("  ldc 0");
			output.add("  goto " + label2);
			output.add(label1 + ":");
			output.add("  ldc 1");
			output.add(label2 + ":");
		}
		return output;
	}

	public List<String> visit(CMMASTTermNode node, List<String> output) {
		// TODO Auto-generated method stub
		return output;
	}

	public List<String> visit(CMMASTConditionNode node, List<String> output) {
		// TODO Auto-generated method stub
		return output;
	}

	public List<String> visit(CMMASTExpNode node, List<String> output) {
		// TODO Auto-generated method stub
		return output;
	}

	/**
	 * Recursively visit all the children of a node
	 * @param node
	 * @param output
	 */
	protected void visitChildren(CMMASTNode node, List<String> output) {
		for (int i = 0; i < node.numChildren(); i++) {
			node.getChild(i).accept(this, output);
		}
	}
	
	public List<String> visit(CMMASTSimpleStatementNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTConstantNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}


	public List<String> visit(CMMASTParameterListNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTArgumentListNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTElementNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTExpressionListNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTElementPlusNode node, List<String> output) {
		if (node.numChildren() == 1) { // just an identifier
			return node.getChild(0).accept(this, output); 
		} else { // a function call
			String fname = node.getChild(0).getValue();
			Data f = lookup(fname);
			if (f == null)
				throw new RuntimeException("Attempt to call non-existent function: " + fname);
			node.getChild(1).accept(this, output);  // push parameters
			output.add("  invokestatic a/" + f.name);
		}
		return output;
	}


	public List<String> visit(CMMASTWhileLoopNode node, List<String> output) {
		throw new UnsupportedOperationException("while loops not yet implemented");
	}

	public List<String> visit(CMMASTDoLoopNode node, List<String> output) {
		throw new UnsupportedOperationException("do-while loops not yet implemented");
	}

	public List<String> visit(CMMASTReturnStatementNode node, List<String> output) {
		node.getChild(1).accept(this, output);
		Data r = lookup("22retval");
		output.add(t2a[r.type] + "return");
		return output;
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
	public List<String> visit(CMMASTFunctionDefinitionNode node, List<String> output) {
		String fname = node.getChild(1).getValue();
		String stype = node.getChild(0).getChild(0).getName();
		int itype = s2t(stype);
		// build method signature
		String sig = fname + "(" 
			+ getSignature((CMMASTParameterListNode)node.getChild(2)) + ")" + t2A[itype];
		output.add("\n.method public static " + sig);
		output.add(".limit stack 50");
		output.add(".limit locals 50");
		frames.peek().addFunction(fname, sig);
		frames.push(new StackFrame(frames.peek()));
		frames.peek().addVariable("22retval", itype);
		node.getChild(2).accept(this, output);   // parameter list
		node.getChild(3).accept(this, output);   // block
		frames.pop();
		output.add(".end method");
		return output;
	}

	public List<String> visit(CMMASTAssignmentNode node, List<String> output) {
		if (node.numChildren() == 1) {  // not really an assignment
			visitChildren(node, output);
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
			output.add("  " + t2a[data.type] + "store " + data.location + "   ; " + id);
		}
		return output;
	}

	@Override
	public List<String> visit(CMMASTStatementNode node, List<String> output) {
		visitChildren(node, output);
		return output;
	}

	@Override
	public List<String> visit(CMMASTTypeNode node, List<String> output) {
		return output;
	}

	public List<String> visit(CMMASTIfStatementNode node, List<String> output) {
		throw new UnsupportedOperationException("if statements are not yet implemented");
	}

	// Declaration -> Type Identifier (listsep Identifier)* eol
	public List<String> visit(CMMASTDeclarationNode node, List<String> output) {
		CMMASTNode type = node.getChild(0);
		String stype = type.getChild(0).getName();
		int itype = s2t(stype);
		for (int i = 1; i < node.numChildren(); i += 2)
			frames.peek().addVariable(node.getChild(i).getValue(), itype);
		return output;
	}

	public List<String> visit(CMMASTProgramNode node, List<String> output) {
		output.addAll(Arrays.asList(header));
		visitChildren(node, output);
		return output;
	}

	public List<String> visit(CMMASTBlockNode node, List<String> output) {
		frames.push(new StackFrame(frames.peek()));
		visitChildren(node, output);
		frames.pop();
		return output;
	}

	@Override
	public List<String> visit(CMMASTToken node, List<String> output) {
		if (node.getName().equals("number")) {
			output.add("  ldc " + node.getValue());
		} else if (node.getName().equals("string")) {
			output.add("  ldc " + node.getValue());
		} else if (node.getName().equals("boolean")) {
			output.add("  ldc " + (node.getValue().equals("true")));
		} else if (node.getName().equals("id")) {
			Data data = lookup(node.getValue());
			output.add("  " + t2a[data.type] + "load " + data.location + "   ;" + node.getValue());
		}
		return output;
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
		try {
			n = p.parse();
		} catch (CMMTokenizerException e) {
			System.err.println("A tokenizer exception occured:" + e);
			System.exit(-1);
		} catch (CMMParserException e) {
			System.err.println("A parse exception occured:" + e);
			System.exit(-1);
		}
		System.out.println("Program parsed successfully - attempting to compile...");
		CMMJasminVisitor v = new CMMJasminVisitor();
		List<String> output = n.accept(v, new ArrayList<String>());
		// System.out.print(output);
		try {
			PrintStream os = new PrintStream(new FileOutputStream("a.j"));
			for (String l : output) {
				os.println(l);
			}
		} catch (IOException e) {
			System.err.println(e);
		}
		System.out.println("Output written to a.j");
	}
}
