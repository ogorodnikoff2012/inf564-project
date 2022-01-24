package mini_c;

import java.util.LinkedList;

public class Typing implements Pvisitor {

	private Decl_fun decl_fun = null;
	private Typ typ = null;
	private Sblock block = null;
	private Stmt stmt = null;
	private Expr expr = null;

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
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");
	}

	@Override
	public void visit(Pint n) {
	  this.expr = new Econst(n.n);
	}

	@Override
	public void visit(Pident n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Punop n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Passign n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");
	}

	@Override
	public void visit(Pbinop n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Parrow n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Pcall n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Psizeof n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Pskip n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Peval n) {
	  n.e.accept(this);
	}

	@Override
	public void visit(Pif n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

	}

	@Override
	public void visit(Pwhile n) {
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

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
		// TODO Auto-generated method stub
		throw new Error(n.getClass().getName() + ": Not implemented");

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
