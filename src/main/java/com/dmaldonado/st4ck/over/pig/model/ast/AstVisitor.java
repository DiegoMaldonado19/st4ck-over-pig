package com.dmaldonado.st4ck.over.pig.model.ast;

import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ClassDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ImportDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Parameter;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Program;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructField;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.BinaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IdentifierExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IncrementExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.LiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MethodCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.NewArrayExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.NewExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ReadExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.TernaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ThisExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.UnaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Assignment;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Block;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.BreakStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.CaseClause;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ContinueStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.DoWhileStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ExpressionStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ForStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IfStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IncrementStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.InputStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.PrintStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SuperConstructorCall;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SwitchStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;

/**
 * Visitor pattern over the AST.
 *
 * Every phase that walks the tree (semantic analysis, quadruple generation)
 * implements this interface, so a new phase can be added without touching a
 * single node class.
 */
public interface AstVisitor<T>
{
    T visitProgram(Program node);
    T visitImportDeclaration(ImportDeclaration node);
    T visitVariableDeclaration(VariableDeclaration node);
    T visitArrayDeclaration(ArrayDeclaration node);
    T visitStructDeclaration(StructDeclaration node);
    T visitStructField(StructField node);
    T visitClassDeclaration(ClassDeclaration node);
    T visitFunctionDeclaration(FunctionDeclaration node);
    T visitParameter(Parameter node);

    T visitBlock(Block node);
    T visitAssignment(Assignment node);
    T visitIfStatement(IfStatement node);
    T visitWhileStatement(WhileStatement node);
    T visitDoWhileStatement(DoWhileStatement node);
    T visitForStatement(ForStatement node);
    T visitSwitchStatement(SwitchStatement node);
    T visitCaseClause(CaseClause node);
    T visitReturnStatement(ReturnStatement node);
    T visitBreakStatement(BreakStatement node);
    T visitContinueStatement(ContinueStatement node);
    T visitPrintStatement(PrintStatement node);
    T visitInputStatement(InputStatement node);
    T visitExpressionStatement(ExpressionStatement node);
    T visitIncrementStatement(IncrementStatement node);
    T visitSuperConstructorCall(SuperConstructorCall node);

    T visitBinaryExpression(BinaryExpression node);
    T visitUnaryExpression(UnaryExpression node);
    T visitIncrementExpression(IncrementExpression node);
    T visitLiteralExpression(LiteralExpression node);
    T visitIdentifierExpression(IdentifierExpression node);
    T visitArrayAccessExpression(ArrayAccessExpression node);
    T visitMemberAccessExpression(MemberAccessExpression node);
    T visitFunctionCallExpression(FunctionCallExpression node);
    T visitMethodCallExpression(MethodCallExpression node);
    T visitNewExpression(NewExpression node);
    T visitNewArrayExpression(NewArrayExpression node);
    T visitThisExpression(ThisExpression node);
    T visitTernaryExpression(TernaryExpression node);
    T visitReadExpression(ReadExpression node);
    T visitCompositeLiteralExpression(CompositeLiteralExpression node);
}
