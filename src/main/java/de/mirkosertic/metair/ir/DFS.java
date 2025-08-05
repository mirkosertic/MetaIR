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
        final Deque<Node> currentPath = new ArrayDeque<>();
        currentPath.add(startNode);
        final Set<Node> marked = new HashSet<>();
        marked.add(startNode);
        while(!currentPath.isEmpty()) {
            final Node currentNode = currentPath.peek();
            final List<Node> forwardNodes = new ArrayList<>();

            for (final Node user : currentNode.usedBy) {
                for (final Node.UseEdge edge : user.uses) {
                    if (edge.node == currentNode) {
                        if (edge.use instanceof final ControlFlowUse cfu && cfu.type == ControlType.FORWARD) {
                            forwardNodes.add(user);
                        } else if (edge.use instanceof DefinedByUse) {
                            forwardNodes.add(user);
                        } else if (edge.use instanceof DataFlowUse) {
                            forwardNodes.add(user);
                        } else if (edge.use instanceof MemoryUse) {
                            forwardNodes.add(user);
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
                        currentPath.push(node);
                        somethingFound = true;
                    }
                }
                if (!somethingFound) {
                    reversePostOrder.add(currentNode);
                    currentPath.pop();
                }
            } else {
                reversePostOrder.add(currentNode);
                currentPath.pop();
            }
        }

        nodesInOrder = new ArrayList<>();
        for (int i = reversePostOrder.size() - 1; i >= 0; i--) {
            nodesInOrder.add(reversePostOrder.get(i));
        }
    }

    public List<Node> getTopologicalOrder() {
        return nodesInOrder;
    }
}