package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Liveness {
  public final Map<Label, LiveInfo> info = new HashMap<>();

  Liveness(ERTLgraph graph) {
    initialize(graph);
    calculatePredecessors();
    runKildall();
  }

  private void initialize(ERTLgraph graph) {
    for (Map.Entry<Label, ERTL> entry : graph.graph.entrySet()) {
      info.put(entry.getKey(), new LiveInfo(entry.getValue()));
    }
  }

  private void calculatePredecessors() {
    for (Map.Entry<Label, LiveInfo> entry : this.info.entrySet()) {
      for (Label succ : entry.getValue().succ) {
        this.info.get(succ).pred.add(entry.getKey());
      }
    }
  }

  private void runKildall() {
    HashSet<Label> inQueue = new HashSet<>(info.keySet());
    Queue<Label> vertices = new LinkedList<>(info.keySet());

    while (!vertices.isEmpty()) {
      Label vertex = vertices.poll();
      inQueue.remove(vertex);

      LiveInfo vertexInfo = info.get(vertex);
      Set oldIn = vertexInfo.ins;
      vertexInfo.outs = calculateOuts(vertexInfo);
      vertexInfo.ins = calculateIns(vertexInfo);

      if (!vertexInfo.ins.equals(oldIn)) {
        for (Label pred : vertexInfo.pred) {
          if (!inQueue.contains(pred)) {
            vertices.add(pred);
            inQueue.add(pred);
          }
        }
      }
    }
  }

  private Set<Register> calculateIns(LiveInfo vertexInfo) {
    Set<Register> result = new HashSet<>(vertexInfo.uses);

    for (Register reg : vertexInfo.outs) {
      if (!vertexInfo.defs.contains(reg)) {
        result.add(reg);
      }
    }

    return result;
  }

  private Set<Register> calculateOuts(LiveInfo vertexInfo) {
    Set<Register> result = new HashSet<>();

    for (Label succ : vertexInfo.succ) {
      result.addAll(info.get(succ).ins);
    }

    return result;
  }

  private void print(Set<Label> visited, Label l) {
    if (visited.contains(l)) return;
    visited.add(l);
    LiveInfo li = this.info.get(l);
    System.out.println("  " + String.format("%3s", l) + ": " + li);
    for (Label s: li.succ) print(visited, s);
  }

  public void print(Label entry) {
    print(new HashSet<Label>(), entry);
  }
}

class LiveInfo {
  public final ERTL instr;
  public final Label[] succ;
  public final Set<Label> pred = new HashSet<>();
  public final Set<Register> defs;
  public final Set<Register> uses;
  public Set<Register> ins = new HashSet<>();
  public Set<Register> outs = new HashSet<>();

  LiveInfo(ERTL instr) {
    this.instr = instr;
    this.succ = instr.succ();
    this.defs = instr.def();
    this.uses = instr.use();
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(instr)
        .append(" d=")
        .append(defs)
        .append(" u=")
        .append(uses)
        .append(" i=")
        .append(ins)
        .append(" o=")
        .append(outs)
        .toString();
  }
}