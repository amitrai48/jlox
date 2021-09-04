package com.amitrai.lox;

import java.util.List;

import com.amitrai.lox.Expr.Assign;
import com.amitrai.lox.Expr.Binary;
import com.amitrai.lox.Expr.Grouping;
import com.amitrai.lox.Expr.Literal;
import com.amitrai.lox.Expr.Logical;
import com.amitrai.lox.Expr.Unary;
import com.amitrai.lox.Expr.Variable;
import com.amitrai.lox.Stmt.Block;
import com.amitrai.lox.Stmt.Expression;
import com.amitrai.lox.Stmt.If;
import com.amitrai.lox.Stmt.Print;
import com.amitrai.lox.Stmt.Var;
import com.amitrai.lox.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

  private Environment environment = new Environment();

  void interpret(List<Stmt> statements) {
    try {
      for(Stmt statement: statements) {
        execute(statement);
      }
    } catch(RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch(expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left <= (double)right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left - (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left * (double)right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right); 
        return (double)left / (double)right;
      case PLUS:
        if(left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if(left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        throw new RuntimeError(expr.operator, "Operands must be both numbers or strings!");
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
    }
    return null;
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object right = evaluate(expr.right);
    switch(expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right); 
        return -(double)right;
    }
    return null;
  }

  @Override
  public Void visitExpressionStmt(Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  private Object evaluate(Expr expression) {
    return expression.accept(this);
  }

  private boolean isTruthy(Object value){
    if(value == null) return false;
    if(value instanceof Boolean) return (boolean)value;
    return true;
  }

  private boolean isEqual(Object left, Object right) {
    if(left == null && right == null) return true;
    if(left == null) return false;
    return left.equals(right);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if(operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be number.");
  }
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if(left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be number.");
  }
  private String stringify(Object value) {
    if(value == null) return "nil";
    if(value instanceof Double) {
      String text = value.toString();
      if(text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return value.toString();
  }
  
  private void execute(Stmt statement) {
    statement.accept(this);
  }

  @Override
  public Void visitVarStmt(Var stmt) {
    Object value = null;
    if(stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitVariableExpr(Variable expr) {
    return environment.get(expr.name);
  }

  @Override
  public Object visitAssignExpr(Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Void visitBlockStmt(Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  public void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for(Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitIfStmt(If stmt) {
    if(isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitLogicalExpr(Logical expr) {
    Object left = evaluate(expr.left);
    if(expr.operator.type == TokenType.OR) {
      if(isTruthy(left)) return left;
    } else {
      if(isTruthy(left)) return left;
    }
    return evaluate(expr.right);
  }

  @Override
  public Void visitWhileStmt(While stmt) {
    while(isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }
}
