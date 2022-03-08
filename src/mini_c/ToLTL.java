package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mini_c.Interference.ArcSet;

public class ToLTL implements ERTLVisitor {

  private LTLfile ltlFile = null;
  private LTLfun ltlFun = null;

  public LTLfile translate(ERTLfile ertlFile) {
    ltlFile = new LTLfile();
    ertlFile.accept(this);
    LTLfile result = ltlFile;
    ltlFile = null;
    return result;
  }

  @Override
  public void visit(ERconst erconst) {
    throw new Error("Not implemented yet: ERconst");
  }

  @Override
  public void visit(ERload erload) {
    throw new Error("Not implemented yet: ERload");
  }

  @Override
  public void visit(ERstore erstore) {
    throw new Error("Not implemented yet: ERstore");
  }

  @Override
  public void visit(ERmunop ermunop) {
    throw new Error("Not implemented yet: ERmunop");
  }

  @Override
  public void visit(ERmbinop ermbinop) {
    throw new Error("Not implemented yet: ERmbinop");
  }

  @Override
  public void visit(ERmubranch ermubranch) {
    throw new Error("Not implemented yet: ERmubranch");
  }

  @Override
  public void visit(ERmbbranch ermbbranch) {
    throw new Error("Not implemented yet: ERmbbranch");
  }

  @Override
  public void visit(ERgoto ergoto) {
    throw new Error("Not implemented yet: ERgoto");
  }

  @Override
  public void visit(ERcall ercall) {
    throw new Error("Not implemented yet: ERcall");
  }

  @Override
  public void visit(ERalloc_frame eralloc_frame) {
    throw new Error("Not implemented yet: ERalloc_frame");
  }

  @Override
  public void visit(ERdelete_frame erdelete_frame) {
    throw new Error("Not implemented yet: ERdelete_frame");
  }

  @Override
  public void visit(ERget_param erget_param) {
    throw new Error("Not implemented yet: ERget_param");
  }

  @Override
  public void visit(ERpush_param erpush_param) {
    throw new Error("Not implemented yet: ERpush_param");
  }

  @Override
  public void visit(ERreturn erreturn) {
    throw new Error("Not implemented yet: ERreturn");
  }

  @Override
  public void visit(ERTLfun ertlfun) {
    assert ltlFun != null;

    // Step 1. Liveness analysis
    Liveness liveness = new Liveness(ertlfun.body);

    // Step 2. Interference graph
    Interference interference = new Interference(liveness);
    print(interference);
  }

  void print(Interference interference) {
    System.out.println("interference:");
    for (Register r : interference.graph.keySet()) {
      ArcSet a = interference.graph.get(r);
      System.out.println("  " + r + " pref=" + a.prefs + " intf=" + a.intfs);
    }
  }

  @Override
  public void visit(ERTLfile ertlfile) {
    assert ltlFile != null;

    for (ERTLfun fun : ertlfile.funs) {
      this.ltlFun = new LTLfun(fun.name);
      fun.accept(this);
      this.ltlFile.funs.add(this.ltlFun);
      this.ltlFun = null;
    }
  }

}

class Interference {

  class ArcSet {

    Set<Register> prefs = new HashSet<>();
    Set<Register> intfs = new HashSet<>();
  }

  enum ArcType {
    PREFERENCE, INTERFERENCE
  }

  Map<Register, ArcSet> graph = new HashMap<>();

  Interference(Liveness lg) {
    buildArcs(lg);
  }

  private void addArcDirected(Register from, Register to, ArcType type) {
    if (from.equals(to)) {
      return;
    }

    if (!graph.containsKey(from)) {
      graph.put(from, new ArcSet());
    }

    ArcSet arcSet = graph.get(from);
    switch (type) {
      case PREFERENCE:
        if (!arcSet.intfs.contains(to)) {
          arcSet.prefs.add(to);
        }
        break;
      case INTERFERENCE:
        arcSet.intfs.add(to);
        arcSet.prefs.remove(to);
        break;
    }
  }

  private void addArc(Register from, Register to, ArcType type) {
    addArcDirected(from, to, type);
    addArcDirected(to, from, type);
  }

  private void buildArcs(Liveness lg) {
    for (LiveInfo info : lg.info.values()) {
      Register preferenceRegister = null;

      if (info.instr instanceof ERmbinop) {
        ERmbinop op = (ERmbinop) info.instr;
        if (op.m == Mbinop.Mmov) {
          preferenceRegister = op.r1;
        }
      }

      for (Register def : info.defs) {
        for (Register out : info.outs) {
          addArc(def, out, ArcType.INTERFERENCE);
        }
        if (preferenceRegister != null) {
          addArc(def, preferenceRegister, ArcType.PREFERENCE);
        }
      }
    }
  }

}

class Coloring {
  Map<Register, Operand> colors = new HashMap<>();
}