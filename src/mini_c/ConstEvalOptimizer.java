package mini_c;

import java.util.LinkedList;

public class ConstEvalOptimizer implements Visitor {
  private Object result;

  @Override
  public void visit(Unop unop) {
    throw new Error("Mustn't be called: Unop");
  }

  @Override
  public void visit(Binop binop) {
    throw new Error("Mustn't be called: Binop");
  }

  @Override
  public void visit(String string) {
    throw new Error("Mustn't be called: String");
  }

  @Override
  public void visit(Tint tint) {
    this.result = tint;
  }

  @Override
  public void visit(Tstructp tstructp) {
    this.result = tstructp;
  }

  @Override
  public void visit(Tvoidstar tvoidstar) {
    this.result = tvoidstar;
  }

  @Override
  public void visit(Ttypenull ttypenull) {
    this.result = ttypenull;
  }

  @Override
  public void visit(Structure structure) {
    this.result = structure;
  }

  @Override
  public void visit(Field field) {
    this.result = field;
  }

  @Override
  public void visit(Decl_var decl_var) {
    this.result = decl_var;
  }

  @Override
  public void visit(Expr expr) {
    throw new Error("Mustn't be visited: Expr");
  }

  @Override
  public void visit(Econst econst) {
    this.result = econst;
  }

  @Override
  public void visit(Eaccess_local eaccess_local) {
    this.result = eaccess_local;
  }

  @Override
  public void visit(Eaccess_field eaccess_field) {
    Eaccess_field ef = new Eaccess_field(transform(eaccess_field.e), eaccess_field.f);
    ef.typ = eaccess_field.typ;
    this.result = ef;
  }

  @Override
  public void visit(Eassign_local eassign_local) {
    Eassign_local el = new Eassign_local(eassign_local.i, transform(eassign_local.e));
    el.typ = eassign_local.typ;
    this.result = el;
  }

  @Override
  public void visit(Eassign_field eassign_field) {
    Expr e1 = transform(eassign_field.e1);
    Expr e2 = transform(eassign_field.e2);
    Eassign_field ef = new Eassign_field(e1, eassign_field.f, e2);
    ef.typ = eassign_field.typ;
    this.result = ef;
  }

  @Override
  public void visit(Eunop eunop) {
    Expr arg = transform(eunop.e);
    if (arg instanceof Econst) {
      Econst c = (Econst) arg;

      int result = 0;
      switch (eunop.u) {
        case Uneg:
          result = -c.i;
          break;
        case Unot:
          result = c.i == 0 ? 1 : 0;
          break;
      }

      Econst ec = new Econst(result);
      ec.typ = Tint.tint;
      this.result = ec;
    } else {
      Eunop eu = new Eunop(eunop.u, arg);
      eu.typ = eunop.typ;
      this.result = eu;
    }
  }

  @Override
  public void visit(Ebinop ebinop) {
    Expr lhs = transform(ebinop.e1);
    Expr rhs = transform(ebinop.e2);

    if (lhs instanceof Econst && rhs instanceof Econst) {
      Econst lconst = (Econst) lhs;
      Econst rconst = (Econst) rhs;

      int result = 0;
      switch (ebinop.b) {
        case Beq:
          result = (lconst.i == rconst.i) ? 1 : 0;
          break;
        case Bneq:
          result = (lconst.i != rconst.i) ? 1 : 0;
          break;
        case Blt:
          result = (lconst.i < rconst.i) ? 1 : 0;
          break;
        case Ble:
          result = (lconst.i <= rconst.i) ? 1 : 0;
          break;
        case Bgt:
          result = (lconst.i > rconst.i) ? 1 : 0;
          break;
        case Bge:
          result = (lconst.i >= rconst.i) ? 1 : 0;
          break;
        case Badd:
          result = lconst.i + rconst.i;
          break;
        case Bsub:
          result = lconst.i - rconst.i;
          break;
        case Bmul:
          result = lconst.i * rconst.i;
          break;
        case Bdiv:
          if (rconst.i == 0) {
            Ebinop eb = new Ebinop(ebinop.b, lhs, rhs);
            eb.typ = ebinop.typ;
            this.result = eb;
            return;
          }
          result = lconst.i / rconst.i;
          break;
        case Bmod:
          if (rconst.i == 0) {
            Ebinop eb = new Ebinop(ebinop.b, lhs, rhs);
            eb.typ = ebinop.typ;
            this.result = eb;
            return;
          }
          result = lconst.i % rconst.i;
          break;
        case Band:
          result = (lconst.i != 0 && rconst.i != 0) ? 1 : 0;
          break;
        case Bor:
          result = (lconst.i != 0 || rconst.i != 0) ? 1 : 0;
          break;
      }

      Econst ec = new Econst(result);
      ec.typ = Tint.tint;
      this.result = ec;
    } else {
      Ebinop eb = new Ebinop(ebinop.b, lhs, rhs);
      eb.typ = ebinop.typ;
      this.result = eb;
    }
  }

  @Override
  public void visit(Ecall ecall) {
    LinkedList<Expr> args = new LinkedList<>();
    for (Expr arg : ecall.el) {
      args.add(transform(arg));
    }
    Ecall ec = new Ecall(ecall.i, args);
    ec.typ = ecall.typ;
    this.result = ec;
  }

  @Override
  public void visit(Esizeof esizeof) {
    Econst ec = new Econst(esizeof.s.getSize());
    ec.typ = Tint.tint;
    this.result = ec;
  }

  @Override
  public void visit(Sskip sskip) {
    this.result = sskip;
  }

  @Override
  public void visit(Sbreak sbreak) {
    this.result = sbreak;
  }

  @Override
  public void visit(Scontinue scontinue) {
    this.result = scontinue;
  }

  @Override
  public void visit(Sexpr sexpr) {
    this.result = new Sexpr(transform(sexpr.e));
  }

  @Override
  public void visit(Sif sif) {
    this.result = new Sif(transform(sif.e), transform(sif.s1), transform(sif.s2));
  }

  @Override
  public void visit(Swhile swhile) {
    this.result = new Swhile(transform(swhile.e), transform(swhile.s));
  }

  @Override
  public void visit(Sblock sblock) {
    LinkedList<Stmt> stmts = new LinkedList<>();
    for (Stmt stmt : sblock.sl) {
      stmts.add(transform(stmt));
    }
    this.result = new Sblock(sblock.dl, stmts);
  }

  @Override
  public void visit(Sreturn sreturn) {
    this.result = new Sreturn(transform(sreturn.e));
  }

  @Override
  public void visit(Decl_fun decl_fun) {
    this.result = new Decl_fun(
        decl_fun.fun_typ,
        decl_fun.fun_name,
        decl_fun.fun_formals,
        transform(decl_fun.fun_body)
    );
  }

  @Override
  public void visit(File file) {
    LinkedList<Decl_fun> funs = new LinkedList<>();
    for (Decl_fun fun : file.funs) {
      funs.add(transform(fun));
    }
    this.result = new File(funs);
  }

  @Override
  public void visit(Sfor sfor) {
    this.result = new Sfor(
        transform(sfor.pre),
        transform(sfor.cond),
        transform(sfor.post),
        transform(sfor.body)
    );
  }

  private Typ transform(Typ typ) {
    typ.accept(this);
    assert this.result != null && this.result instanceof Typ;
    return (Typ) this.result;
  }

  private Structure transform(Structure structure) {
    structure.accept(this);
    assert this.result != null && this.result instanceof Structure;
    return (Structure) this.result;
  }

  private Field transform(Field field) {
    field.accept(this);
    assert this.result != null && this.result instanceof Field;
    return (Field) this.result;
  }

  private Decl_var transform(Decl_var decl_var) {
    decl_var.accept(this);
    assert this.result != null && this.result instanceof Decl_var;
    return (Decl_var) this.result;
  }

  private Expr transform(Expr expr) {
    expr.accept(this);
    assert this.result != null && this.result instanceof Expr;
    Expr e = (Expr) this.result;
    assert e.typ != null;
    return e;
  }

  private Stmt transform(Stmt stmt) {
    stmt.accept(this);
    assert this.result != null && this.result instanceof Stmt;
    return (Stmt) this.result;
  }

  private Decl_fun transform(Decl_fun decl_fun) {
    decl_fun.accept(this);
    assert this.result != null && this.result instanceof Decl_fun;
    return (Decl_fun) this.result;
  }

  public File transform(File file) {
    file.accept(this);
    assert this.result != null && this.result instanceof File;
    return (File) this.result;
  }
}
