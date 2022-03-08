package mini_c;

import java.util.HashSet;

public class Lin implements LTLVisitor {
  private LTLgraph cfg = null;
  private X86_64 asm = null;
  private HashSet<Label> visited = null;

  public X86_64 linearize(LTLfile file) {
    asm = new X86_64();
    asm.globl("main");
    file.accept(this);
    X86_64 result = asm;
    asm = null;
    return result;
  }

  private void lin(Label l) {
    if (visited.contains(l)) {
      asm.needLabel(l);
      asm.jmp(l.name);
    } else {
      visited.add(l);
      asm.label(l);
      cfg.graph.get(l).accept(this);
    }
  }

  @Override
  public void visit(Lload lload) {
    asm.movq(lload.i  + "(" + lload.r1 + ")", lload.r2.toString());
    lin(lload.l);
  }

  @Override
  public void visit(Lstore lstore) {
    asm.movq(lstore.r1.toString(), lstore.i + "(" + lstore.r2 + ")");
    lin(lstore.l);
  }

  @Override
  public void visit(Lmubranch lmubranch) {
    int val = 0;
    if (lmubranch.m instanceof Mjlei) {
      val = ((Mjlei) lmubranch.m).n;
    } else if (lmubranch.m instanceof Mjgi) {
      val = ((Mjgi) lmubranch.m).n;
    }
    asm.cmpq(val, lmubranch.r.toString()); // r -= val

    if (!visited.contains(lmubranch.l2)) {
      if (lmubranch.m instanceof Mjz) {
        asm.jz(lmubranch.l1.name);
      } else if (lmubranch.m instanceof Mjnz) {
        asm.jnz(lmubranch.l1.name);
      } else if (lmubranch.m instanceof Mjlei) {
        asm.jle(lmubranch.l1.name);
      } else {
        assert lmubranch.m instanceof Mjgi;
        asm.jg(lmubranch.l1.name);
      }

      lin(lmubranch.l2);
      lin(lmubranch.l1);
    } else if (!visited.contains(lmubranch.l1)) {
      if (lmubranch.m instanceof Mjz) {
        asm.jnz(lmubranch.l2.name);
      } else if (lmubranch.m instanceof Mjnz) {
        asm.jz(lmubranch.l2.name);
      } else if (lmubranch.m instanceof Mjlei) {
        asm.jg(lmubranch.l2.name);
      } else {
        assert lmubranch.m instanceof Mjgi;
        asm.jle(lmubranch.l2.name);
      }

      lin(lmubranch.l1);
      lin(lmubranch.l2);
    } else {
      if (lmubranch.m instanceof Mjz) {
        asm.jz(lmubranch.l1.name);
      } else if (lmubranch.m instanceof Mjnz) {
        asm.jnz(lmubranch.l1.name);
      } else if (lmubranch.m instanceof Mjlei) {
        asm.jle(lmubranch.l1.name);
      } else {
        assert lmubranch.m instanceof Mjgi;
        asm.jg(lmubranch.l1.name);
      }

      asm.jmp(lmubranch.l2.name);
    }
  }

  @Override
  public void visit(Lmbbranch lmbbranch) {
    asm.cmpq(lmbbranch.r1.toString(), lmbbranch.r2.toString());

    if (!visited.contains(lmbbranch.l2)) {
      switch (lmbbranch.m) {
        case Mjl:
          asm.jl(lmbbranch.l1.name);
          break;
        case Mjle:
          asm.jle(lmbbranch.l1.name);
          break;
      }

      lin(lmbbranch.l2);
      lin(lmbbranch.l1);
    } else if (!visited.contains(lmbbranch.l1)) {
      switch (lmbbranch.m) {
        case Mjl:
          asm.jge(lmbbranch.l2.name);
          break;
        case Mjle:
          asm.jg(lmbbranch.l2.name);
          break;
      }

      lin(lmbbranch.l1);
      lin(lmbbranch.l2);
    } else {
      switch (lmbbranch.m) {
        case Mjl:
          asm.jl(lmbbranch.l1.name);
          break;
        case Mjle:
          asm.jle(lmbbranch.l1.name);
          break;
      }

      asm.jmp(lmbbranch.l2.name);
    }
  }

  @Override
  public void visit(Lgoto lgoto) {
    if (visited.contains(lgoto.l)) {
      asm.jmp(lgoto.l.name);
    } else {
      lin(lgoto.l);
    }
  }

  @Override
  public void visit(Lreturn lreturn) {
    asm.ret();
  }

  @Override
  public void visit(Lconst lconst) {
    asm.movq(lconst.i, lconst.o.toString());
    lin(lconst.l);
  }

  @Override
  public void visit(Lmunop lmunop) {
    if (lmunop.m instanceof Maddi) {
      asm.addq(((Maddi) lmunop.m).n, lmunop.o.toString());
    } else if (lmunop.m instanceof  Msetei) {
      asm.xchg(lmunop.o.toString(), Register.rax.name);
      asm.cmpq(((Msetei) lmunop.m).n, Register.rax.name);
      asm.sete("%al");
      asm.movzbq("%al", Register.rax.name);
      asm.xchg(lmunop.o.toString(), Register.rax.name);
    } else {
      asm.xchg(lmunop.o.toString(), Register.rax.name);
      asm.cmpq(((Msetnei) lmunop.m).n, Register.rax.name);
      asm.setne("%al");
      asm.movzbq("%al", Register.rax.name);
      asm.xchg(lmunop.o.toString(), Register.rax.name);
    }
    lin(lmunop.l);
  }

  @Override
  public void visit(Lmbinop lmbinop) {
    String lhs = lmbinop.o1.toString();
    String rhs = lmbinop.o2.toString();

    switch (lmbinop.m) {
      case Mmov:
        asm.movq(lhs, rhs);
        break;
      case Madd:
        asm.addq(lhs, rhs);
        break;
      case Msub:
        asm.subq(lhs, rhs);
        break;
      case Mmul:
        asm.imulq(lhs, rhs);
        break;
      case Mdiv:
        asm.cqto();
        asm.idivq(lhs);
        break;
      case Msete:
        asm.cmpq(lhs, rhs);
        asm.sete(rhs);
        break;
      case Msetne:
        asm.cmpq(lhs, rhs);
        asm.setne(rhs);
        break;
      case Msetl:
        asm.cmpq(lhs, rhs);
        asm.setl(rhs);
        break;
      case Msetle:
        asm.cmpq(lhs, rhs);
        asm.setle(rhs);
        break;
      case Msetg:
        asm.cmpq(lhs, rhs);
        asm.setg(rhs);
        break;
      case Msetge:
        asm.cmpq(lhs, rhs);
        asm.setge(rhs);
        break;
    }
    lin(lmbinop.l);
  }

  @Override
  public void visit(Lpush lpush) {
    asm.pushq(lpush.o.toString());
    lin(lpush.l);
  }

  @Override
  public void visit(Lpop lpop) {
    asm.popq(lpop.r.toString());
    lin(lpop.l);
  }

  @Override
  public void visit(Lcall lcall) {
    asm.call(lcall.s);
    lin(lcall.l);
  }

  @Override
  public void visit(LTLfun ltlfun) {
    throw new Error("Not implemented yet: LTLfun");
  }

  @Override
  public void visit(LTLfile ltlfile) {
    assert asm != null;

    for (LTLfun fun : ltlfile.funs) {
      visited = new HashSet<>();

      cfg = fun.body;
      asm.label(fun.name);
      lin(fun.entry);

      visited = null;
    }

  }

}
