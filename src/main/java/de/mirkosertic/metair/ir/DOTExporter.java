package de.mirkosertic.metair.ir;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DOTExporter {

    public static void writeTo(final Method method, final PrintStream ps) {
        ps.println("digraph {");

        final Deque<Node> workingQueue = new ArrayDeque<>();
        workingQueue.push(method);

        final List<Node> nodeindex = new ArrayList<>();
        nodeindex.add(method);

        for (final Node node : new DFS(method).getTopologicalOrder()) {
            if (!nodeindex.contains(node)) {
                nodeindex.add(node);
            }
        }

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

                    if (useEdge.use instanceof final PHIUse phiUse) {
                        ps.print("[");
                        ps.print("headlabel=\"if #" + nodeindex.indexOf(phiUse.origin) + "\", labeldistance=2, color=blue, constraint=false");
                        ps.print("]");
                    } else if (useEdge.use instanceof DataFlowUse) {
                        ps.print("[");
                        ps.print("headlabel=\"*" + item.uses.indexOf(useEdge) + "\", labeldistance=2");
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
                   d4 [style = invis];
                   d5 [style = invis];
                  }
                  c0 -> c1 [label="Control flow", style=solid, color=red]
                  c1 -> c2 [label="Control flow back edge", style=dashed, color=red]
                  d0 -> d1 [label="Data flow"]
                  d2 -> d3 [label="Declaration", style=dotted]
                  d4 -> d5 [label="PHI Data flow", color=blue]
                 }
                """);

        ps.println("}");
    }

    private static void printNode(final int index, final Node node, final PrintStream ps) {
        printNode(index, node, ps, "");
    }


    private static void printNode(final int index, final Node node, final PrintStream ps, final String labelSuffex) {
        ps.print("label=\"#" + index + " " + node.debugDescription() + labelSuffex + "\"");
        if (node instanceof Method || node instanceof LabelNode || node instanceof Return || node instanceof ReturnValue || node instanceof Goto || node instanceof If || node instanceof Copy || node instanceof Invocation || node instanceof ClassInitialization || node instanceof MonitorEnter || node instanceof MonitorExit || node instanceof Throw || node instanceof ArrayStore || node instanceof ArrayLoad || node instanceof CheckCast) {
            ps.print(",shape=box,fillcolor=lightgrey,style=filled");
        } else if (node.isConstant()) {
            ps.print(",shape=octagon,fillcolor=lightgreen,style=filled");
        } else if (node instanceof Value) {
            ps.print(",shape=octagon,fillcolor=orange,style=filled");
        }
    }

    public static void writeTo(final DominatorTree tree, final OutputStream fileOutputStream) {
        final PrintStream ps = new PrintStream(fileOutputStream);

        ps.println("digraph debugoutput {");
        for (final Node n : tree.preOrder) {
            ps.print(" node" + tree.preOrder.indexOf(n) + "[");
            printNode(tree.preOrder.indexOf(n), n, ps, " Order : " + tree.rpo.indexOf(n));
            ps.println("];");

            final Node id = tree.idom.get(n);
            if (id != n) {
                ps.print(" node" + tree.preOrder.indexOf(n) + " -> node" + tree.preOrder.indexOf(id) + "[dir=\"forward\"");
                ps.print(" color=\"green\" penwidth=\"2\"");
                ps.println("];");
            }

            for (final Node.UseEdge useEdge : n.uses) {
                final Node usedNode = useEdge.node;

                ps.print(" node" + tree.preOrder.indexOf(usedNode));
                ps.print(" -> node" + tree.preOrder.indexOf(n));

                if (useEdge.use instanceof final PHIUse phiUse) {
                    ps.print("[");
                    ps.print("headlabel=\"if #" + tree.preOrder.indexOf(phiUse.origin) + "\", labeldistance=2, color=blue, constraint=false");
                    ps.print("]");
                } else if (useEdge.use instanceof DataFlowUse) {
                    ps.print("[");
                    ps.print("headlabel=\"*" + n.uses.indexOf(useEdge) + "\", labeldistance=2");
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
                   d4 [style = invis];
                   d5 [style = invis];
                   d6 [style = invis];
                   d7 [style = invis];
                  }
                  c0 -> c1 [label="Control flow", style=solid, color=red]
                  c1 -> c2 [label="Control flow back edge", style=dashed, color=red]
                  d0 -> d1 [label="Data flow"]
                  d2 -> d3 [label="Declaration", style=dotted]
                  d4 -> d5 [label="PHI Data flow", color=blue]
                  d6 -> d7 [label="Dominance", color=green]
                 }
                """);
        ps.println("}");
        ps.flush();
    }
}
