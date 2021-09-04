package com.amitrai.lox;

import java.util.ArrayList;
import java.util.List;
import com.amitrai.lox.Expr.Binary;
import com.amitrai.lox.Expr.Grouping;
import com.amitrai.lox.Expr.Literal;
import com.amitrai.lox.Expr.Unary;

public class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while(!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }

  // expression = assignment
  private Expr expression() {
    return assignment();
  }

  //assignment = IDENTIFIER "=" assigment | logic_or
  private Expr assignment() {
    Expr expr = or();
    if(match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      if(expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }
      error(equals, "Invalid assignment target.");
    }
    return expr;
  }

  //logic_or = logic_and ( "or" logic_and )* ;
  private Expr or() {
    Expr expr = and();
    while(match(TokenType.OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  //logic_and = equality ( "and" equality )*
  private Expr and() {
    Expr expr = equality();
    while(match(TokenType.AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  // equality = comparison ( (!= | ==)  comparison )*
  private Expr equality() {
    Expr expr = comparison();
    while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  // comparison = term ( ( "<" | "<=" | ">" | ">=" ) term )*
  private Expr comparison() {
    Expr expr = term();
    while(match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  // term = factor ( ("+" | "-") factor)*
  private Expr term() {
    Expr expr = factor();
    while(match(TokenType.MINUS, TokenType.PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  //factor = unary (("*" | "/") unary)*
  private Expr factor() {
    Expr expr = unary();
    while(match(TokenType.STAR, TokenType.SLASH)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Binary(expr, operator, right);
    }
    return expr;
  }

  //unary = ("!" | "-") unary | primary
  private Expr unary() {
    if(match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Unary(operator, right);
    }
    return primary();
  }

  //primary = NUMBER | STRING | "true" | "false" | "nil"| "(" expression ")" ;
  private Expr primary() {
    if(match(TokenType.FALSE)) return new Literal(false);
    if(match(TokenType.TRUE)) return new Literal(true);
    if(match(TokenType.NIL)) return new Literal(null);

    if(match(TokenType.NUMBER, TokenType.STRING)) {
      return new Literal(previous().literal);
    }

    if(match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if(match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Grouping(expr);
    }
    throw error(peek(), "Expect expression");
  }

  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) return varDeclaration();
      return statement();
    } catch(ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name");
    Expr initializer = null;
    if(match(TokenType.EQUAL)) {
      initializer = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  // statement = exprStatement | if statement | printStatement | blockStatement
  private Stmt statement() {
    if (match(TokenType.IF)) return ifStatement();
    if (match(TokenType.PRINT)) return printStatement();
    if (match(TokenType.LEFT_BRACE)) return blockStatement();
    return expressionStatement();
  }

  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect ( after if");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ) after condition");
    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if(match(TokenType.ELSE)) {
      elseBranch = statement();
    }
    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ; after value");
    return new Stmt.Print(value);
  }

  private Stmt expressionStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ; after value");
    return new Stmt.Expression(value);
  }

  private Stmt blockStatement() {
    List<Stmt> statements = new ArrayList<>();
    while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }
    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return new Stmt.Block(statements);
  }

  private boolean match(TokenType... types) {
    for(TokenType type: types) {
      if(check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token advance() {
    if (!isAtEnd()) {
      this.current++;
    }
    return previous();
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token peek() {
    return this.tokens.get(this.current);
  }

  private boolean isAtEnd() {
    return this.tokens.get(this.current).type == TokenType.EOF;
  }

  private Token previous() {
    return this.tokens.get(this.current - 1);
  }

  private Token consume(TokenType type, String message) {
    if(check(type)) return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
