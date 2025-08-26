package de.mirkosertic.metair.ir;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DFS2 {

    private final List<Node> nodesInOrder;
    private final List<Node> workList;
    private final Set<Node> visited;
    private final Map<Node, Set<Node>> precomputedPredecessors;
    private final Map<Node, List<Node>> precomputedForwards;

    public DFS2(final Node node) {

        this.nodesInOrder = new ArrayList<>();
        this.workList = new ArrayList<>();
        this.visited = new HashSet<>();
        this.precomputedPredecessors = new HashMap<>();
        this.precomputedForwards = new HashMap<>();

        long safelock = 0;

        workList.add(node);
        listhandling: while (!workList.isEmpty()) {
            safelock++;
            if (safelock > 1000) {
                throw new IllegalStateException("Unschedulable IR detected!");
            }

            for (final Node currentNode : workList) {
                if (check(currentNode)) continue listhandling;
            }
        }
    }

    private boolean check(final Node currentNode) {
        final Set<Node> predecessors = precomputedPredecessors.computeIfAbsent(currentNode, this::predecessorsOf);

        if (visited.containsAll(predecessors)) {
            // All predecessors are fully resolved, we can continue resolving this node

            nodesInOrder.add(currentNode);

            visited.add(currentNode);

            final List<Node> forwardNodes = getForwardNodesFor(currentNode);

            if (forwardNodes.isEmpty()) {
                workList.remove(currentNode);
                return true;
            } else {
                // TODO: Sort the nodes by their usage, with control flow use first, then data flow, then arg-flow, and then define flow
                forwardNodes.sort(Comparator.comparing(o -> o.getClass().getSimpleName()));
                for (final Node forwardNode : forwardNodes) {
                    if (!workList.contains(forwardNode) && !visited.contains(forwardNode)) {
                        workList.add(forwardNode);
                    }
                }

                workList.remove(currentNode);

                return true;
            }
        }
        return false;
    }

    private List<Node> getForwardNodesFor(final Node currentNode) {
        return precomputedForwards.computeIfAbsent(currentNode, key -> {
            final List<Node> forwardNodes = new ArrayList<>();
            for (final Node user : key.usedBy) {
                for (final Node.UseEdge edge : user.uses) {
                    if (edge.node() == key) {
                        if (edge.use() instanceof final ControlFlowUse cfu && cfu.type == FlowType.FORWARD) {
                            forwardNodes.add(user);
                        } else if (edge.use() instanceof final PHIUse pu && pu.type == FlowType.FORWARD) {
                            forwardNodes.add(user);
                        } else if (edge.use() instanceof DefinedByUse) {
                            forwardNodes.add(user);
                        } else if (edge.use() instanceof DataFlowUse) {
                            forwardNodes.add(user);
                        } else if (edge.use() instanceof MemoryUse) {
                            forwardNodes.add(user);
                        }
                    }
                }
            }
            return forwardNodes;
        });
    }

    public List<Node> getTopologicalOrder() {
        return nodesInOrder;
    }

    private Set<Node> predecessorsOf(final Node node) {
        final Set<Node> predecessors = new HashSet<>();
        for (final Node.UseEdge edge : node.uses) {
            if (edge.use() instanceof final ControlFlowUse cfu) {
                if (cfu.type == FlowType.FORWARD) {
                    predecessors.add(edge.node());
                }
            } else if (edge.use() instanceof final PHIUse pu) {
                if (pu.type == FlowType.FORWARD) {
                    predecessors.add(edge.node());
                }
            } else predecessors.add(edge.node());
        }
        return predecessors;
    }
}
