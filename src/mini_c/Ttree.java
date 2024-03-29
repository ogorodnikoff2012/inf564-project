package mini_c;

import java.util.HashMap;
import java.util.LinkedList;

abstract class Typ {
	abstract void accept(Visitor v);

	public abstract int getSize();
}

class Tint extends Typ {
	public static final Tint tint = new Tint();
	private Tint() {}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Tint || obj instanceof Ttypenull;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public int getSize() {
		return 8;
	}

	@Override
	public String toString() {
	  return "int";
	}
}

class Tstructp extends Typ {
	public Structure s;

	Tstructp(Structure s) {
		this.s = s;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Ttypenull) {
			return true;
		}
		if (!(obj instanceof Tstructp)) {
			return false;
		}
		return s.equals(((Tstructp)obj).s);
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public int getSize() {
		return 8;
	}

	@Override
	public String toString() {
	  return "struct " + s.str_name + "*";
	}
}

class Tvoidstar extends Typ {
	public static final Tvoidstar tvoidstar = new Tvoidstar();
	private Tvoidstar() {
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Tvoidstar || obj instanceof Ttypenull;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public int getSize() {
		return 8;
	}

	@Override
	public String toString() {
	  return "void*";
	}
}

class Ttypenull extends Typ {
	public static final Ttypenull ttypenull = new Ttypenull();
	private Ttypenull() {
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Typ;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public int getSize() {
		return 8;
	}

	@Override
	public String toString() {
	  return "typenull";
	}
}

class Structure {
	public final String str_name;
	private final HashMap<String, Field> fields;
	private int size;

	Structure(String str_name) {
		this.str_name = str_name;
		this.fields = new HashMap<String, Field>();
		this.size = 0;
	}

	void addField(Field f) throws TypeError {
		String name = f.field_name.id;
		if (fields.containsKey(name)) {
			throw new TypeError("Redefinition of field " + name + " in structure " + this.str_name, f.field_name.loc);
		}
		f.setOffset(size);
		size += f.getSize();
		fields.put(name, f);
	}

	int getSize() {
		return size;
	}

	Field getField(String name) {
		return fields.get(name);
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

class Field {
	public final Pstring field_name;
	public final Typ field_typ;
	private int offset;

	Field(Pstring field_name, Typ field_typ) {
		this.field_name = field_name;
		this.field_typ = field_typ;
		this.offset = 0;
	}

	void setOffset(int offset) {
		this.offset = offset;
	}

	int getOffset() {
		return offset;
	}

	int getSize() {
		return field_typ.getSize();
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
		return "Field{" +
				"field_name='" + field_name + '\'' +
				", field_typ=" + field_typ +
				'}';
	}
}

class Decl_var {
	public Typ t;
	public Pstring name;


	Decl_var(Typ t, Pstring i) {
		this.t = t;
		this.name = i;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

	@Override
	public String toString() {
	  return t.toString() + " " + name;
	}
}

// expression

abstract class Expr {
	public Typ typ; // chaque expression est décorée par son type

  abstract void accept(Visitor v);
}

class Econst extends Expr {
	public int i;

	Econst(int i) {
		this.i = i;
	}


	void accept(Visitor v) {
		v.visit(this);
	}
}

class Eaccess_local extends Expr {
	public Pstring i;

	Eaccess_local(Pstring i) {
		this.i = i;
	}


	void accept(Visitor v) {
		v.visit(this);
	}

}

class Eaccess_field extends Expr {
	public Expr e;
	public Field f;

	Eaccess_field(Expr e, Field f) {
		this.e = e;
		this.f = f;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

class Eassign_local extends Expr {
	public Pstring i;
	public Expr e;

	Eassign_local(Pstring i, Expr e) {
		this.i = i;
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Eassign_field extends Expr {
	public Expr e1;
	public Field f;
	public Expr e2;

	Eassign_field(Expr e1, Field f, Expr e2) {
		this.e1 = e1;
		this.f = f;
		this.e2 = e2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Eunop extends Expr {
	public Unop u;
	public Expr e;

	Eunop(Unop u, Expr e) {
		this.u = u;
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Ebinop extends Expr {
	public Binop b;
	public Expr e1;
	public Expr e2;

	Ebinop(Binop b, Expr e1, Expr e2) {
		this.b = b;
		this.e1 = e1;
		this.e2 = e2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Ecall extends Expr {
	public String i;
	public LinkedList<Expr> el;

	Ecall(String i, LinkedList<Expr> el) {
		this.i = i;
		this.el = el;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Esizeof extends Expr {
	public Structure s;

	Esizeof(Structure s) {
		this.s = s;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

// instruction

abstract class Stmt {
	abstract void accept(Visitor v);
}

class Sskip extends Stmt {
	Sskip() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Sbreak extends Stmt {
	Sbreak() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

class Scontinue extends Stmt {
	Scontinue() {
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

class Sexpr extends Stmt {
	public Expr e;

	Sexpr(Expr e) {
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Sif extends Stmt {
	public Expr e;
	public Stmt s1;
	public Stmt s2;

	Sif(Expr e, Stmt s1, Stmt s2) {
		this.e = e;
		this.s1 = s1;
		this.s2 = s2;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Swhile extends Stmt {
	public Expr e;
	public Stmt s;

	Swhile(Expr e, Stmt s) {
		this.e = e;
		this.s = s;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Sfor extends Stmt {
	public Expr pre;
	public Expr cond;
	public Expr post;
	public Stmt body;

	Sfor(Expr pre, Expr cond, Expr post, Stmt body) {
		this.pre = pre;
		this.cond = cond;
		this.post = post;
		this.body = body;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

class Sblock extends Stmt {
	public LinkedList<Decl_var> dl;
	public LinkedList<Stmt> sl;

	Sblock(LinkedList<Decl_var> dl, LinkedList<Stmt> sl) {
		this.dl = dl;
		this.sl = sl;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

class Sreturn extends Stmt {
	public Expr e;

	Sreturn(Expr e) {
		this.e = e;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

// fonction

class Decl_fun {
	public Typ fun_typ;
	public String fun_name;
	public LinkedList<Decl_var> fun_formals;
	public Stmt fun_body;

	Decl_fun(Typ fun_typ, String fun_name, LinkedList<Decl_var> fun_formals,
			Stmt fun_body) {
		this.fun_typ = fun_typ;
		this.fun_name = fun_name;
		this.fun_formals = fun_formals;
		this.fun_body = fun_body;
	}

	void accept(Visitor v) {
		v.visit(this);
	}

}

// programme = liste de fonctions

class File {
	public LinkedList<Decl_fun> funs;

	File(LinkedList<Decl_fun> funs) {
		this.funs = funs;
	}

	void accept(Visitor v) {
		v.visit(this);
	}
}

interface Visitor {
	public void visit(Unop n);
	public void visit(Binop n);
	public void visit(String n);
	public void visit(Tint n);
	public void visit(Tstructp n);
	public void visit(Tvoidstar n);
	public void visit(Ttypenull n);
	public void visit(Structure n);
	public void visit(Field n);
	public void visit(Decl_var n);
	public void visit(Expr n);
	public void visit(Econst n);
	public void visit(Eaccess_local n);
	public void visit(Eaccess_field n);
	public void visit(Eassign_local n);
	public void visit(Eassign_field n);
	public void visit(Eunop n);
	public void visit(Ebinop n);
	public void visit(Ecall n);
	public void visit(Esizeof n);
	public void visit(Sskip n);
	public void visit(Sbreak n);
	public void visit(Scontinue n);
	public void visit(Sexpr n);
	public void visit(Sif n);
	public void visit(Swhile n);
	public void visit(Sblock n);
	public void visit(Sreturn n);
	public void visit(Decl_fun n);
	public void visit(File n);
	public void visit(Sfor n);
}
