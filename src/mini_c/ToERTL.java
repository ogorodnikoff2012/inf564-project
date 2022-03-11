package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class ToERTL implements RTLVisitor {

  private ERTLfile ertlFile = null;
  private ERTLfun ertlFun = null;
  private ERTL ertl = null;

  public ERTLfile translate(RTLfile rtlFile) {
    ertlFile = new ERTLfile();
    rtlFile.accept(this);
    ERTLfile result = ertlFile;
    ertlFile = null;
    return result;
  }

  @Override
  public void visit(Rconst rconst) {
    assert this.ertl == null;
    this.ertl = new ERconst(rconst.i, rconst.r, rconst.l);
  }

  @Override
  public void visit(Rload rload) {
    assert this.ertl == null;
    this.ertl = new ERload(rload.r1, rload.i, rload.r2, rload.l);
  }

  @Override
  public void visit(Rstore rstore) {
    assert this.ertl == null;
    this.ertl = new ERstore(rstore.r1, rstore.r2, rstore.i, rstore.l);
  }

  @Override
  public void visit(Rmunop rmunop) {
    assert this.ertl == null;
    Label l2 = this.ertlFun.body.add(new ERmbinop(Mbinop.Mmov, Register.rax, rmunop.r, rmunop.l));
    Label l1 = this.ertlFun.body.add(new ERmunop(rmunop.m, Register.rax, l2));
    this.ertl = new ERmbinop(Mbinop.Mmov, rmunop.r, Register.rax, l1);
  }

  private static final HashMap<Mbinop, Register> kBinopsRequireReg = new HashMap<>();
  static {
    kBinopsRequireReg.put(Mbinop.Mdiv, Register.rax);
    kBinopsRequireReg.put(Mbinop.Mmod, Register.rdx);
    kBinopsRequireReg.put(Mbinop.Msete, Register.rax);
    kBinopsRequireReg.put(Mbinop.Msetne, Register.rax);
    kBinopsRequireReg.put(Mbinop.Msetl, Register.rax);
    kBinopsRequireReg.put(Mbinop.Msetle, Register.rax);
    kBinopsRequireReg.put(Mbinop.Msetg, Register.rax);
    kBinopsRequireReg.put(Mbinop.Msetge, Register.rax);
  }

  @Override
  public void visit(Rmbinop rmbinop) {
    assert this.ertl == null;
    if (kBinopsRequireReg.containsKey(rmbinop.m)) {
      Register reg = kBinopsRequireReg.get(rmbinop.m);
      Label movResult = this.ertlFun.body.add(new ERmbinop(Mbinop.Mmov, reg, rmbinop.r2, rmbinop.l));
      Label op = this.ertlFun.body.add(new ERmbinop(rmbinop.m, rmbinop.r1, reg, movResult));
      this.ertl = new ERmbinop(Mbinop.Mmov, rmbinop.r2, reg, op);
    } else {
      this.ertl = new ERmbinop(rmbinop.m, rmbinop.r1, rmbinop.r2, rmbinop.l);
    }
  }

  @Override
  public void visit(Rmubranch rmubranch) {
    assert this.ertl == null;
    this.ertl = new ERmubranch(rmubranch.m, rmubranch.r, rmubranch.l1, rmubranch.l2);
  }

  @Override
  public void visit(Rmbbranch rmbbranch) {
    assert this.ertl == null;
    this.ertl = new ERmbbranch(rmbbranch.m, rmbbranch.r1, rmbbranch.r2, rmbbranch.l1, rmbbranch.l2);
  }

  @Override
  public void visit(Rcall rcall) {
    final int maxArgsInRegs = Register.parameters.size();
    final int argsInRegs = Math.min(rcall.rl.size(), maxArgsInRegs);
    final int argsOnStack = rcall.rl.size() - argsInRegs;

    // Step 4, clear stack
    Label step4 = rcall.l;
    if (argsOnStack > 0) {
      step4 = this.ertlFun.body.add(new ERmunop(new Maddi(argsOnStack * 8), Register.rsp, step4));
    }

    // Step 3, copy result to pseudo-register
    Label step3 = this.ertlFun.body.add(new ERmbinop(Mbinop.Mmov, Register.result, rcall.r, step4));

    // Step 2, perform call
    Label step2 = this.ertlFun.body.add(new ERcall(rcall.s, argsInRegs, step3));

    // Step 1, store arguments
    Iterator<Register> iter = ((LinkedList<Register>)rcall.rl).iterator();
    Label exitPoint = step2;

    for (int i = 0; i < argsInRegs; ++i) {
      exitPoint = this.ertlFun.body.add(new ERmbinop(Mbinop.Mmov, iter.next(), Register.parameters.get(i), exitPoint));
    }

    for (int i = 0; i < argsOnStack; ++i) {
      exitPoint = this.ertlFun.body.add(new ERpush_param(iter.next(), exitPoint));
    }

    this.ertl = new ERgoto(exitPoint);
  }

  @Override
  public void visit(Rgoto rgoto) {
    assert this.ertl == null;
    this.ertl = new ERgoto(rgoto.l);
  }

  @Override
  public void visit(RTLfun rtlfun) {
    assert this.ertlFun != null;
    this.ertlFun.locals.addAll(rtlfun.locals);

    ertlFun.body = new ERTLgraph();

    HashMap<Register, Register> calleeSavedToPseudo = new HashMap<>();
    for (Register phys : Register.callee_saved) {
      Register pseudo = new Register();
      calleeSavedToPseudo.put(phys, pseudo);
      ertlFun.locals.add(pseudo);
    }

    // Epilogue
    // Step 4. Return
    Label epilogueStep4 = ertlFun.body.add(new ERreturn());
    // Step 3. Delete frame
    Label epilogueStep3 = ertlFun.body.add(new ERdelete_frame(epilogueStep4));
    // Step 2. Restore callee-saved registers
    Label epilogueStep2 = epilogueStep3;
    for (Register phys : Register.callee_saved) {
      Register pseudo = calleeSavedToPseudo.get(phys);
      epilogueStep2 = ertlFun.body.add(new ERmbinop(Mbinop.Mmov, pseudo, phys, epilogueStep2));
    }
    // Step 1. Move result from physical register
    ertlFun.body.put(rtlfun.exit, new ERmbinop(Mbinop.Mmov, rtlfun.result, Register.result, epilogueStep2));

    for (Map.Entry<Label, RTL> entry : rtlfun.body.graph.entrySet()) {
      RTL value = entry.getValue();
      if (value != null) {
        value.accept(this);
        ertlFun.body.put(entry.getKey(), this.ertl);
        this.ertl = null;
      }
    }

    // Prologue
    Label prologueEnd = rtlfun.entry;

    // Step 3. Collect parameters to pseudo-registers
    Label prologueStep3 = prologueEnd;
    final int maxArgsInRegs = Register.parameters.size();
    final int argsInRegs = Math.min(rtlfun.formals.size(), maxArgsInRegs);
    final int argsOnStack = rtlfun.formals.size() - argsInRegs;

    Iterator<Register> iter = rtlfun.formals.iterator();
    for (int i = 0; i < argsInRegs; ++i) {
      prologueStep3 = ertlFun.body.add(new ERmbinop(Mbinop.Mmov, Register.parameters.get(i), iter.next(), prologueStep3));
    }
    for (int i = 0; i < argsOnStack; ++i) {
      prologueStep3 = ertlFun.body.add(new ERget_param((i + 2) * 8, iter.next(), prologueStep3));
    }

    // Step 2. Save callee-saved registers
    Label prologueStep2 = prologueStep3;
    for (Register phys : Register.callee_saved) {
      Register pseudo = calleeSavedToPseudo.get(phys);
      prologueStep2 = ertlFun.body.add(new ERmbinop(Mbinop.Mmov, phys, pseudo, prologueStep2));
    }

    // Step 1. Allocate frame
    Label prologueStep1 = ertlFun.body.add(new ERalloc_frame(prologueStep2));

    ertlFun.entry = prologueStep1;
  }

  @Override
  public void visit(RTLfile rtlfile) {
    assert ertlFile != null;
    for (RTLfun fun : rtlfile.funs) {
      this.ertlFun = new ERTLfun(fun.name, fun.formals.size());
      fun.accept(this);
      ertlFile.funs.add(this.ertlFun);
      this.ertlFun = null;
    }
  }
}
