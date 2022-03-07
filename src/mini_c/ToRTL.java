package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ToRTL implements Visitor {

  private RTLfile rtlFile = null;
  private RTLfun rtlFun = null;
  private Label exitPoint = null;

  private ScopeStack scopeStack = new ScopeStack();
  private LinkedList<Register> exprValueStack = new LinkedList<>();

  private static final HashSet<String> specialFunctions = new HashSet<>();
  static {
    specialFunctions.add("sbrk");
    specialFunctions.add("putchar");
  }

  public RTLfile translate(File typedProgram) {
    rtlFile = new RTLfile();
    typedProgram.accept(this);
    RTLfile result = rtlFile;
    rtlFile = null;
    return result;
  }

  @Override
  public void visit(Unop unop) {
    throw new Error("Not implemented yet: Unop");
  }

  @Override
  public void visit(Binop binop) {
    throw new Error("Not implemented yet: Binop");
  }

  @Override
  public void visit(String string) {
    throw new Error("Not implemented yet: String");
  }

  @Override
  public void visit(Tint tint) {
    throw new Error("Not implemented yet: Tint");
  }

  @Override
  public void visit(Tstructp tstructp) {
    throw new Error("Not implemented yet: Tstructp");
  }

  @Override
  public void visit(Tvoidstar tvoidstar) {
    throw new Error("Not implemented yet: Tvoidstar");
  }

  @Override
  public void visit(Ttypenull ttypenull) {
    throw new Error("Not implemented yet: Ttypenull");
  }

  @Override
  public void visit(Structure structure) {
    throw new Error("Not implemented yet: Structure");
  }

  @Override
  public void visit(Field field) {
    throw new Error("Not implemented yet: Field");
  }

  @Override
  public void visit(Decl_var decl_var) {
    throw new Error("Not implemented yet: Decl_var");
  }

  @Override
  public void visit(Expr expr) {
    throw new Error("Not implemented yet: Expr");
  }

  @Override
  public void visit(Econst econst) {
    Register reg = exprValueStack.pop();
    this.exitPoint = this.rtlFun.body.add(new Rconst(econst.i, reg, this.exitPoint));
  }

  @Override
  public void visit(Eaccess_local eaccess_local) {
    Register out = exprValueStack.pop();
    Register in = scopeStack.resolve(eaccess_local.i);

    this.exitPoint = this.rtlFun.body.add(new Rmbinop(Mbinop.Mmov, in, out, this.exitPoint));
  }

  @Override
  public void visit(Eaccess_field eaccess_field) {
    Register out = exprValueStack.pop();
    Register in = new Register();

    this.exitPoint = this.rtlFun.body.add(new Rload(in, eaccess_field.f.getOffset(), out, this.exitPoint));

    exprValueStack.push(in);
    eaccess_field.e.accept(this);
  }

  @Override
  public void visit(Eassign_local eassign_local) {
    Register out = this.exprValueStack.pop();
    Register var = scopeStack.resolve(eassign_local.i);

    this.exprValueStack.push(var);

    this.exitPoint = this.rtlFun.body.add(new Rmbinop(Mbinop.Mmov, var, out, this.exitPoint));

    eassign_local.e.accept(this);
  }

  @Override
  public void visit(Eassign_field eassign_field) {
    Register out = exprValueStack.pop();

    Register lhs = new Register();
    Register rhs = out;

    this.exitPoint = this.rtlFun.body.add(new Rstore(rhs, lhs, eassign_field.f.getOffset(), this.exitPoint));

    exprValueStack.push(lhs);
    eassign_field.e1.accept(this);

    exprValueStack.push(rhs);
    eassign_field.e2.accept(this);
  }

  @Override
  public void visit(Eunop eunop) {
    Register outVal = exprValueStack.pop();

    if (eunop.u == Unop.Uneg) {
      Register inVal = new Register();

      exprValueStack.push(inVal);

      this.exitPoint = this.rtlFun.body
          .add(new Rmbinop(Mbinop.Msub, inVal, outVal, this.exitPoint));
      this.exitPoint = this.rtlFun.body.add(new Rconst(0, outVal, this.exitPoint));
    } else if (eunop.u == Unop.Unot) {
      exprValueStack.push(outVal);

      this.exitPoint = this.rtlFun.body.add(new Rmunop(new Msetei(0), outVal, this.exitPoint));
    } else {
      throw new Error("Not implemented yet: " + eunop.u);
    }

    eunop.e.accept(this);
  }

  @Override
  public void visit(Ebinop ebinop) {
    Register lhs = exprValueStack.pop();
    Register rhs = new Register();

    exprValueStack.push(lhs);
    exprValueStack.push(rhs);

    if (ebinop.b == Binop.Band || ebinop.b == Binop.Bor) {
      Label exitPoint = this.exitPoint;

      Label castBool = this.rtlFun.body.add(new Rmunop(new Msetnei(0), lhs,exitPoint));
      Label returnRhs = this.rtlFun.body.add(new Rmbinop(Mbinop.Mmov, rhs, lhs, castBool));
      this.exitPoint = returnRhs;
      ebinop.e2.accept(this);
      Label evalRhs = this.exitPoint;

      Label returnLhs = castBool;
      Mubranch branchOp = (ebinop.b == Binop.Band ? new Mjz() : new Mjnz());
      Label shortCircuit = this.rtlFun.body.add(new Rmubranch(branchOp, lhs, returnLhs, evalRhs));

      this.exitPoint = shortCircuit;
      ebinop.e1.accept(this);

      return;
    }

    Mbinop opCode;
    switch (ebinop.b) {
      case Beq:
        opCode = Mbinop.Msete;
        break;
      case Bneq:
        opCode = Mbinop.Msetne;
        break;
      case Blt:
        opCode = Mbinop.Msetl;
        break;
      case Ble:
        opCode = Mbinop.Msetle;
        break;
      case Bgt:
        opCode = Mbinop.Msetg;
        break;
      case Bge:
        opCode = Mbinop.Msetge;
        break;
      case Badd:
        opCode = Mbinop.Madd;
        break;
      case Bsub:
        opCode = Mbinop.Msub;
        break;
      case Bmul:
        opCode = Mbinop.Mmul;
        break;
      case Bdiv:
        opCode = Mbinop.Mdiv;
        break;
      default:
        throw new Error("Not implemented yet: " + ebinop.b);
    }

    Label opLabel = rtlFun.body.add(new Rmbinop(opCode, rhs, lhs, this.exitPoint));
    this.exitPoint = opLabel;

    ebinop.e2.accept(this);
    ebinop.e1.accept(this);
  }

  @Override
  public void visit(Ecall ecall) {
    Register result = exprValueStack.pop();
    LinkedList<Register> arguments = new LinkedList<>();
    this.exitPoint = this.rtlFun.body.add(new Rcall(result, ecall.i, arguments, this.exitPoint));

    for (Expr expr : ecall.el) {
      Register arg = new Register();
      arguments.add(arg);
      exprValueStack.push(arg);
      expr.accept(this);
    }
  }

  @Override
  public void visit(Esizeof esizeof) {
    Register reg = exprValueStack.pop();
    this.exitPoint = this.rtlFun.body.add(new Rconst(esizeof.s.getSize(), reg, this.exitPoint));
  }

  @Override
  public void visit(Sskip sskip) {
    // Do nothing, YAY !!!
  }

  @Override
  public void visit(Sexpr sexpr) {
    exprValueStack.push(new Register());
    sexpr.e.accept(this);
  }

  @Override
  public void visit(Sif sif) {
    Label exitPoint = this.exitPoint;

    sif.s2.accept(this);
    Label elseEntry = this.exitPoint;
    this.exitPoint = exitPoint;

    sif.s1.accept(this);
    Label thenEntry = this.exitPoint;

    Register val = new Register();
    exprValueStack.push(val);

    this.exitPoint = this.rtlFun.body.add(new Rmubranch(new Mjz(), val, elseEntry, thenEntry));

    sif.e.accept(this);
  }

  @Override
  public void visit(Swhile swhile) {
    Label exitPoint = this.exitPoint;

    Label gotoHead = this.rtlFun.body.add(null); // To be filled later
    this.exitPoint = gotoHead;
    swhile.s.accept(this);
    Label bodyBegin = this.exitPoint;

    Register val = new Register();
    exprValueStack.push(val);

    this.exitPoint = this.rtlFun.body.add(new Rmubranch(new Mjz(), val, exitPoint, bodyBegin));
    swhile.e.accept(this);

    this.rtlFun.body.replace(gotoHead, new Rgoto(this.exitPoint));
  }

  @Override
  public void visit(Sblock sblock) {
    scopeStack.openScope(sblock.dl);
    for (Decl_var decl_var : sblock.dl) {
      Register reg = scopeStack.resolve(decl_var.name);
      rtlFun.locals.add(reg);
    }

    Iterator<Stmt> iter = sblock.sl.descendingIterator();
    while (iter.hasNext()) {
      Stmt stmt = iter.next();
      stmt.accept(this);
    }

    scopeStack.closeScope();
  }

  @Override
  public void visit(Sreturn sreturn) {
    assert rtlFun != null;

    assert exprValueStack.isEmpty();

    exprValueStack.push(rtlFun.result);
    this.exitPoint = rtlFun.exit;
    sreturn.e.accept(this);

    assert exprValueStack.isEmpty();
  }

  @Override
  public void visit(Decl_fun decl_fun) {
    assert this.rtlFun != null;
    rtlFun.body = new RTLgraph();
    scopeStack.openScope(decl_fun.fun_formals);

    for (Decl_var decl_var : decl_fun.fun_formals) {
      Register reg = scopeStack.resolve(decl_var.name);
      rtlFun.formals.add(reg);
    }

    rtlFun.result = new Register();

    rtlFun.exit = rtlFun.body.add(null);
    this.exitPoint = rtlFun.exit;

    decl_fun.fun_body.accept(this);

    rtlFun.entry = this.exitPoint;
    this.exitPoint = null;

    scopeStack.closeScope();
  }

  @Override
  public void visit(File file) {
    assert rtlFile != null;
    for (Decl_fun decl_fun : file.funs) {
      if (specialFunctions.contains(decl_fun.fun_name)) {
        continue;
      }
      this.rtlFun = new RTLfun(decl_fun.fun_name);
      decl_fun.accept(this);
      rtlFile.funs.add(this.rtlFun);
      this.rtlFun = null;
    }
  }
}

class ScopeStack {

  private final LinkedList<HashMap<String, Register>> scopes = new LinkedList<>();

  public void openScope(List<Decl_var> variableDeclarations) {
    HashMap<String, Register> scope = new HashMap<>();
    for (Decl_var decl_var : variableDeclarations) {
      scope.put(decl_var.name, new Register());
    }
    scopes.push(scope);
  }

  public Register resolve(String name) {
    for (HashMap<String, Register> scope : scopes) {
      Register reg = scope.get(name);
      if (reg != null) {
        return reg;
      }
    }
    throw new Error("Variable not found: " + name);
  }

  public void closeScope() {
    scopes.pop();
  }
}