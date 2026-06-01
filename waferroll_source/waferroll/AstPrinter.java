package waferroll;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    private int depth = 0;

    String print(Expr expr) {
        return expr.accept(this);
    }

    String print(Stmt stmt) {
        return stmt.accept(this);
    }

    String print(List<Stmt> statements) {
        StringBuilder sb = new StringBuilder();
        for (Stmt stmt : statements) {
            sb.append(stmt.accept(this)).append("\n");
        }
        return sb.toString();
    }
    // ── Expr ──────────────────────────────────────────

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return expr.left.accept(this) + " " + expr.operator.lexeme + " " + expr.right.accept(this);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.expression.accept(this) + ")";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        if (expr.value instanceof String) return "\"" + expr.value + "\"";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.operator.lexeme + expr.right.accept(this);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.lexeme + " = " + expr.value.accept(this);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return expr.left.accept(this) + " " + expr.operator.lexeme + " " + expr.right.accept(this);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.callee.accept(this)).append("(");
        for (int i = 0; i < expr.arguments.size(); i++) {
            if (i != 0) sb.append(", ");
            sb.append(expr.arguments.get(i).accept(this));
        }
        sb.append(")");
        return sb.toString();
    }

    // ── Stmt ──────────────────────────────────────────

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        depth++;
        String indent = "  ".repeat(depth);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Stmt s : stmt.statements) {
            sb.append(indent);
            sb.append(s.accept(this));
            sb.append("\n");
        }
        depth--;
        sb.append("  ".repeat(depth)).append("}");
        return sb.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return stmt.expression.accept(this) + ";";
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("fun ").append(stmt.name.lexeme).append("(");
        for (int i = 0; i < stmt.params.size(); i++) {
            if (i != 0) sb.append(", ");
            sb.append(stmt.params.get(i).lexeme);
        }
        sb.append(") {\n");
        for (Stmt s : stmt.body) {
            sb.append("  ").append(s.accept(this)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(stmt.condition.accept(this)).append(") ");
        sb.append(stmt.thenBranch.accept(this));
        if (stmt.elseBranch != null) {
            sb.append(" else ").append(stmt.elseBranch.accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) return "return;";
        return "return " + stmt.value.accept(this) + ";";
    }

    @Override
    public String visitBreakStmt(Stmt.Break stmt) {
        return "break;";
    }

    public String visitContinueStmt(Stmt.Continue stmt){
        return "continue;";
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        if (stmt.initializer == null) return "var " + stmt.name.lexeme + ";";
        return "var " + stmt.name.lexeme + " = " + stmt.initializer.accept(this) + ";";
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        String result = "while (" + stmt.condition.accept(this) + ") ";
        
        if (stmt.increment != null) {
            String body = stmt.body.accept(this);
            String incrementIndent = "  ".repeat(depth);
            String closingIndent = "  ".repeat(depth);
            body = body.substring(0, body.lastIndexOf("}"))
                + incrementIndent + stmt.increment.accept(this) + ";\n"
                + closingIndent + "}";
            result += body;
        } else {
            result += stmt.body.accept(this);
        }
        
        return result;
    }

/*
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Literal(45.67)));

        System.out.println(new AstPrinter().print(expression));
    }
*/
    // ── main ──────────────────────────────────────────


}