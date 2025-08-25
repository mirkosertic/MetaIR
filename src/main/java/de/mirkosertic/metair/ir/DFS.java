package de.mirkosertic.metair.ir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DFS {

    private final List<Node> nodesInOrder;

    public DFS(final Node startNode) {
        final List<Node> reversePostOrder = new ArrayList<>();
        final Deque<Node> worklist = new ArrayDeque<>();
        worklist.add(startNode);
        final Set<Node> marked = new HashSet<>();
        marked.add(startNode);
        while(!worklist.isEmpty()) {
            final Node currentNode = worklist.peek();
            final List<Node> forwardNodes = new ArrayList<>();

            for (final Node user : currentNode.usedBy) {
                for (final Node.UseEdge edge : user.uses) {
                    if (edge.node() == currentNode) {
                        if (edge.use() instanceof final ControlFlowUse cfu && cfu.type == ControlType.FORWARD) {
                            final Set<Node> preds = predecessorsOf(user);
                            if (marked.containsAll(preds)) {
                                //System.out.println("All predecessors of node " + currentNode + " visited, continuing");
                                forwardNodes.add(user);
                            } else {
                                //System.out.println("Not all preds of node " + currentNode + " visited yet, skipping for now");
                            }
                        } else if (edge.use() instanceof DefinedByUse) {
                            final Set<Node> preds = predecessorsOf(user);
                            if (marked.containsAll(preds)) {
                                //System.out.println("All predecessors of node " + currentNode + " visited, continuing");
                                forwardNodes.add(user);
                            } else {
                                //System.out.println("Not all preds of node " + currentNode + " visited yet, skipping for now");
                            }
                        } else if (edge.use() instanceof DataFlowUse) {
                            final Set<Node> preds = predecessorsOf(user);
                            if (marked.containsAll(preds)) {
                                //System.out.println("All predecessors of node " + currentNode + " visited, continuing");
                                forwardNodes.add(user);
                            } else {
                                //System.out.println("Not all preds of node " + currentNode + " visited yet, skipping for now");
                            }
                        } else if (edge.use() instanceof MemoryUse) {
                            final Set<Node> preds = predecessorsOf(user);
                            if (marked.containsAll(preds)) {
                                //System.out.println("All predecessors of node " + currentNode + " visited, continuing");
                                forwardNodes.add(user);
                            } else {
                                //System.out.println("Not all preds of node " + currentNode + " visited yet, skipping for now");
                            }
                        }
                    }
                }
            }

            // TODO: Sort the nodes by their usage, with control flow use first, then data flow, then arg-flow, and then define flow
            forwardNodes.sort(Comparator.comparing(o -> o.getClass().getSimpleName()));

            if (!forwardNodes.isEmpty()) {
                boolean somethingFound = false;
                for (final Node node : forwardNodes) {
                    if (marked.add(node)) {
                        worklist.push(node);
                        somethingFound = true;
                    }
                }
                if (!somethingFound) {
                    reversePostOrder.add(currentNode);
                    worklist.pop();
                }
            } else {
                reversePostOrder.add(currentNode);
                worklist.pop();
            }
        }

        nodesInOrder = new ArrayList<>();
        for (int i = reversePostOrder.size() - 1; i >= 0; i--) {
            nodesInOrder.add(reversePostOrder.get(i));
        }
    }

    private Set<Node> predecessorsOf(final Node node) {
        final Set<Node> predecessors = new HashSet<>();
        for (final Node.UseEdge edge : node.uses) {
            if (edge.use() instanceof final ControlFlowUse cfu) {
                if (cfu.type == ControlType.FORWARD) {
                    predecessors.add(edge.node());
                }
            } else predecessors.add(edge.node());
        }
        return predecessors;
    }

    public List<Node> getTopologicalOrder() {
        return nodesInOrder;
    }
}