package de.mirkosertic.metair.ir;

import java.util.*;

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
                    if (edge.use instanceof final ControlFlowUse cfu && cfu.type == ControlType.FORWARD && edge.node == currentNode) {
                        forwardNodes.add(user);
                    } else if (edge.use instanceof DefinedByUse) {
                        forwardNodes.add(user);
                    } else if (edge.use instanceof DataFlowUse) {
                        forwardNodes.add(user);
                    }
                }
            }

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