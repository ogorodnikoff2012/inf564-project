package mini_c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import mini_c.Interference.ArcSet;

public class ToLTL implements ERTLVisitor {

  private LTLfile ltlFile = null;
  private LTLfun ltlFun = null;
  private Coloring coloring = null;
  private LTL ltl = null;
  private final boolean debug;

  public ToLTL(boolean debug) {
    this.debug = debug;
  }

  public LTLfile translate(ERTLfile ertlFile) {
    ltlFile = new LTLfile();
    ertlFile.accept(this);
    LTLfile result = ltlFile;
    ltlFile = null;
    return result;
  }

  @Override
  public void visit(ERconst erconst) {
    this.ltl = new Lconst(erconst.i,coloring.colors.get(erconst.r), erconst.l);
  }

  @Override
  public void visit(ERload erload) {
    Operand arg1 = coloring.colors.get(erload.r1);
    Operand arg2 = coloring.colors.get(erload.r2);

    if (arg1 instanceof Reg) {
      Reg op1 = (Reg) arg1;
      if (arg2 instanceof Reg) {
        Reg op2 = (Reg) arg2;
        this.ltl = new Lload(op1.r, erload.i, op2.r, erload.l);
      } else {
        Spilled op2 = (Spilled) arg2;
        Reg tmp = new Reg(Register.tmp1);

        Label l1 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, tmp, op2, erload.l));
        this.ltl = new Lload(op1.r, erload.i, tmp.r, l1);
      }
    } else {
      Spilled op1 = (Spilled) arg1;
      if (arg2 instanceof Reg) {
        Reg op2 = (Reg) arg2;
        Reg tmp = new Reg(Register.tmp1);

        Label l1 = this.ltlFun.body.add(new Lload(tmp.r, erload.i, op2.r, erload.l));
        this.ltl = new Lmbinop(Mbinop.Mmov, op1, tmp, l1);
      } else {
        Spilled op2 = (Spilled) arg2;
        Reg tmp1 = new Reg(Register.tmp1);
        Reg tmp2 = new Reg(Register.tmp2);

        Label l2 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, tmp2, op2, erload.l));
        Label l1 = this.ltlFun.body.add(new Lload(tmp1.r, erload.i, tmp2.r, l2));
        this.ltl = new Lmbinop(Mbinop.Mmov, op1, tmp1, l1);
      }
    }
  }

  @Override
  public void visit(ERstore erstore) {
    Operand arg1 = coloring.colors.get(erstore.r1);
    Operand arg2 = coloring.colors.get(erstore.r2);

    if (arg1 instanceof Reg) {
      Reg op1 = (Reg) arg1;
      if (arg2 instanceof Reg) {
        Reg op2 = (Reg) arg2;

        this.ltl = new Lstore(op1.r, op2.r, erstore.i, erstore.l);
      } else {
        Spilled op2 = (Spilled) arg2;
        Reg tmp = new Reg(Register.tmp1);

        Label l1 = this.ltlFun.body.add(new Lstore(op1.r, tmp.r, erstore.i, erstore.l));
        this.ltl = new Lmbinop(Mbinop.Mmov, op2, tmp, l1);
      }
    } else {
      Spilled op1 = (Spilled) arg1;
      if (arg2 instanceof Reg) {
        Reg op2 = (Reg) arg2;
        Reg tmp = new Reg(Register.tmp1);

        Label l1 = this.ltlFun.body.add(new Lstore(tmp.r, op2.r, erstore.i, erstore.l));
        this.ltl = new Lmbinop(Mbinop.Mmov, op1, tmp, l1);
      } else {
        Spilled op2 = (Spilled) arg2;
        Reg tmp1 = new Reg(Register.tmp1);
        Reg tmp2 = new Reg(Register.tmp2);

        Label l2 = this.ltlFun.body.add(new Lstore(tmp1.r, tmp2.r, erstore.i, erstore.l));
        Label l1 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, op2, tmp2, l2));
        this.ltl = new Lmbinop(Mbinop.Mmov, op1, tmp1, l1);
      }
    }
  }

  @Override
  public void visit(ERmunop ermunop) {
    this.ltl = new Lmunop(ermunop.m, coloring.colors.get(ermunop.r), ermunop.l);
  }

  @Override
  public void visit(ERmbinop ermbinop) {
    Operand arg1 = coloring.colors.get(ermbinop.r1);
    Operand arg2 = coloring.colors.get(ermbinop.r2);

    if (ermbinop.m == Mbinop.Mmov && arg1.equals(arg2)) {
      this.ltl = new Lgoto(ermbinop.l);
      return;
    }

    if (ermbinop.m == Mbinop.Mmul) {
      if (arg2 instanceof Spilled) {
        Spilled s2 = (Spilled) arg2;
        Reg tmp = new Reg(Register.tmp1);

        Label l2 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, tmp, s2, ermbinop.l));
        Label l1 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmul, arg1, tmp, l2));
        this.ltl = new Lmbinop(Mbinop.Mmov, s2, tmp, l1);
      } else {
        this.ltl = new Lmbinop(Mbinop.Mmul, arg1, arg2, ermbinop.l);
      }

      return;
    }

    if (arg1 instanceof Spilled && arg2 instanceof Spilled) {
      Reg tmp = new Reg(Register.tmp1);
      Spilled s2 = (Spilled) arg2;

      Label l1 = this.ltlFun.body.add(new Lmbinop(ermbinop.m, tmp, s2, ermbinop.l));
      this.ltl = new Lmbinop(Mbinop.Mmov, arg1, tmp, l1);
    } else {
      this.ltl = new Lmbinop(ermbinop.m, arg1, arg2, ermbinop.l);
    }
  }

  @Override
  public void visit(ERmubranch ermubranch) {
    this.ltl = new Lmubranch(ermubranch.m, coloring.colors.get(ermubranch.r), ermubranch.l1,
        ermubranch.l2);
  }

  @Override
  public void visit(ERmbbranch ermbbranch) {
    Operand arg1 = coloring.colors.get(ermbbranch.r1);
    Operand arg2 = coloring.colors.get(ermbbranch.r2);

    if (arg1 instanceof Spilled && arg2 instanceof Spilled) {
      Spilled s1 = (Spilled) arg1;
      Reg tmp = new Reg(Register.tmp1);

      Label l1 = this.ltlFun.body.add(new Lmbbranch(ermbbranch.m, tmp, arg2, ermbbranch.l1,
          ermbbranch.l2));
      this.ltl = new Lmbinop(Mbinop.Mmov, s1, tmp, l1);
    } else {
      this.ltl = new Lmbbranch(ermbbranch.m, arg1, arg2, ermbbranch.l1, ermbbranch.l2);
    }
  }

  @Override
  public void visit(ERgoto ergoto) {
    this.ltl = new Lgoto(ergoto.l);
  }

  @Override
  public void visit(ERcall ercall) {
    this.ltl = new Lcall(ercall.s, ercall.l);
  }

  @Override
  public void visit(ERalloc_frame eralloc_frame) {
    Reg rsp = new Reg(Register.rsp);
    Reg rbp = new Reg(Register.rbp);
    Label exitPoint = eralloc_frame.l;

    if (coloring.locals > 0) {
      exitPoint = this.ltlFun.body.add(new Lmunop(new Maddi(-8 * coloring.locals), rsp, exitPoint));
    }

    exitPoint = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, rsp, rbp, exitPoint));
    this.ltl = new Lpush(rbp, exitPoint);
  }

  @Override
  public void visit(ERdelete_frame erdelete_frame) {
    Reg rsp = new Reg(Register.rsp);
    Reg rbp = new Reg(Register.rbp);
    Label exitPoint = erdelete_frame.l;

    exitPoint = this.ltlFun.body.add(new Lpop(rbp.r, exitPoint));
    this.ltl = new Lmbinop(Mbinop.Mmov, rbp, rsp, exitPoint);
  }

  @Override
  public void visit(ERget_param erget_param) {
    Spilled from = new Spilled(erget_param.i);
    Operand to = coloring.colors.get(erget_param.r);

    if (to instanceof Spilled) {
      Spilled s = (Spilled) to;
      Reg tmp = new Reg(Register.tmp1);

      Label l1 = this.ltlFun.body.add(new Lmbinop(Mbinop.Mmov, tmp, s, erget_param.l));
      this.ltl = new Lmbinop(Mbinop.Mmov, from, tmp, l1);
    } else {
      this.ltl = new Lmbinop(Mbinop.Mmov, from, to, erget_param.l);
    }
  }

  @Override
  public void visit(ERpush_param erpush_param) {
    Operand op = coloring.colors.get(erpush_param.r);

    if (op instanceof Spilled) {
      Spilled s = (Spilled) op;
      Reg tmp = new Reg(Register.tmp1);

      Label l1 = this.ltlFun.body.add(new Lpush(tmp, erpush_param.l));
      this.ltl = new Lmbinop(Mbinop.Mmov, s, tmp, l1);
    } else {
      this.ltl = new Lpush(op, erpush_param.l);
    }
  }

  @Override
  public void visit(ERreturn erreturn) {
    this.ltl = new Lreturn();
  }

  @Override
  public void visit(ERTLfun ertlfun) {
    assert ltlFun != null;

    ltlFun.body = new LTLgraph();
    ltlFun.entry = ertlfun.entry;

    // Step 1. Liveness analysis
    Liveness liveness = new Liveness(ertlfun.body);

    // Step 2. Interference graph
    Interference interference = new Interference(liveness);

    // Step 3. Coloring
    Coloring coloring = new Coloring(interference);

    if (debug) {
      System.out.println(ertlfun.name);
      print(interference);
      System.out.println(coloring.colors);
    }

    this.coloring = coloring;

    for (Map.Entry<Label, ERTL> entry : ertlfun.body.graph.entrySet()) {
      entry.getValue().accept(this);
      assert this.ltl != null;
      ltlFun.body.put(entry.getKey(), this.ltl);
      this.ltl = null;
    }

    this.coloring = null;
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
  Set<Register> vertices = new HashSet<>();

  Interference(Liveness lg) {
    buildArcs(lg);
  }

  private void addArcDirected(Register from, Register to, ArcType type) {
    if (from.equals(to)) {
      return;
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

  private void addVertex(Register vertex) {
    vertices.add(vertex);
    if (!graph.containsKey(vertex)) {
      graph.put(vertex, new ArcSet());
    }
  }

  private void addArc(Register from, Register to, ArcType type) {
    addVertex(from);
    addVertex(to);
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
        addVertex(def);
        for (Register out : info.outs) {
          if (!out.equals(preferenceRegister)) {
            addArc(def, out, ArcType.INTERFERENCE);
          }
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
  int locals = 0;

  Coloring(Interference ig) {
    ArrayList<Register> todo = new ArrayList<>();
    HashMap<Register, HashSet<Operand>> possibleColorsMap = new HashMap<>();

    // Colors of physical registers are fixed
    for (Register reg : ig.vertices) {
      if (reg.isHW()) {
        Operand color = new Reg(reg);
        colors.put(reg, color);
        HashSet<Operand> possibleColors = new HashSet<>();
        possibleColors.add(color);
        possibleColorsMap.put(reg, possibleColors);
      } else {
        ArcSet arcSet = ig.graph.get(reg);
        HashSet<Operand> possibleColors = new HashSet<>();
        for (Register col : Register.allocatable) {
          if (!arcSet.intfs.contains(col)) {
            possibleColors.add(new Reg(col));
          }
        }

        possibleColorsMap.put(reg, possibleColors);
        todo.add(reg);
      }
    }

    HashSet<Register> spilledRegisters = new HashSet<>();

    RankExtractor rankExtractor = new RankExtractor(possibleColorsMap, ig);
    while (!todo.isEmpty()) {
      Register reg = rankExtractor.extractFrom(todo);

      HashSet<Operand> possibleColors = possibleColorsMap.get(reg);

      if (possibleColors.isEmpty()) {
        spilledRegisters.add(reg);
        continue;
      }

      Operand color = chooseColor(possibleColors, ig.graph.get(reg));
      colors.put(reg, color);

      for (Register intf : ig.graph.get(reg).intfs) {
        possibleColorsMap.get(intf).remove(color);
      }
    }

    colorSpilledRegisters(ig, spilledRegisters);
  }

  private void colorSpilledRegisters(Interference ig, HashSet<Register> spilledRegisters) {
    HashMap<Register, HashSet<Register>> subgraph = new HashMap<>();

    for (Register reg : spilledRegisters) {
      HashSet<Register> arcs = new HashSet<>();
      for (Register arc : ig.graph.get(reg).intfs) {
        if (spilledRegisters.contains(arc)) {
          arcs.add(arc);
        }
      }
      subgraph.put(reg, arcs);
    }

    LinkedList<Register> stack = new LinkedList<>();
    while (!spilledRegisters.isEmpty()) {
      Register bestRegister = null;
      int bestDegree = Integer.MAX_VALUE;

      for (Register reg : spilledRegisters) {
        int degree = subgraph.get(reg).size();
        if (bestRegister == null || degree < bestDegree) {
          bestRegister = reg;
          bestDegree = degree;
        }
      }

      spilledRegisters.remove(bestRegister);

      for (Register arc : subgraph.get(bestRegister)) {
        subgraph.get(arc).remove(bestRegister);
      }

      stack.push(bestRegister);
    }

    while (!stack.isEmpty()) {
      Register reg = stack.pop();
      HashSet<Operand> blockedColors = new HashSet<>();
      for (Register arc : subgraph.get(reg)) {
        blockedColors.add(colors.get(arc));
      }

      int offset = -8;
      Operand color = new Spilled(offset);
      while (blockedColors.contains(color)) {
        offset -= 8;
        color = new Spilled(offset);
      }

      colors.put(reg, color);
      locals = Math.max(locals, offset / -8);
    }
  }

  private Operand chooseColor(HashSet<Operand> possibleColors, ArcSet arcSet) {
    for (Register arc : arcSet.prefs) {
      Operand color = colors.get(arc);
      if (color != null && possibleColors.contains(color)) {
        return color;
      }
    }

    return possibleColors.iterator().next();
  }

  class RankExtractor {

    private final HashMap<Register, HashSet<Operand>> possibleColorsMap;
    private final Interference ig;

    RankExtractor(HashMap<Register, HashSet<Operand>> possibleColorsMap, Interference ig) {
      this.possibleColorsMap = possibleColorsMap;
      this.ig = ig;
    }

    Register extractFrom(ArrayList<Register> registers) {
      assert !registers.isEmpty();

      int bestIdx = 0;
      int bestRank = rank(registers.get(0));

      for (int i = 1; i < registers.size(); ++i) {
        int r = rank(registers.get(i));

        if (r < bestRank) {
          bestIdx = i;
          bestRank = r;
        }
      }

      if (bestIdx != registers.size() - 1) {
        Register t = registers.get(bestIdx);
        registers.set(bestIdx, registers.get(registers.size() - 1));
        registers.set(registers.size() - 1, t);
      }

      return registers.remove(registers.size() - 1);
    }

    private int rank(Register reg) {
      HashSet<Operand> possibleColors = possibleColorsMap.get(reg);
      ArcSet arcSet = ig.graph.get(reg);

      if (possibleColors.size() == 1) {
        for (Register arc : arcSet.prefs) {
          if (possibleColors.contains(colors.get(arc))) {
            return 0;
          }
        }

        return 1;
      } else if (possibleColors.size() > 1) {
        for (Register arc : arcSet.prefs) {
          if (colors.containsKey(arc)) {
            return 2;
          }
        }

        return 3;
      } else {
        return 4;
      }
    }
  }
}