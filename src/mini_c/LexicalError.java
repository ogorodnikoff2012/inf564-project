package mini_c;

public class LexicalError extends CompilerError {
  public LexicalError(String message, Loc location) {
    super("Lexical error: " + message, location);
  }
}
