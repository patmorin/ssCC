Program -> FunctionDefinition*

FunctionDefinition -> Type id ParameterList Block

ParameterList -> lparen (Parameter (listsep Parameter)*)? rparen

Parameter -> Type id

Block -> bb ExpressionList be

ExpressionList -> Statement*

Statement -> Declaration | WhileLoop | DoLoop | IfStatement | SimpleStatement

Declaration -> Type id (listsep id)* eol

Type -> number_t | string_t | boolean_t 

WhileLoop -> while Condition Block

DoLoop -> do Block while Condition eol

IfStatement -> if Condition Block (elsif Condition Block)* (else Block)?

Condition -> lparen Assignment rparen

SimpleStatement -> Assignment eol

Assignment -> Logical (gets Logical)?

Logical -> Comparison ((and|or) Comparison)*  [>1]
Comparison -> Sum ((lt|gt|eq|le|ge|ne) Sum)?  [>1]
Sum -> Term ((plus|minus) Term)*  [>1]
Term -> Exp ((multiply|divide|mod) Exp)* [>1]  
Exp -> Element (exp Element)*  [>1] 
Element -> Constant | lparen Logical rparen | ElementPlus
ElementPlus -> id ArgumentList?

Constant -> string | boolean | number

ArgumentList -> lparen (Assignment (listsep Assignment)*)? rparen

