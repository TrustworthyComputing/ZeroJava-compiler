//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package visitor;
import syntaxtree.*;
import java.util.*;

/**
 * All void visitors must implement this interface.
 */

public interface Visitor {

   //
   // void Auto class visitors
   //

   public void visit(NodeList n) throws Exception;
   public void visit(NodeListOptional n) throws Exception;
   public void visit(NodeOptional n) throws Exception;
   public void visit(NodeSequence n) throws Exception;
   public void visit(NodeToken n) throws Exception;

   //
   // User-generated visitor methods below
   //

   /**
    * f0 -> ( MethodDeclaration() )*
    * f1 -> MainMethodDeclaration()
    * f2 -> <EOF>
    */
   public void visit(Goal n) throws Exception;

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public void visit(VarDeclaration n) throws Exception;

   /**
    * f0 -> "void"
    * f1 -> "main"
    * f2 -> "("
    * f3 -> "void"
    * f4 -> ")"
    * f5 -> "{"
    * f6 -> ( VarDeclaration() )*
    * f7 -> ( Statement() )*
    * f8 -> "}"
    */
   public void visit(MainMethodDeclaration n) throws Exception;

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ( FormalParameterList() )?
    * f4 -> ")"
    * f5 -> "{"
    * f6 -> ( VarDeclaration() )*
    * f7 -> ( Statement() )*
    * f8 -> "return"
    * f9 -> Expression()
    * f10 -> ";"
    * f11 -> "}"
    */
   public void visit(MethodDeclaration n) throws Exception;

   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public void visit(FormalParameterList n) throws Exception;

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public void visit(FormalParameter n) throws Exception;

   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   public void visit(FormalParameterTail n) throws Exception;

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public void visit(FormalParameterTerm n) throws Exception;

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public void visit(Type n) throws Exception;

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public void visit(ArrayType n) throws Exception;

   /**
    * f0 -> "boolean"
    */
   public void visit(BooleanType n) throws Exception;

   /**
    * f0 -> "int"
    */
   public void visit(IntegerType n) throws Exception;

   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | PlusPlusExpression()
    *       | MinusMinusExpression()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    *       | ReadPrimaryTape()
    *       | ReadPrivateTape()
    *       | SeekPrimaryTape()
    *       | SeekPrivateTape()
    *       | AnswerStatement()
    */
   public void visit(Statement n) throws Exception;

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   public void visit(Block n) throws Exception;

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public void visit(AssignmentStatement n) throws Exception;

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public void visit(ArrayAssignmentStatement n) throws Exception;

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public void visit(IfStatement n) throws Exception;

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public void visit(WhileStatement n) throws Exception;

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public void visit(PrintStatement n) throws Exception;

   /**
    * f0 -> "PrimaryTape.read"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public void visit(ReadPrimaryTape n) throws Exception;

   /**
    * f0 -> "PrivateTape.read"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public void visit(ReadPrivateTape n) throws Exception;

   /**
    * f0 -> "PrimaryTape.seek"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ","
    * f4 -> Expression()
    * f5 -> ")"
    * f6 -> ";"
    */
   public void visit(SeekPrimaryTape n) throws Exception;

   /**
    * f0 -> "PrivateTape.seek"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ","
    * f4 -> Expression()
    * f5 -> ")"
    * f6 -> ";"
    */
   public void visit(SeekPrivateTape n) throws Exception;

   /**
    * f0 -> "Prover.answer"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public void visit(AnswerStatement n) throws Exception;

   /**
    * f0 -> AndExpression()
    *       | OrExpression()
    *       | EqExpression()
    *       | LessThanExpression()
    *       | GreaterThanExpression()
    *       | LessEqualThanExpression()
    *       | GreaterEqualThanExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | MessageSend()
    *       | MethodCall()
    *       | Clause()
    */
   public void visit(Expression n) throws Exception;

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public void visit(AndExpression n) throws Exception;

   /**
    * f0 -> Clause()
    * f1 -> "||"
    * f2 -> Clause()
    */
   public void visit(OrExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "=="
    * f2 -> PrimaryExpression()
    */
   public void visit(EqExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public void visit(LessThanExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> ">"
    * f2 -> PrimaryExpression()
    */
   public void visit(GreaterThanExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<="
    * f2 -> PrimaryExpression()
    */
   public void visit(LessEqualThanExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> ">="
    * f2 -> PrimaryExpression()
    */
   public void visit(GreaterEqualThanExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public void visit(PlusExpression n) throws Exception;

   /**
    * f0 -> Identifier()
    * f1 -> "++"
    * f2 -> ";"
    */
   public void visit(PlusPlusExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public void visit(MinusExpression n) throws Exception;

   /**
    * f0 -> Identifier()
    * f1 -> "--"
    * f2 -> ";"
    */
   public void visit(MinusMinusExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public void visit(TimesExpression n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public void visit(ArrayLookup n) throws Exception;

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public void visit(MessageSend n) throws Exception;

   /**
    * f0 -> Identifier()
    * f1 -> "("
    * f2 -> ( ExpressionList() )?
    * f3 -> ")"
    */
   public void visit(MethodCall n) throws Exception;

   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public void visit(ExpressionList n) throws Exception;

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public void visit(ExpressionTail n) throws Exception;

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public void visit(ExpressionTerm n) throws Exception;

   /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public void visit(Clause n) throws Exception;

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
   public void visit(PrimaryExpression n) throws Exception;

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public void visit(IntegerLiteral n) throws Exception;

   /**
    * f0 -> "true"
    */
   public void visit(TrueLiteral n) throws Exception;

   /**
    * f0 -> "false"
    */
   public void visit(FalseLiteral n) throws Exception;

   /**
    * f0 -> <IDENTIFIER>
    */
   public void visit(Identifier n) throws Exception;

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public void visit(ArrayAllocationExpression n) throws Exception;

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public void visit(AllocationExpression n) throws Exception;

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public void visit(NotExpression n) throws Exception;

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public void visit(BracketExpression n) throws Exception;

}

