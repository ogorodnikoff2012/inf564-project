package mini_c;

public class TypeError extends CompilerError {
  public TypeError(String message, Loc location) {
    super("Type error: " + message, location);
  }
}
