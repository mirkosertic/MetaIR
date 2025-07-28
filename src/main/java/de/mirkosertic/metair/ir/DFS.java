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

            for (final Map.Entry<Node, List<Node.UseEdge>> emtry : currentNode.outgoingControlFlows().entrySet()) {
                for (final Node.UseEdge use : emtry.getValue()) {
                    if (use.use instanceof final ControlFlowUse cfu) {
                        if (cfu.type == ControlType.FORWARD) {
                            forwardNodes.add(emtry.getKey());
                        }
                    }
                }
            }

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