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
  private int loopCount = 0;

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
  public void visit(Pfile n) throws SyntaxError, TypeError {
    LinkedList<Decl_fun> functionDeclarations = new LinkedList<>();
    file = new File(functionDeclarations);

    addPredefinedFunctions();

    for (Pdecl pdecl : n.l) {
      pdecl.accept(this);
    }

    resolveFunction(new Pstring("main", Loc.nullLoc));
  }

  @Override
  public void visit(Pfor n) throws SyntaxError, TypeError {
    Expr old_expr = this.expr;

    n.pre.accept(this);
    Expr pre = this.expr;

    n.cond.accept(this);
    Expr cond = this.expr;

    n.post.accept(this);
    Expr post = this.expr;

    this.expr = old_expr;

    ++loopCount;

    n.body.accept(this);
    Stmt body = this.stmt;

    this.stmt = new Sfor(pre, cond, post, body);

    --loopCount;
  }

  private void addPredefinedFunctions() throws SyntaxError, TypeError {
    Pfun functions[] = new Pfun[]{
        new Pfun(
            Ptype.ptint,
            new Pstring("putchar", Loc.nullLoc),
            new LinkedList<Pdeclvar>(Arrays.asList(new Pdeclvar(
                Ptype.ptint,
                new Pstring("c", Loc.nullLoc)
            ))),
            new Pbloc(
                new LinkedList<>(),
                new LinkedList<>(),
                Loc.nullLoc)
        ),
        new Pfun(
            Ptype.ptvoidstar,
            new Pstring("sbrk", Loc.nullLoc),
            new LinkedList<Pdeclvar>(Arrays.asList(new Pdeclvar(
                Ptype.ptint,
                new Pstring("n", Loc.nullLoc)
            ))),
            new Pbloc(
                new LinkedList<>(),
                new LinkedList<>(),
                Loc.nullLoc)
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
  public void visit(PTstruct n) throws SyntaxError {
    this.typ = new Tstructp(resolveStructure(new Pstring(n.id, n.loc)));
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
  public void visit(Pident n) throws SyntaxError {
    this.expr = new Eaccess_local(n.id);
    this.expr.typ = resolveLocalType(n.id);
  }

  @Override
  public void visit(Punop n) throws SyntaxError, TypeError {
    Expr old_expr = this.expr;
    n.e1.accept(this);
    Expr e = this.expr;
    assert e != null;
    assert e.typ != null;
    this.expr = old_expr;

    Typ type = resolveUnopType(n.op, e.typ, n.loc);
    this.expr = new Eunop(n.op, e);
    this.expr.typ = type;
  }

  @Override
  public void visit(Passign n) throws TypeError, SyntaxError {
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
  public void visit(Pbinop n) throws SyntaxError, TypeError {
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

    Typ type = resolveBinopType(n.op, e1.typ, e2.typ, n.loc);

    this.expr = new Ebinop(n.op, e1, e2);
    this.expr.typ = type;
  }

  @Override
  public void visit(Parrow n) throws TypeError, SyntaxError {
    n.e.accept(this);
    Expr e = this.expr;
    assert e != null;
    assert e.typ != null;

    Eaccess_field eaccess_field = new Eaccess_field(e, resolveField(e.typ, n.f));
    this.expr = eaccess_field;
    this.expr.typ = eaccess_field.f.field_typ;
  }

  private LinkedList<Expr> collect_expr(LinkedList<Pexpr> pexpr_list)
      throws SyntaxError, TypeError {
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
  public void visit(Pcall n) throws SyntaxError, TypeError {
    LinkedList<Expr> expr_list = collect_expr(n.l);
    this.expr = new Ecall(n.f, expr_list);
    Decl_fun decl_fun = resolveFunction(new Pstring(n.f, n.loc));
    verifyFunctionArguments(decl_fun, expr_list, n.loc);
    this.expr.typ = decl_fun.fun_typ;
  }

  @Override
  public void visit(Psizeof n) throws SyntaxError {
    this.expr = new Esizeof(resolveStructure(n.id));
    this.expr.typ = Tint.tint;
  }

  @Override
  public void visit(Pskip n) {
    this.stmt = new Sskip();
  }

  @Override
  public void visit(Pbreak n) throws SyntaxError {
    if (this.loopCount == 0) {
      throw new SyntaxError("break not in a loop", n.loc);
    }

    this.stmt = new Sbreak();
  }

  @Override
  public void visit(Pcontinue n) throws SyntaxError {
    if (this.loopCount == 0) {
      throw new SyntaxError("continue not in a loop", n.loc);
    }

    this.stmt = new Scontinue();
  }

  @Override
  public void visit(Peval n) throws SyntaxError, TypeError {
    Expr old_expr = this.expr;
    n.e.accept(this);
    Expr expr = this.expr;
    assert expr != null;
    assert expr.typ != null;
    this.expr = old_expr;

    this.stmt = new Sexpr(expr);
  }

  @Override
  public void visit(Pif n) throws SyntaxError, TypeError {
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
  public void visit(Pwhile n) throws SyntaxError, TypeError {
    Expr old_expr = this.expr;
    n.e.accept(this);
    Expr e = this.expr;
    this.expr = old_expr;

    ++this.loopCount;

    n.s1.accept(this);
    Stmt s = this.stmt;

    this.stmt = new Swhile(e, s);
    --this.loopCount;
  }

  private void collect_stmt(LinkedList<Pstmt> pstmt_list, LinkedList<Stmt> result)
      throws SyntaxError, TypeError {
    for (Pstmt pstmt : pstmt_list) {
      Stmt old_stmt = this.stmt;
      pstmt.accept(this);
      result.add(this.stmt);
      this.stmt = old_stmt;
    }
  }

  @Override
  public void visit(Pbloc n) throws SyntaxError, TypeError {
    LinkedList<Decl_var> decl_var = new LinkedList<>();
    LinkedList<Stmt> stmts = new LinkedList<>();
    Sblock block = new Sblock(decl_var, stmts);

    blockStack.push(block);

    collect_decl_var(n.vl, decl_var);
    collect_stmt(n.sl, stmts);

    this.stmt = this.lastBlock = blockStack.pop();
  }

  @Override
  public void visit(Preturn n) throws SyntaxError, TypeError {
    Expr old_expr = this.expr;
    n.e.accept(this);
    if (!this.expr.typ.equals(this.decl_fun.fun_typ)) {
      throw new TypeError("Return value type differs from function type",n.loc);
    }
    this.stmt = new Sreturn(this.expr);
    this.expr = old_expr;
  }

  @Override
  public void visit(Pstruct n) throws SyntaxError, TypeError {
    if (findStructure(n.s.toString()) != null) {
      throw new SyntaxError("Redefinition of structure " + n.s, n.s.loc);
    }

    Structure structure = new Structure(n.s.id);
    declaredStructures.put(structure.str_name, structure);

    LinkedList<Decl_var> decl_var_list = new LinkedList<>();
    collect_decl_var(n.fl, decl_var_list);

    for (Decl_var decl_var : decl_var_list) {
      structure.addField(new Field(decl_var.name, decl_var.t));
    }
  }

  private void collect_decl_var(LinkedList<Pdeclvar> pdeclvar_list, LinkedList<Decl_var> result)
      throws SyntaxError {
    for (Pdeclvar pdeclvar : pdeclvar_list) {
      Typ old_typ = this.typ;
      pdeclvar.typ.accept(this);
      if (findDeclVar(result, pdeclvar.id) != null) {
        throw new SyntaxError("Redefinition of variable or field: " + pdeclvar.id, pdeclvar.loc);
      }
      result.add(new Decl_var(this.typ, new Pstring(pdeclvar.id, pdeclvar.loc)));
      this.typ = old_typ;
    }
  }

  @Override
  public void visit(Pfun n) throws SyntaxError, TypeError {
    if (findFunction(n.s) != null) {
      throw new SyntaxError("Redefinition of function " + n.s, n.loc);
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
      if (decl_var.name.id.equals(varName)) {
        return decl_var;
      }
    }
    return null;
  }

  private Typ resolveLocalType(Pstring localName) throws SyntaxError {
    for (Sblock block : blockStack) {
      Decl_var decl_var = findDeclVar(block.dl, localName.id);
      if (decl_var != null) {
        return decl_var.t;
      }
    }

    Decl_var decl_var = findDeclVar(this.decl_fun.fun_formals, localName.id);
    if (decl_var != null) {
      return decl_var.t;
    }

    throw new SyntaxError("Undeclared variable: " + localName, localName.loc);
  }

  private Field resolveField(Typ type, Pstring fieldName) throws TypeError, SyntaxError {
    if (!(type instanceof Tstructp)) {
      throw new SyntaxError("Not a struct: " + type, fieldName.loc);
    }

    Tstructp structType = (Tstructp) type;
    assert structType.s != null;

    Field field = structType.s.getField(fieldName.id);
    if (field == null) {
      throw new TypeError("Struct " + structType.s.str_name + " does not have field " + fieldName, fieldName.loc);
    }

    return field;
  }

  private Decl_fun findFunction(String functionName) {
    for (Decl_fun decl_fun : this.file.funs) {
      if (decl_fun.fun_name.equals(functionName)) {
        return decl_fun;
      }
    }
    return null;
  }

  private Decl_fun resolveFunction(Pstring functionName) throws SyntaxError {
    Decl_fun decl_fun = findFunction(functionName.id);
    if (decl_fun != null) {
      return decl_fun;
    }
    throw new SyntaxError("Function not declared: " + functionName, functionName.loc);
  }

  private Structure findStructure(String structureName) {
    return declaredStructures.get(structureName);
  }

  private Structure resolveStructure(Pstring structureName) throws SyntaxError {
    Structure s = findStructure(structureName.id);
    if (s != null) {
      return s;
    }
    throw new SyntaxError("Structure not declared: " + structureName, structureName.loc);
  }

  private Typ resolveBinopType(Binop op, Typ lhs, Typ rhs, Loc location) throws TypeError {
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
      case Bmod:
        if (Tint.tint.equals(lhs) && Tint.tint.equals(rhs)) {
          return Tint.tint;
        }
        break;
      case Band:
      case Bor:
        return Tint.tint;
    }

    throw new TypeError(
        "Operator " + op + " is not defined for arguments of type `" + lhs + "` and `" + rhs + "`", location);
  }

  private Typ resolveUnopType(Unop op, Typ argType, Loc location) throws TypeError {
    switch (op) {
      case Uneg:
        if (Tint.tint.equals(argType)) {
          return Tint.tint;
        }
        break;
      case Unot:
        return Tint.tint;
    }

    throw new TypeError("Operator " + op + " is not defined for argument of type `" + argType + "`", location);
  }

  private void verifyFunctionArguments(Decl_fun decl_fun, LinkedList<Expr> args, Loc location) throws TypeError {
    if (decl_fun.fun_formals.size() != args.size()) {
      throw new TypeError(
          "In function " + decl_fun.fun_name + ": expected " + decl_fun.fun_formals.size()
              + " arguments, got " + args.size(), location);
    }

    Iterator<Decl_var> paramIter = decl_fun.fun_formals.iterator();
    Iterator<Expr> argIter = args.iterator();

    int argIndex = 0;

    while (paramIter.hasNext()) {
      Decl_var param = paramIter.next();
      Expr arg = argIter.next();

      if (!param.t.equals(arg.typ)) {
        throw new TypeError(
            "Argument " + (argIndex + 1) + ": expected type `" + param.t + "`, got `" + arg.typ
                + "`", location);
      }

      ++argIndex;
    }
  }
}
