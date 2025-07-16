package de.mirkosertic.metair.ir;

import java.io.PrintStream;
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
                        ps.print("headlabel=\"" + cfu.condition.debugDescription() + "\",labeldistance=2,color=red, fontcolor=red");
                        if (cfu.type == ControlType.BACKWARD) {
                            ps.print(" ,style=dashed");
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

        ps.println("}");
    }

    private static void printNode(final int index, final Node node, final PrintStream ps) {
        ps.print("label=\"#" + index + " " + node.debugDescription() + "\"");
        if (node instanceof Method || node instanceof Label || node instanceof Return || node instanceof ReturnValue || node instanceof Goto || node instanceof If || node instanceof Copy || node instanceof Invocation || node instanceof ClassInitialization) {
            ps.print(",shape=box,fillcolor=lightgrey,style=filled");
        } else if (node.isConstant()) {
            ps.print(",shape=octagon,fillcolor=lightgreen,style=filled");
        } else if (node instanceof Value) {
            ps.print(",shape=octagon,fillcolor=orange,style=filled");
        }
    }
}
