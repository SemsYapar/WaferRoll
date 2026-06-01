package waferroll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    public final Map<Integer, FileInputStream> readHandles = new HashMap<>();
    public final Map<Integer, FileOutputStream> writeHandles = new HashMap<>();
    public int nextHandle = 0;
    
    Interpreter(){
        globals.define("clock", new LoxCallable() {
                @Override
                public int arity() { return 0;}

                @Override
                public Object call(Interpreter interpreter, List<Object> arguments){
                    return (double)System.currentTimeMillis() / 1000.0;
                }

                @Override
                public String toString() { return "<native fn clock>"; };
            });

        globals.define("getline", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String line = reader.readLine();
                    if (line == null) return null;
                    return line;
                } catch (IOException e) {
                    System.err.println("getline error: " + e.getMessage());
                    return null;
                }
            }

            @Override
            public String toString() { return "<native fn getline>"; }
        });

        globals.define("println", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments){
                System.out.println(arguments.get(0));
                return null;
            }

            @Override
            public String toString() { return "<native fn println>"; }
        });

        globals.define("print", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments){
                System.out.print(arguments.get(0));
                return null;
            }

            @Override
            public String toString() { return "<native fn print>"; }
        });
    
        //File jobs
        globals.define("open", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String path = (String) arguments.get(0);
                int id = interpreter.nextHandle++;

                String resolvedPath;
                File file = new File(path);
                if (file.isAbsolute()) {
                    resolvedPath = path;
                } else if (WaferRoll.scriptPath != null) {
                    String scriptDir = new File(WaferRoll.scriptPath).getParent();
                    resolvedPath = new File(scriptDir, path).getAbsolutePath();
                } else {
                    resolvedPath = new File(System.getProperty("user.dir"), path).getAbsolutePath();
                }

                try {
                    interpreter.readHandles.put(id, new FileInputStream(resolvedPath));
                } catch (FileNotFoundException e) {
                    interpreter.nextHandle--;
                    return -2.0;
                } catch (SecurityException e) {
                    interpreter.nextHandle--;
                    return -3.0;
                }

                try {
                    interpreter.writeHandles.put(id, new FileOutputStream(resolvedPath, false));
                } catch (SecurityException ignored) {}
                catch (IOException ignored) {}

                return (double) id;
            }

            @Override
            public String toString() { return "<native fn open>"; }
        });

        globals.define("close", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                int id = (int)(double) arguments.get(0);

                if (!interpreter.readHandles.containsKey(id) && !interpreter.writeHandles.containsKey(id)) {
                    return -1.0;
                }

                try {
                    FileInputStream in = interpreter.readHandles.remove(id);
                    if (in != null) in.close();
                    FileOutputStream out = interpreter.writeHandles.remove(id);
                    if (out != null) out.close();
                } catch (IOException e) {
                    System.err.println("close error: " + e.getMessage());
                }

                return null;
            }

            @Override
            public String toString() { return "<native fn close>"; }
        });

        globals.define("read", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                int id = (int)(double) arguments.get(0);

                if (!interpreter.readHandles.containsKey(id)) {
                    return -1.0;
                }

                FileInputStream in = interpreter.readHandles.get(id);
                try {
                    byte[] bytes = in.readAllBytes();
                    if (bytes.length == 0) return null;
                    return new String(bytes);
                } catch (IOException e) {
                    System.err.println("read error: " + e.getMessage());
                    return -1.0;
                }
            }

            @Override
            public String toString() { return "<native fn read>"; }
        });

        globals.define("write", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                int id = (int)(double) arguments.get(0);

                if (!interpreter.writeHandles.containsKey(id)) {
                    return -1.0;
                }

                String data = arguments.get(1).toString();
                FileOutputStream out = interpreter.writeHandles.get(id);
                try {
                    out.write(data.getBytes());
                    out.flush();
                } catch (IOException e) {
                    System.err.println("write error: " + e.getMessage());
                    return -1.0;
                }

                return null;
            }

            @Override
            public String toString() { return "<native fn write>"; }
        });
 
        globals.define("str", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object value = arguments.get(0);
                if (value == null) return "nil";
                if (value instanceof Boolean) return value.toString();
                if (value instanceof Double) {
                    double d = (double) value;
                    if (d == (long) d) return String.valueOf((long) d);
                    return String.valueOf(d);
                }
                return value.toString();
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt){
        Object value = null;
        if (stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt){
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt){
        throw new Break();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt){
        throw new Continue();
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        boolean doContinue = false;
        while (isTruthy(evaluate(stmt.condition))) {
            try{
                execute(stmt.body);
            } catch (Break b){
                break;
            } catch (Continue c){
                doContinue = true;
            }
            if (stmt.increment != null) evaluate(stmt.increment);
            if (doContinue) continue;
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr){
        Integer distance = locals.get(expr);
        if (distance != null){
            return environment.getAt(distance, name.lexeme);
        } else{
            return globals.get(name);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr){
        return evaluate(expr.expression);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr){
        Object callee = evaluate(expr.callee);

        if (!(callee instanceof LoxCallable)){
            throw new RuntimeError(expr.paren, "Can only call functions");
        }
        LoxCallable function = (LoxCallable)callee;

        if (expr.arguments.size() != function.arity()){
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + expr.arguments.size()+".");
        }

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments){
            arguments.add(evaluate(argument));
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right = evaluate(expr.right);
        switch (expr.operator.type){
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        //Unreachable
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
            } else {
                if (!isTruthy(left)) return left;
            }

        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr){
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, right, left);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, right, left);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, right, left);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, right, left);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, right, left);
                return (double)left-(double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double){
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String){
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,"Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, right, left);
                return (double)left/(double)right;
            case STAR:
                checkNumberOperands(expr.operator, right, left);
                return (double)left*(double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
        }

        //Unreachable.
        return null;
    }
    
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        
        Integer distance = locals.get(expr);
        if (distance != null){
            environment.assignAt(distance, expr.name, value);
        } else{
            globals.assign(expr.name, value);
        }
        
        return value;
    }


    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
            } finally {
            this.environment = previous;
        }
    }

    private boolean isTruthy(Object object){
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b){
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
        String text = object.toString();
        if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    return object.toString();
    }

    private void execute(Stmt stmt){
        stmt.accept(this);
    } 

    public Object evaluate(Expr expr){
        return expr.accept(this);
    }

    void resolve(Expr expr, int depth){
        locals.put(expr, depth);
    }

    void interpret(List<Stmt> statements) { 
    try{
        for(Stmt statement: statements){
            execute(statement);
        }
    } catch (RuntimeError error){
        WaferRoll.runtimeError(error);
    }
  }
}
