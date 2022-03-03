package mini_c;

import java.util.LinkedList;

public class Typing implements Pvisitor {

	private Decl_fun decl_fun = null;
	private Typ typ = null;
	private Sblock block = null;
	private Stmt stmt = null;
	private Expr expr = null;
	private Sif sif = null;
	private Sskip skip = null;
	private Ebinop binop = null;
	private Eunop unop = null;
	private Esizeof sizeof = null;
	private Ecall ecall = null;
    private Swhile swhile = null;
    private Eaccess_field access_field = null;
    private Eaccess_local access_local = null;
    private Eassign_local assign_local = null;
    private Eassign_field assign_field = null;
    private Tstructp tstructp = null;
    private Structure structure = null;

	// le résultat du typage sera mis dans cette variable
	private File file;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	
	// il faut compléter le visiteur ci-dessous pour réaliser le typage
	
	@Override
	public void visit(Pfile n) {
		LinkedList<Decl_fun> decl_fun = new LinkedList<>();

	  for (Pdecl pdecl : n.l) {
	  	Decl_fun old_decl_fun = this.decl_fun;
			pdecl.accept(this);
			decl_fun.add(this.decl_fun);
			this.decl_fun = old_decl_fun;
		}

	  file = new File(decl_fun);
	}

	@Override
	public void visit(PTint n) {
	  this.typ = new Tint();
	}

	@Override
	public void visit(PTstruct n) {
		this.tstructp = new Tstructp(new Structure(n.id));
	}

	@Override
	public void visit(Pint n) {
	  this.expr = new Econst(n.n);
	}

	@Override
	public void visit(Pident n) {
        this.access_local = new Eaccess_local(n.id);
	}

	@Override
	public void visit(Punop n) {
        Expr old_expr = this.expr;
        n.e1.accept(this);
        Expr e = this.expr;
        this.expr = old_expr;

        this.unop = new Eunop(n.op, e);
	}

	@Override
	public void visit(Passign n) {
		Expr old_expr = this.expr;
		n.e2.accept(this);
		Expr e2 = this.expr;
		this.expr = old_expr;

        if(n.e1 instanceof Pident) {
            this.assign_local = new Eassign_local(n.e1.id, e2);
        } else {
            n.e1.e.accept(this);
            Expr e1 = this.expr;
            this.expr = old_expr;
            Field f = new Field(n.e1.f, this.typ);
            this.assign_field = new Eassign_field(e1, f, e2);
        }
    }

	@Override
	public void visit(Pbinop n) {
        Expr old_expr = this.expr;
        n.e1.accept(this);
        Expr e1 = this.expr;
        this.expr = old_expr;

        n.e2.accept(this);
        Expr e2 = this.expr;
        this.expr = old_expr;

        this.binop = new Ebinop(n.op, e1, e2);
	}

	@Override
	public void visit(Parrow n) {
        Expr old_expr = this.expr;
        n.e.accept(this);
        Expr e = this.expr;
        this.expr = old_expr;

        this.access_field = new Eaccess_field(e, new Field(n.f, this.typ));
	}

    private LinkedList<Expr> collect_expr(LinkedList<Pexpr> pexpr_list) {
        LinkedList<Expr> expr_list = new LinkedList<>();

        for(Pexpr pexpr : pexpr_list) {
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
        this.ecall = new Ecall(n.f, expr_list);
	}

	@Override
	public void visit(Psizeof n) {
        this.sizeof = new Esizeof(new Structure(n.id));
	}

	@Override
	public void visit(Pskip n) {
        this.skip = new Sskip();
	}

	@Override
	public void visit(Peval n) {
	  n.e.accept(this);
	}

	@Override
	public void visit(Pif n) {
        Expr old_expr = this.expr;
        n.e.accept(this);
        Expr expr = this.expr;
        this.expr = old_expr;

        Expr old_stmt = this.stmt;
        n.s1.accept(this);
        Stmt stmt1 = this.stmt;
        this.stmt = old_stmt;

        n.s2.accept(this);
        Stmt stmt2 = this.stmt;
        this.stmt = old_stmt;

        this.sif = new Sif(expr, stmt1, stmt2);
	}

	@Override
	public void visit(Pwhile n) {
        Expr old_expr = this.expr;
        n.e.accept(this);
        Expr e = this.expr;
        this.expr = old_expr;

        Stmt old_stmt = this.stmt;
        n.s1.accept(this);
        Stmt s = this.stmt;
        this.stmt = old_stmt;

        this.swhile = new Swhile(e, s);
	}

	private LinkedList<Stmt> collect_stmt(LinkedList<Pstmt> pstmt_list) {
		LinkedList<Stmt> stmt = new LinkedList<>();

		for (Pstmt pstmt : pstmt_list) {
			Stmt old_stmt = this.stmt;
			pstmt.accept(this);
			stmt.add(this.stmt);
			this.stmt = old_stmt;
		}

		return stmt;
	}

	@Override
	public void visit(Pbloc n) {
	  LinkedList<Decl_var> decl_var = collect_decl_var(n.vl);
		LinkedList<Stmt> stmt = collect_stmt(n.sl);

	  this.block = new Sblock(decl_var, stmt);
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
        LinkedList<Decl_var> decl_var_list = collect_decl_var(n.fl);
        this.structure = new Structure(n.s);
        for(Decl_var decl_var : decl_var_list) {
            this.structure.fields.put(decl_var.name, new Field(decl_var.name, decl_var.t));
        }
	}

	private LinkedList<Decl_var> collect_decl_var(LinkedList<Pdeclvar> pdeclvar_list) {
		LinkedList<Decl_var> decl_var = new LinkedList<>();

		for (Pdeclvar pdeclvar : pdeclvar_list) {
			Typ old_typ = this.typ;
			pdeclvar.typ.accept(this);
			decl_var.add(new Decl_var(this.typ, pdeclvar.id));
			this.typ = old_typ;
		}

		return decl_var;
	}

	@Override
	public void visit(Pfun n) {
        Typ old_typ = this.typ;
	  n.ty.accept(this);
	  Typ typ = this.typ;
	  this.typ = old_typ;

	  LinkedList<Decl_var> decl_var = collect_decl_var(n.pl);

		Sblock old_block = this.block;
		n.b.accept(this);
		Sblock block = this.block;
		this.block = old_block;

	  this.decl_fun = new Decl_fun(typ, n.s, decl_var, block);
	}

}
