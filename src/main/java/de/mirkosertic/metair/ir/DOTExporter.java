package de.mirkosertic.metair.ir;


import java.io.PrintStream;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.LabelTarget;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

                if (!(item instanceof Projection)) {

                    final List<Projection> projections = item.usedBy.stream().filter(t -> t instanceof Projection).map(t -> (Projection) t).sorted().toList();
                    if (projections.isEmpty()) {
                        ps.print(" node" + index);
                        ps.print("[");

                        printNode(index, item, ps);

                        ps.println("];");
                    } else {

                        final StringBuilder label = new StringBuilder("<<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"5\">");
                        label.append("<tr><td colspan=\"");
                        label.append(projections.size());
                        label.append("\">#");
                        label.append(index);
                        label.append(" ");
                        label.append(item.debugDescription());
                        label.append("</td></tr>");
                        label.append("<tr>");
                        for (final Projection projection : projections) {
                            label.append("<td port=\"prj");
                            label.append(nodeindex.indexOf((Node) projection));
                            label.append("\" bgcolor=\"yellow\">");
                            label.append(projection.debugDescription());
                            label.append("</td>");
                        }
                        label.append("</tr>");
                        label.append("</table>>");

                        ps.print(" node" + index);
                        ps.print("[margin=\"0\", ");

                        printNodeWithLabel(label.toString(), "none", item, ps);

                        ps.println("];");
                    }
                }

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

                    if (!(item instanceof Projection)) {
                        if (usedNode instanceof Projection) {
                            // Projections have only one use
                            final Node use = usedNode.uses.getFirst().node;
                            ps.print(" node" + nodeindex.indexOf(use));
                            ps.print(":prj");
                            ps.print(nodeindex.indexOf(usedNode));
                        } else {
                            ps.print(" node" + nodeindex.indexOf(usedNode));
                        }
                        ps.print(" -> node" + index);

                        if (useEdge.use instanceof final PHIUse phiUse) {
                            ps.print("[");
                            ps.print("headlabel=\"if #" + nodeindex.indexOf(phiUse.origin) + "\", labeldistance=2, color=blue, constraint=false");
                            ps.print("]");
                        } else if (useEdge.use instanceof MemoryUse) {
                            ps.print("[labeldistance=2, color=green, constraint=false]");
                        } else if (useEdge.use instanceof DataFlowUse) {
                            ps.print("[");
                            ps.print("headlabel=\"*" + item.uses.indexOf(useEdge) + "\", labeldistance=2");
                            ps.print("]");
                        } else if (useEdge.use instanceof DefinedByUse) {
                            ps.print("[style=dotted]");
                        } else if (useEdge.use instanceof final ControlFlowUse cfu) {
                            ps.print("[labeldistance=2, color=red, fontcolor=red");
                            if (cfu.type == ControlType.BACKWARD) {
                                ps.print(", style=dashed");
                            }
                            ps.print("]");
                        }

                        ps.println(";");
                    }
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
                   d6 [style = invis];
                   d7 [style = invis];
                  }
                  c0 -> c1 [label="Control flow", style=solid, color=red]
                  c2 -> c3 [label="Control flow back edge", style=dashed, color=red]
                  d0 -> d1 [label="Data flow"]
                  d2 -> d3 [label="Declaration", style=dotted]
                  d4 -> d5 [label="PHI Data flow", color=blue]
                  d6 -> d7 [label="Memory flow", color=green]
                 }
                """);

        ps.println("}");
    }

    private static void printNode(final int index, final Node node, final PrintStream ps) {
        printNode(index, node, ps, "");
    }

    private static void printNode(final int index, final Node node, final PrintStream ps, final String labelSuffex) {
        printNode("box", index, node, ps, labelSuffex);
    }

    private static void printNode(final String shape, final int index, final Node node, final PrintStream ps, final String labelSuffex) {
        ps.print("label=\"#" + index + " " + node.debugDescription() + labelSuffex + "\"");
        if (node instanceof Method || node instanceof LabelNode || node instanceof Return || node instanceof ReturnValue || node instanceof Goto || node instanceof If || node instanceof Copy || node instanceof Invocation || node instanceof ClassInitialization || node instanceof MonitorEnter || node instanceof MonitorExit || node instanceof Throw || node instanceof ArrayStore || node instanceof ArrayLoad || node instanceof CheckCast || node instanceof PutField || node instanceof PutStatic || node instanceof MergeNode || node instanceof Projection || node instanceof LoopHeaderNode) {
            ps.print(",shape=" + shape +", fillcolor=lightgrey, style=filled");
        } else if (node.isConstant()) {
            ps.print(",shape=octagon, fillcolor=lightgreen, style=filled");
        } else if (node instanceof Value) {
            ps.print(",shape=octagon, fillcolor=orange, style=filled");
        }
    }

    private static void printNodeWithLabel(final String label, final String shape, final Node node, final PrintStream ps) {
        ps.print("label=" + label);
        if (node instanceof Method || node instanceof LabelNode || node instanceof Return || node instanceof ReturnValue || node instanceof Goto || node instanceof If || node instanceof Copy || node instanceof Invocation || node instanceof ClassInitialization || node instanceof MonitorEnter || node instanceof MonitorExit || node instanceof Throw || node instanceof ArrayStore || node instanceof ArrayLoad || node instanceof CheckCast || node instanceof PutField || node instanceof PutStatic || node instanceof MergeNode || node instanceof Projection || node instanceof LoopHeaderNode) {
            ps.print(",shape=" + shape +", fillcolor=lightgrey, style=filled");
        } else if (node.isConstant()) {
            ps.print(",shape=octagon, fillcolor=lightgreen, style=filled");
        } else if (node instanceof Value) {
            ps.print(",shape=octagon, fillcolor=orange, style=filled");
        }
    }

    public static void writeTo(final DominatorTree tree, final PrintStream ps) {
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
                    ps.print("[labeldistance=2, color=red, fontcolor=red");
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
                  c2 -> c3 [label="Control flow back edge", style=dashed, color=red]
                  d0 -> d1 [label="Data flow"]
                  d2 -> d3 [label="Declaration", style=dotted]
                  d4 -> d5 [label="PHI Data flow", color=blue]
                  d6 -> d7 [label="Dominance", color=green]
                 }
                """);
        ps.println("}");
        ps.flush();
    }

    public static void writeBytecodeCFGTo(final MethodAnalyzer analyzer, final PrintStream ps) {
        ps.println("digraph {");

        final MethodModel method = analyzer.getMethod();
        final Optional<CodeModel> optCode = method.code();
        if (optCode.isPresent()) {
            final CodeModel code = optCode.get();

            final MethodAnalyzer.Frame[] frames = analyzer.getFrames();
            final List<MethodAnalyzer.Frame> topologicalOrder = analyzer.getCodeModelTopologicalOrder();

            final List<CodeElement> elements = code.elementList();
            for (int i = 0; i < elements.size(); i++) {
                if (i == elements.size() - 1 && elements.get(i) instanceof LabelTarget) {
                    // Ignore last label
                    continue;
                }
                ps.print(" node" + i);
                ps.print("[");
                ps.print("label=\"");
                ps.print("#");
                ps.print(i);
                ps.print(" ");
                ps.print(elements.get(i));
                if (topologicalOrder != null && topologicalOrder.size() > i) {
                    ps.print(" Order: " + topologicalOrder.indexOf(frames[i]));
                }
                ps.print("\"");
                ps.println(", shape=box, fillcolor=lightgrey, style=filled];");
            }

            for (int elementIndex = 0; elementIndex < frames.length - 1; elementIndex++) {
                final MethodAnalyzer.Frame frame = frames[elementIndex];
                if (frame != null) {
                    for (final MethodAnalyzer.CFGEdge edge : frame.predecessors) {
                        ps.print(" node");
                        ps.print(edge.fromIndex());
                        ps.print(" -> node");
                        ps.print(elementIndex);
                        ps.print("[color=red");
                        if (edge.controlType() == ControlType.BACKWARD) {
                            ps.print(", style=dotted");
                        }
                        ps.println("];");
                    }
                }
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
                  }
                  c0 -> c1 [label="Control flow", style=solid, color=red]
                  c2 -> c3 [label="Control flow back edge", style=dashed, color=red]
                 }
                """);

        ps.println("}");

    }
}
