package mini_c;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Typing implements Pvisitor {

  private Decl_fun decl_fun = null;
  private Typ typ = null;
  private final LinkedList<Sblock> blockStack = new LinkedList<>();
  private Sblock lastBlock = null;
  private Stmt stmt = null;
  private Expr expr = null;

  private final HashMap<String, Structure> declaredStructures = new HashMap<>();

  // le résultat du typage sera mis dans cette variable
  private File file;

  // et renvoyé par cette fonction
  File getFile() {
    if (file == null) {
      throw new Error("typing not yet done!");
    }
    return file;
  }

  // il faut compléter le visiteur ci-dessous pour réaliser le typage

  @Override
  public void visit(Pfile n) {
    LinkedList<Decl_fun> functionDeclarations = new LinkedList<>();
    file = new File(functionDeclarations);

    addPredefinedFunctions();

    for (Pdecl pdecl : n.l) {
      pdecl.accept(this);
    }

    resolveFunction("main");
  }

  private void addPredefinedFunctions() {
    Pfun functions[] = new Pfun[]{
        new Pfun(
            Ptype.ptint,
            new Pstring("putchar", null),
            new LinkedList<Pdeclvar>(Arrays.asList(new Pdeclvar(
                Ptype.ptint,
                new Pstring("c", null)
            ))),
            new Pbloc(
                new LinkedList<>(),
                new LinkedList<>(),
                null)
        ),
        new Pfun(
            Ptype.ptvoidstar,
            new Pstring("sbrk", null),
            new LinkedList<Pdeclvar>(Arrays.asList(new Pdeclvar(
                Ptype.ptint,
                new Pstring("n", null)
            ))),
            new Pbloc(
                new LinkedList<>(),
                new LinkedList<>(),
                null)
        ),
    };

    for (Pfun pfun : functions) {
      pfun.accept(this);
    }
  }

  @Override
  public void visit(PTint n) {
    this.typ = Tint.tint;
  }

  @Override
  public void visit(PTstruct n) {
    this.typ = new Tstructp(resolveStructure(n.id));
  }

  @Override
  public void visit(PTvoidstar n) {
    this.typ = Tvoidstar.tvoidstar;
  }

  @Override
  public void visit(PTypenull n) {
    this.typ = Ttypenull.ttypenull;
  }

  @Override
  public void visit(Pint n) {
    this.expr = new Econst(n.n);
    this.expr.typ = n.n == 0 ? Ttypenull.ttypenull : Tint.tint;
  }

  @Override
  public void visit(Pident n) {
    this.expr = new Eaccess_local(n.id);
    this.expr.typ = resolveLocalType(n.id);
  }

  @Override
  public void visit(Punop n) {
    Expr old_expr = this.expr;
    n.e1.accept(this);
    Expr e = this.expr;
    assert e != null;
    assert e.typ != null;
    this.expr = old_expr;

    Typ type = resolveUnopType(n.op, e.typ);
    this.expr = new Eunop(n.op, e);
    this.expr.typ = type;
  }

  @Override
  public void visit(Passign n) {
    n.e2.accept(this);
    Expr e2 = this.expr;
    assert e2 != null;
    assert e2.typ != null;

    if (n.e1 instanceof Pident) {
      this.expr = new Eassign_local(((Pident) n.e1).id, e2);
    } else {
      Parrow p_arrow = (Parrow) n.e1;
      p_arrow.e.accept(this);
      Expr e1 = this.expr;
      Field f = resolveField(e1.typ, p_arrow.f);
      this.expr = new Eassign_field(e1, f, e2);
    }
    this.expr.typ = e2.typ;
  }

  @Override
  public void visit(Pbinop n) {
    Expr old_expr = this.expr;
    n.e1.accept(this);
    Expr e1 = this.expr;
    assert e1 != null;
    assert e1.typ != null;
    this.expr = old_expr;

    n.e2.accept(this);
    Expr e2 = this.expr;
    assert e2 != null;
    assert e2.typ != null;
    this.expr = old_expr;

    Typ type = resolveBinopType(n.op, e1.typ, e2.typ);

    this.expr = new Ebinop(n.op, e1, e2);
    this.expr.typ = type;
  }

  @Override
  public void visit(Parrow n) {
    n.e.accept(this);
    Expr e = this.expr;
    assert e != null;
    assert e.typ != null;

    Eaccess_field eaccess_field = new Eaccess_field(e, resolveField(e.typ, n.f));
    this.expr = eaccess_field;
    this.expr.typ = eaccess_field.f.field_typ;
  }

  private LinkedList<Expr> collect_expr(LinkedList<Pexpr> pexpr_list) {
    LinkedList<Expr> expr_list = new LinkedList<>();

    for (Pexpr pexpr : pexpr_list) {
      Expr old_expr = this.expr;
      pexpr.accept(this);
      expr_list.add(this.expr);
      this.expr = old_expr;
    }

    return expr_list;
  }

  @Override
  public void visit(Pcall n) {
    LinkedList<Expr> expr_list = collect_expr(n.l);
    this.expr = new Ecall(n.f, expr_list);
    Decl_fun decl_fun = resolveFunction(n.f);
    verifyFunctionArguments(decl_fun, expr_list);
    this.expr.typ = decl_fun.fun_typ;
  }

  @Override
  public void visit(Psizeof n) {
    this.expr = new Esizeof(resolveStructure(n.id));
    this.expr.typ = Tint.tint;
  }

  @Override
  public void visit(Pskip n) {
    this.stmt = new Sskip();
  }

  @Override
  public void visit(Peval n) {
    Expr old_expr = this.expr;
    n.e.accept(this);
    Expr expr = this.expr;
    assert expr != null;
    assert expr.typ != null;
    this.expr = old_expr;

    this.stmt = new Sexpr(expr);
  }

  @Override
  public void visit(Pif n) {
    Expr old_expr = this.expr;
    n.e.accept(this);
    Expr expr = this.expr;
    this.expr = old_expr;

    n.s1.accept(this);
    Stmt stmt1 = this.stmt;

    n.s2.accept(this);
    Stmt stmt2 = this.stmt;

    this.stmt = new Sif(expr, stmt1, stmt2);
  }

  @Override
  public void visit(Pwhile n) {
    Expr old_expr = this.expr;
    n.e.accept(this);
    Expr e = this.expr;
    this.expr = old_expr;

    n.s1.accept(this);
    Stmt s = this.stmt;

    this.stmt = new Swhile(e, s);
  }

  private void collect_stmt(LinkedList<Pstmt> pstmt_list, LinkedList<Stmt> result) {
    for (Pstmt pstmt : pstmt_list) {
      Stmt old_stmt = this.stmt;
      pstmt.accept(this);
      result.add(this.stmt);
      this.stmt = old_stmt;
    }
  }

  @Override
  public void visit(Pbloc n) {
    LinkedList<Decl_var> decl_var = new LinkedList<>();
    LinkedList<Stmt> stmts = new LinkedList<>();
    Sblock block = new Sblock(decl_var, stmts);

    blockStack.push(block);

    collect_decl_var(n.vl, decl_var);
    collect_stmt(n.sl, stmts);

    this.stmt = this.lastBlock = blockStack.pop();
  }

  @Override
  public void visit(Preturn n) {
    Expr old_expr = this.expr;
    n.e.accept(this);
    this.stmt = new Sreturn(this.expr);
    this.expr = old_expr;
  }

  @Override
  public void visit(Pstruct n) {
    if (findStructure(n.s) != null) {
      throw new Error("Redefinition of structure " + n.s);
    }

    Structure structure = new Structure(n.s);
    declaredStructures.put(structure.str_name, structure);

    LinkedList<Decl_var> decl_var_list = new LinkedList<>();
    collect_decl_var(n.fl, decl_var_list);

    for (Decl_var decl_var : decl_var_list) {
      structure.fields.put(decl_var.name, new Field(decl_var.name, decl_var.t));
    }
  }

  private void collect_decl_var(LinkedList<Pdeclvar> pdeclvar_list, LinkedList<Decl_var> result) {
    for (Pdeclvar pdeclvar : pdeclvar_list) {
      Typ old_typ = this.typ;
      pdeclvar.typ.accept(this);
      if (findDeclVar(result, pdeclvar.id) != null) {
        throw new Error("Redefinition of variable or field: " + pdeclvar.id);
      }
      result.add(new Decl_var(this.typ, pdeclvar.id));
      this.typ = old_typ;
    }
  }

  @Override
  public void visit(Pfun n) {
    if (findFunction(n.s) != null) {
      throw new Error("Redefinition of function " + n.s);
    }

    Typ old_typ = this.typ;
    n.ty.accept(this);
    Typ typ = this.typ;
    this.typ = old_typ;

    LinkedList<Decl_var> decl_var = new LinkedList<>();
    collect_decl_var(n.pl, decl_var);

    this.decl_fun = new Decl_fun(typ, n.s, decl_var, null);
    this.file.funs.add(this.decl_fun);

    n.b.accept(this);
    assert this.lastBlock != null;
    this.decl_fun.fun_body = this.lastBlock;

    this.decl_fun = null;
  }

  private static Decl_var findDeclVar(LinkedList<Decl_var> declVars, String varName) {
    for (Decl_var decl_var : declVars) {
      if (decl_var.name.equals(varName)) {
        return decl_var;
      }
    }
    return null;
  }

  private Typ resolveLocalType(String localName) {
    for (Sblock block : blockStack) {
      Decl_var decl_var = findDeclVar(block.dl, localName);
      if (decl_var != null) {
        return decl_var.t;
      }
    }

    Decl_var decl_var = findDeclVar(this.decl_fun.fun_formals, localName);
    if (decl_var != null) {
      return decl_var.t;
    }

    throw new Error("Undeclared variable: " + localName);
  }

  private Field resolveField(Typ type, String fieldName) {
    if (!(type instanceof Tstructp)) {
      throw new Error("Not a struct: " + type);
    }

    Tstructp structType = (Tstructp) type;
    assert structType.s != null;

    if (!structType.s.fields.containsKey(fieldName)) {
      throw new Error("Struct " + structType.s.str_name + " does not have field " + fieldName);
    }

    return structType.s.fields.get(fieldName);
  }

  private Decl_fun findFunction(String functionName) {
    for (Decl_fun decl_fun : this.file.funs) {
      if (decl_fun.fun_name.equals(functionName)) {
        return decl_fun;
      }
    }
    return null;
  }

  private Decl_fun resolveFunction(String functionName) {
    Decl_fun decl_fun = findFunction(functionName);
    if (decl_fun != null) {
      return decl_fun;
    }
    throw new Error("Function not declared: " + functionName);
  }

  private Structure findStructure(String structureName) {
    return declaredStructures.get(structureName);
  }

  private Structure resolveStructure(String structureName) {
    Structure s = findStructure(structureName);
    if (s != null) {
      return s;
    }
    throw new Error("Structure not declared: " + structureName);
  }

  private Typ resolveBinopType(Binop op, Typ lhs, Typ rhs) {
    switch (op) {
      case Beq:
      case Bneq:
      case Blt:
      case Ble:
      case Bgt:
      case Bge:
        if (lhs.equals(rhs)) {
          return Tint.tint;
        }
        break;
      case Badd:
      case Bsub:
      case Bmul:
      case Bdiv:
        if (Tint.tint.equals(lhs) && Tint.tint.equals(rhs)) {
          return Tint.tint;
        }
        break;
      case Band:
      case Bor:
        return Tint.tint;
    }

    throw new Error(
        "Operator " + op + " is not defined for arguments of type `" + lhs + "` and `" + rhs + "`");
  }

  private Typ resolveUnopType(Unop op, Typ argType) {
    switch (op) {
      case Uneg:
        if (Tint.tint.equals(argType)) {
          return Tint.tint;
        }
        break;
      case Unot:
        return Tint.tint;
    }

    throw new Error("Operator " + op + " is not defined for argument of type `" + argType + "`");
  }

  private void verifyFunctionArguments(Decl_fun decl_fun, LinkedList<Expr> args) {
    if (decl_fun.fun_formals.size() != args.size()) {
      throw new Error(
          "In function " + decl_fun.fun_name + ": expected " + decl_fun.fun_formals.size()
              + " arguments, got " + args.size());
    }

    Iterator<Decl_var> paramIter = decl_fun.fun_formals.iterator();
    Iterator<Expr> argIter = args.iterator();

    int argIndex = 0;

    while (paramIter.hasNext()) {
      Decl_var param = paramIter.next();
      Expr arg = argIter.next();

      if (!param.t.equals(arg.typ)) {
        throw new Error(
            "Argument " + (argIndex + 1) + ": expected type `" + param.t + "`, got `" + arg.typ
                + "`");
      }

      ++argIndex;
    }
  }
}
