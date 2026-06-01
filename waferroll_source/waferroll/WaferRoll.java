package waferroll;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WaferRoll {

    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static boolean in_comment = false;
    static String scriptPath = null;
    public static void main(String[] args) throws IOException {
        if (args.length > 1){
            System.out.println("Usage: interpreter [script]");
            System.exit(64);
        } else if (args.length == 1){
            scriptPath = args[0];
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        
        for (;;){
            if (in_comment){
                System.out.print("[COMMENT]> ");
            } else {
                System.out.print("> ");
            }
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) throws IOException {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (hadError) return;

        List<Stmt> toInterpret = statements;

        if (scriptPath != null) {
            Obfuscator obfuscator = new Obfuscator();
            List<Stmt> obfuscated = obfuscator.obfuscate(statements);

            // safe klasörü oluştur
            Path scriptFile = Paths.get(scriptPath);
            Path safeDir = scriptFile.getParent().resolve("safe");
            Files.createDirectories(safeDir);

            // aynı isimde .wro uzantısı ile kaydet
            String fileName = scriptFile.getFileName().toString();
            String baseName = fileName.contains(".") 
                ? fileName.substring(0, fileName.lastIndexOf('.')) 
                : fileName;
            Path outputPath = safeDir.resolve(baseName + ".wro");

            // AstPrinter ile yaz
            String obfuscatedSource = new AstPrinter().print(obfuscated);
            Files.write(outputPath, obfuscatedSource.getBytes());

            toInterpret = obfuscated;
        }

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(toInterpret);
        if (hadError) return;
        interpreter.interpret(toInterpret);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line "+ line + "] Error"+ where + ": "+ message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
  }
}
