package waferroll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;

public class Obfuscator implements Expr.Visitor<Expr>, Stmt.Visitor<Stmt>{
    private final Map<String, String> nameMap = new HashMap<>();
    private int count = 0;
    private final Random random = new Random();
    private List<Stmt.Var> pendingVars = new ArrayList<>();

    Obfuscator(){
        nameMap.put("clock", "clock");
        nameMap.put("getline", "getline");
        nameMap.put("print", "print");
        nameMap.put("println", "println");
        nameMap.put("open", "open");
        nameMap.put("close", "close");
        nameMap.put("read", "read");
        nameMap.put("write", "write");
        nameMap.put("append", "append");
        nameMap.put("str", "str");
        nameMap.put("round", "round");
    }

    List<Stmt> obfuscate(List<Stmt> statements){
        List<Stmt> obfuscated = new ArrayList<>();
        for(Stmt statement : statements){
            obfuscated.add(transform(statement));
        }

        // pendingVars ı karıştır ve başa ekle
        Collections.shuffle(pendingVars);
        for (int i = pendingVars.size() - 1; i >= 0; i--) {
            obfuscated.add(0, pendingVars.get(i));
        }

        return obfuscated;
    }

    private Stmt transform(Stmt stmt){
        return stmt.accept(this);
    }

    private Expr transform(Expr expr){
        return expr.accept(this);
    }

    public Stmt visitClassStmt(Stmt.Class stmt){
        String newName = obfuscateName(stmt.name.lexeme);
        Token newToken = new Token(stmt.name.type, newName, null, stmt.name.line);
        return new Stmt.Class(newToken, stmt.methods);
    }

    public Stmt visitBlockStmt(Stmt.Block stmt){
        List<Stmt> newStatements = new ArrayList<>();
        for (Stmt s : stmt.statements) {
            newStatements.add(transform(s));
        }
        return new Stmt.Block(newStatements);
    }

    public Stmt visitExpressionStmt(Stmt.Expression stmt){
        Expr newExpression = transform(stmt.expression);
        return new Stmt.Expression(newExpression);
    }

    public Stmt visitIfStmt(Stmt.If stmt){
        Expr newCondition = transform(stmt.condition);
        Stmt newThenBranch = transform(stmt.thenBranch);
        Stmt newElseBranch = null;
        if (stmt.elseBranch != null) newElseBranch = transform(stmt.elseBranch);
        return new Stmt.If(newCondition, newThenBranch, newElseBranch); 
    }

    public Stmt visitReturnStmt(Stmt.Return stmt){
        Expr newValue = null;
        if (stmt.value != null) newValue = transform(stmt.value);
        return new Stmt.Return(stmt.keyword, newValue);
    }

    public Stmt visitBreakStmt(Stmt.Break stmt){
        return stmt;
    }

    public Stmt visitContinueStmt(Stmt.Continue stmt){
        return stmt;
    }

    public Stmt visitVarStmt(Stmt.Var stmt){
        String newName = obfuscateName(stmt.name.lexeme);
        Token newToken = new Token(stmt.name.type, newName, null, stmt.name.line);
        Expr newInitializer = null;
        if (stmt.initializer != null) newInitializer = transform(stmt.initializer);
        return new Stmt.Var(newToken, newInitializer);
    }

    public Stmt visitWhileStmt(Stmt.While stmt){
        Expr newCondition = transform(stmt.condition);
        Stmt newBody = transform(stmt.body);
        Expr newIncrement = stmt.increment != null ? transform(stmt.increment) : null;
        return new Stmt.While(newCondition, newBody, newIncrement);
    }


    public Stmt visitFunctionStmt(Stmt.Function stmt){
        String newName = obfuscateName(stmt.name.lexeme);
        Token newToken = new Token(stmt.name.type, newName, null, stmt.name.line);
        List<Token> newParams = new ArrayList<>();
        for (Token param : stmt.params){
            String newParamName = obfuscateName(param.lexeme);
            Token newParamToken = new Token(param.type, newParamName, null, param.line);
            newParams.add(newParamToken);
        }
        List<Stmt> newBody = new ArrayList<>();
        for (Stmt s : stmt.body) {
            newBody.add(transform(s));
        }
        return new Stmt.Function(newToken, newParams, newBody);
    }

    public Expr visitVariableExpr(Expr.Variable expr){
        String newName = obfuscateName(expr.name.lexeme);
        Token newToken = new Token(expr.name.type, newName, null, expr.name.line);
        return new Expr.Variable(newToken);
    }

    public Expr visitAssignExpr(Expr.Assign expr){
        String newName = obfuscateName(expr.name.lexeme);
        Token newToken = new Token(expr.name.type, newName, null, expr.name.line);
        Expr newExpression = transform(expr.value);
        return new Expr.Assign(newToken, newExpression);
    }

    public Expr visitBinaryExpr(Expr.Binary expr){
        Expr leftExpression = transform(expr.left);
        Expr rightExpression = transform(expr.right);
        return new Expr.Binary(leftExpression, expr.operator, rightExpression);
    }

    public Expr visitLogicalExpr(Expr.Logical expr){
        Expr leftExpression = transform(expr.left);
        Expr rightExpression = transform(expr.right);
        return new Expr.Logical(leftExpression, expr.operator, rightExpression);
    }

    public Expr visitCallExpr(Expr.Call expr){
        Expr newCallee = transform(expr.callee);
        List<Expr> newArguments = new ArrayList<>();
        for (Expr arg : expr.arguments){
            newArguments.add(transform(arg));
        }
        return new Expr.Call(newCallee, expr.paren, newArguments);
    }

    public Expr visitGroupingExpr(Expr.Grouping expr){
        Expr newExpression = transform(expr.expression);
        return new Expr.Grouping(newExpression);
    }

    public Expr visitUnaryExpr(Expr.Unary expr){
        Expr newRight = transform(expr.right);
        return new Expr.Unary(expr.operator, newRight);
    }

    public Expr visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof Double) {
            return obfuscateNumber((double) expr.value);
        }
        if (expr.value instanceof String) {
            return obfuscateString((String) expr.value);
        }
        return expr;
    }

    private String obfuscateName(String name){
        return nameMap.computeIfAbsent(name, k -> generateName());
    }

    private Expr obfuscateNumber(double value) {
        if (value != (long) value){
            return new Expr.Literal(value);
        }
        
        int strategy = random.nextInt(2);
        
        if (strategy == 0) {
            double a = random.nextInt(Math.max(1, (int)Math.abs(value)));
            double b = value - a;
            return new Expr.Grouping(new Expr.Binary(
                new Expr.Literal(a),
                new Token(TokenType.PLUS, "+", null, 0),
                new Expr.Literal(b)
            ));
        } else {
            double a = value + random.nextInt(100);
            double b = a - value;
            return new Expr.Grouping(new Expr.Binary(
                new Expr.Literal(a),
                new Token(TokenType.MINUS, "-", null, 0),
                new Expr.Literal(b)
            ));
        }
    }

    private Expr obfuscateString(String value) {
        int partCount = (int) Math.max(2, Math.ceil(Math.sqrt(value.length())));
        
        // string i partCount kadar parçaya böl
        List<String> parts = new ArrayList<>();
        int len = value.length();
        int start = 0;
        for (int i = 0; i < partCount; i++) {
            int end = start + (int) Math.ceil((double)(len - start) / (partCount - i));
            parts.add(value.substring(start, Math.min(end, len)));
            start = end;
            if (start >= len) break;
        }

        // her parça için değişken üret, pendingVars a ekle
        List<String> varNames = new ArrayList<>();
        for (String part : parts) {
            String varName = generateName();
            varNames.add(varName);
            Token token = new Token(TokenType.IDENTIFIER, varName, null, 0);
            pendingVars.add(new Stmt.Var(token, new Expr.Literal(part)));
        }

        // değişkenleri birleştiren Expr zinciri üret
        Expr result = new Expr.Variable(new Token(TokenType.IDENTIFIER, varNames.get(0), null, 0));
        for (int i = 1; i < varNames.size(); i++) {
            result = new Expr.Binary(
                result,
                new Token(TokenType.PLUS, "+", null, 0),
                new Expr.Variable(new Token(TokenType.IDENTIFIER, varNames.get(i), null, 0))
            );
        }
        return result;
    }

    private String generateName() {
        return "xx" + count++;
    }
}
