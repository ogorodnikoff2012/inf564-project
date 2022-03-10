package mini_c;

public class SyntaxError extends CompilerError {
  public SyntaxError(String message, Loc location) {
    super("Syntax error: " + message, location);
  }
}
