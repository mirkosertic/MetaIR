package de.mirkosertic.metair.ir;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

public final class DOTExporter {

    public static void writeTo(final Method method, final PrintStream ps) {
        ps.println("digraph {");

        final Deque<Node> workingQueue = new ArrayDeque<>();
        workingQueue.push(method);

        final List<Node> nodeindex = new ArrayList<>();
        nodeindex.add(method);

        final Set<Node> visited = new HashSet<>();

        while (!workingQueue.isEmpty()) {
            final Node item = workingQueue.pop();
            if (visited.add(item)) {
                final int index = nodeindex.indexOf(item);

                ps.print(" node" + index);
                ps.print("[");

                printNode(nodeindex.indexOf(item), item, ps);

                ps.println("];");

                for (final Node usedBy : item.usedBy) {
                    if (!visited.contains(usedBy)) {
                        workingQueue.push(usedBy);
                    }

                    if (!nodeindex.contains(usedBy)) {
                        nodeindex.add(usedBy);
                    }
                }

                for (final Node.UseEdge useEdge : item.uses) {
                    final Node usedNode = useEdge.node;
                    if (!visited.contains(usedNode)) {
                        workingQueue.push(usedNode);
                    }

                    if (!nodeindex.contains(usedNode)) {
                        nodeindex.add(usedNode);
                    }

                    ps.print(" node" + nodeindex.indexOf(usedNode));
                    ps.print(" -> node" + index);

                    if (useEdge.use instanceof DataFlowUse) {
                        ps.print("[");
                        ps.print("headlabel=\"*" + item.uses.indexOf(useEdge) + "\",labeldistance=2");
                        ps.print("]");
                    } else if (useEdge.use instanceof DefinedByUse) {
                        ps.print("[");
                        ps.print("style=dotted");
                        ps.print("]");
                    } else if (useEdge.use instanceof final ControlFlowUse cfu) {
                        ps.print("[");
                        if (useEdge.node instanceof ConditionalNode) {
                            ps.print("headlabel=\"" + cfu.condition.debugDescription() + "\",labeldistance=2,color=red, fontcolor=red");
                        } else {
                            ps.print("labeldistance=2,color=red, fontcolor=red");
                        }
                        if (cfu.type == ControlType.BACKWARD) {
                            ps.print(", style=dashed");
                        }
                        ps.print("]");
                    }

                    ps.println(";");
                }
            }
        }

        final Map<Node, List<Node>> defines = new HashMap<>();

        for (final Node node : nodeindex) {
            final List<Node.UseEdge> edges = node.uses.stream().filter(t -> t.use instanceof DefinedByUse).toList();
            for (final Node.UseEdge edge : edges) {
                defines.computeIfAbsent(edge.node, k -> new ArrayList<>()).add(node);
            }
        }

        int clusterIndex = 0;
        for (final Map.Entry<Node, List<Node>> entry : defines.entrySet()) {
            ps.println(" subgraph cluster_" + (clusterIndex++) + " {");
            ps.println("  color=lightgray;");
            ps.println("  node" + nodeindex.indexOf(entry.getKey()) + ";");
            for (final Node value : entry.getValue()) {
                ps.println("  node" + nodeindex.indexOf(value) + ";");
            }
            ps.println(" }");
        }

        ps.println("""
                 subgraph cluster_000 {
                  label = "Legend";
                  node [shape=point]
                  {
                   rank=same;
                   c0 [style = invis];
                   c1 [style = invis];
                   c2 [style = invis];
                   c3 [style = invis];
                   d0 [style = invis];
                   d1 [style = invis];
                   d2 [style = invis];
                   d3 [style = invis];
                  }
                  c0 -> c1 [label="Control flow", style=solid, color=red]
                  c1 -> c2 [label="Control flow back edge", style=dashed, color=red]
                  d0 -> d1 [label="Data flow"]
                  d2 -> d3 [label="Declaration", style=dotted]
                 }
                """);

        ps.println("}");
    }

    private static void printNode(final int index, final Node node, final PrintStream ps) {
        ps.print("label=\"#" + index + " " + node.debugDescription() + "\"");
        if (node instanceof Method || node instanceof LabelNode || node instanceof Return || node instanceof ReturnValue || node instanceof Goto || node instanceof If || node instanceof Copy || node instanceof Invocation || node instanceof ClassInitialization) {
            ps.print(",shape=box,fillcolor=lightgrey,style=filled");
        } else if (node.isConstant()) {
            ps.print(",shape=octagon,fillcolor=lightgreen,style=filled");
        } else if (node instanceof Value) {
            ps.print(",shape=octagon,fillcolor=orange,style=filled");
        }
    }

    public static void writeTo(final DominatorTree tree, final OutputStream fileOutputStream) {
        final PrintWriter pw = new PrintWriter(fileOutputStream);

        pw.println("digraph debugoutput {");
        for (final Node n : tree.preOrder) {
            final String label = "#" + tree.preOrder.indexOf(n) + " " + n.debugDescription() + " Order : " + tree.rpo.indexOf(n);

            pw.print(" node_" + tree.preOrder.indexOf(n) + "[label=\"" + label + "\" ");
            pw.print("shape=\"box\" fillcolor=\"orangered\" style=\"filled\"");
            pw.println("];");

            final Node id = tree.idom.get(n);
            if (id != n) {
                pw.print(" node_" + tree.preOrder.indexOf(n) + " -> node_" + tree.preOrder.indexOf(id) + "[dir=\"forward\"");
                pw.print(" color=\"black\" penwidth=\"2\"");
                pw.println("];");
            }

            for (final Map.Entry<Node, List<Node.UseEdge>> entry : n.outgoingControlFlows().entrySet()) {
                for (final Node.UseEdge use : entry.getValue()) {
                    if (use.use instanceof final ControlFlowUse cfu) {
                        pw.print(" node_" + tree.preOrder.indexOf(n) + " -> node_" + tree.preOrder.indexOf(entry.getKey()) + "[dir=\"forward\"");
                        pw.print(" color=\"red\" penwidth=\"1\"");
                        if (cfu.type == ControlType.BACKWARD) {
                            pw.print(" style=\"dashed\"");
                        }
                        pw.println("];");
                    }
                }
            }
        }
        pw.println("}");
        pw.flush();
    }
}
