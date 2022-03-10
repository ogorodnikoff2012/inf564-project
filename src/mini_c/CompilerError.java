package mini_c;

public abstract class CompilerError extends Exception {
  private final Loc location;
  public CompilerError(String message, Loc location) {
    super(message);
    this.location = location;
  }

  public Loc getLocation() {
    return location;
  }
}