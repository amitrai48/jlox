package com.amitrai.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAST {
  public static void main(String[] args) throws IOException {
    if(args.length != 1){
      System.out.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];
    defineAST(outputDir, "Expr", Arrays.asList(
      "Assign: Token name, Expr value",
      "Binary: Expr left, Token operator, Expr right",
      "Logical: Expr left, Token operator, Expr right",
      "Grouping: Expr expression",
      "Literal: Object value",
      "Unary: Token operator, Expr right",
      "Variable: Token name"));
    
    defineAST(outputDir, "Stmt", Arrays.asList(
      "Block: List<Stmt> statements",
      "Expression: Expr expression",
      "If: Expr condition, Stmt thenBranch, Stmt elseBranch", 
      "Print: Expr expression",
      "Var: Token name, Expr initializer",
      "While: Expr condition, Stmt body"
    ));
  }

  private static void defineAST(String outputDir, String baseName, 
    List<String> types) throws IOException {
      String path = outputDir + "/" + baseName + ".java";
      PrintWriter pw = new PrintWriter(path, "UTF-8");
      pw.println("package com.amitrai.lox;");
      pw.println();
      pw.println("import java.util.List;");
      pw.println();
      pw.println("abstract class " + baseName + " {");
      
      defineVisitor(pw, baseName, types);

      pw.println("abstract <R> R accept(Visitor<R> visitor);");

      //AST classes
      for(String type: types) {
        String className = type.split(":")[0].trim();
        String fieldList = type.split(":")[1].trim();
        defineTypes(pw, baseName, className, fieldList);
      }
      pw.println("}");
      pw.close();
  }

  private static void defineVisitor(PrintWriter pw, String baseName, List<String> types) {
    pw.println("interface Visitor<R> {");
    for (String type: types) {
      String typeName = type.split(":")[0].trim();
      pw.println("R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
    }
    pw.println("}");
  }

  private static void defineTypes(PrintWriter pw, String baseName, String className, String fieldList) {
    pw.println("static class " + className + " extends " + baseName + " {");
    String[] fields = fieldList.split(", ");
    for(String field: fields) {
      pw.println("final " + field+ ";");
    }
    pw.println();
    pw.println(className + "(" + fieldList + ") {");
    for(String field: fields) {
      String name = field.split(" ")[1];
      pw.println("this." + name +" = " + name + ";");
    }
    pw.println("}");
    pw.println();
    pw.println("@Override");
    pw.println("<R> R accept(Visitor<R> visitor) {");
    pw.println("return visitor.visit" + className + baseName + "(this);");
    pw.println("}");
    pw.println("}");
  }
}
