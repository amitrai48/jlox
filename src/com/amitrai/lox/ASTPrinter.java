package com.amitrai.lox;

import com.amitrai.lox.Expr.Assign;
import com.amitrai.lox.Expr.Binary;
import com.amitrai.lox.Expr.Grouping;
import com.amitrai.lox.Expr.Literal;
import com.amitrai.lox.Expr.Logical;
import com.amitrai.lox.Expr.Unary;
import com.amitrai.lox.Expr.Variable;

public class ASTPrinter implements Expr.Visitor<String> {

  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(Binary expr) {
    return paranthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Grouping expr) {
    return paranthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Literal expr) {
    if(expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Unary expr) {
    return paranthesize(expr.operator.lexeme, expr.right);
  }
  
  private String paranthesize(String name, Expr ...exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for(Expr expr: exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitVariableExpr(Variable expr) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String visitAssignExpr(Assign expr) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String visitLogicalExpr(Logical expr) {
    // TODO Auto-generated method stub
    return null;
  }
}
