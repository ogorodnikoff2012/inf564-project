package mini_c;

public class TtreePrinterVisitor implements Visitor {

  private final StringBuilder sBuilder = new StringBuilder();
  private boolean indentPrinted = false;
  private int indentLevel = 0;

  @Override
  public void visit(Unop n) {
    println(n.toString());
  }

  @Override
  public void visit(Binop n) {
    print(n.toString());
  }

  @Override
  public void visit(String n) {
    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Tint n) {
    print(n.toString());
  }

  @Override
  public void visit(Tstructp n) {
    print(n.toString(), " # Fields: ", n.s.fields.toString());
  }

  @Override
  public void visit(Tvoidstar n) {
    print(n.toString());
  }

  @Override
  public void visit(Ttypenull n) {
    print(n.toString());
  }

  @Override
  public void visit(Structure n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Field n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Decl_var n) {
    println("Name: ", n.name);
    print("Type: ");
    n.t.accept(this);
    println();
  }

  @Override
  public void visit(Expr n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Econst n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Eaccess_local n) {
    println("Eaccess_local: ");
    indentIn();
    println("Name: ", n.i);
    print("Type: ");
    n.typ.accept(this);
    println();
    indentOut();
  }

  @Override
  public void visit(Eaccess_field n) {
    println("Eaccess_field:");
    indentIn();
    println("Field: ", n.f.field_name);
    print("Type: ");
    n.f.field_typ.accept(this);
    println();
    println("Base:");
    indentIn();
    n.e.accept(this);
    indentOut();
    indentOut();
  }

  @Override
  public void visit(Eassign_local n) {
    println("Eassign_local:");
    indentIn();
    println("LHS: ", n.i);
    println("RHS:");
    indentIn();
    n.e.accept(this);
    indentOut();
    indentOut();
  }

  @Override
  public void visit(Eassign_field n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Eunop n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Ebinop n) {
    println("Ebinop:");
    indentIn();

    print("Type: ");
    print("!!! TODO fix");
    // n.typ.accept(this);
    println();

    print("Op: ");
    visit(n.b);
    println();

    println("LHS:");
    indentIn();
    n.e1.accept(this);
    indentOut();

    println("RHS:");
    indentIn();
    n.e2.accept(this);
    indentOut();

    indentOut();
  }

  @Override
  public void visit(Ecall n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Esizeof n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Sskip n) {
    println("!!! STUB ", n.getClass().getName());

  }

  @Override
  public void visit(Sexpr n) {
    println("Sexpr:");
    indentIn();
    n.e.accept(this);
    indentOut();
  }

  @Override
  public void visit(Sif n) {
    println("!!! STUB ", n.getClass().getName());

  }

  @Override
  public void visit(Swhile n) {
    println("!!! STUB ", n.getClass().getName());

  }

  @Override
  public void visit(Sblock n) {
    println("Vars:");
    indentIn();
    for (Decl_var decl_var : n.dl) {
      decl_var.accept(this);
    }
    indentOut();

    println("Stmts:");
    indentIn();
    for (Stmt stmt : n.sl) {
      stmt.accept(this);
    }
    indentOut();
  }

  @Override
  public void visit(Sreturn n) {

    println("!!! STUB ", n.getClass().getName());
  }

  @Override
  public void visit(Decl_fun n) {
    println("Decl_fun:");

    indentIn();
    println("Name: ", n.fun_name);
    print("Type: ");
    n.fun_typ.accept(this);
    println();
    println("Formals:");

    indentIn();
    for (Decl_var decl_var : n.fun_formals) {
      decl_var.accept(this);
    }
    indentOut();

    println("Body:");
    indentIn();
    n.fun_body.accept(this);
    indentOut();

    indentOut();
  }

  @Override
  public void visit(File n) {
    println("File:");
    indentIn();
    for (Decl_fun f : n.funs) {
      f.accept(this);
    }
    indentOut();
  }

  private void print(String... strs) {
    indent();
    for (String str : strs) {
      sBuilder.append(str);
    }
  }

  private void println(String... strs) {
    print(strs);
    print("\n");
    indentPrinted = false;
  }

  private void indent() {
    if (indentPrinted) {
      return;
    }
    for (int i = 0; i < indentLevel; ++i) {
      sBuilder.append("  ");
    }
    indentPrinted = true;
  }

  private void indentIn() {
    ++indentLevel;
  }

  private void indentOut() {
    --indentLevel;
  }

  @Override
  public String toString() {
    return sBuilder.toString();
  }
}
