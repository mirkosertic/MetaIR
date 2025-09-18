package de.mirkosertic.metair.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DominatorTree {

    final List<Node> preOrder;
    final Map<Node, Node> idom;

    final List<Node> rpo;

    public DominatorTree(final Node start) {
        preOrder = new DFS2(start).getTopologicalOrder();
        idom = new HashMap<>();
        rpo = new ArrayList<>();
        computeDominators();
        computeRPO(start);
    }

    private void computeRPO(final Node consumer) {
        final List<Node> finished = new ArrayList<>();
        final Set<Node> visited = new HashSet<>();
        computeRPO(consumer, finished, visited);

        Collections.reverse(finished);
        rpo.addAll(finished);
    }

    private void computeRPO(final Node current, final List<Node> finished, final Set<Node> visited) {
        if (visited.add(current)) {
            for (final Node user : current.usedBy.stream().sorted(Comparator.comparing((Node o) -> o.getClass().getSimpleName())).toList()) {
                for (final Node.UseEdge edge : user.uses) {
                    if (edge.node() == current) {
                        if (edge.use() instanceof final ControlFlowUse cfu) {
                            if (cfu.type == FlowType.FORWARD) {
                                computeRPO(user, finished, visited);
                            }
                        } else {
                            computeRPO(user, finished, visited);
                        }
                    }
                }
            }
            finished.add(current);
        }
    }

    public List<Node> getPreOrder() {
        return preOrder;
    }

    public List<Node> getRpo() {
        return rpo;
    }

    private void computeDominators() {
        final Node firstElement = preOrder.getFirst();
        idom.put(firstElement, firstElement);

        boolean changed;
        do {
            changed = false;
            for (final Node v : preOrder) {
                if (v.equals(firstElement))
                    continue;
                final Node oldIdom = getIDom(v);
                Node newIdom = null;
                for (final Node.UseEdge edge : v.uses) {
                    if (edge.use() instanceof ControlFlowUse || edge.use() instanceof DefinedByUse || edge.use() instanceof DataFlowUse || edge.use() instanceof MemoryUse) {
                        if (getIDom(edge.node()) == null)
                            /* not yet analyzed */ continue;
                        if (newIdom == null) {
                            /* If we only have one (defined) predecessor pre, IDom(v) = pre */
                            newIdom = edge.node();
                        } else {
                            /* compute the intersection of all defined predecessors of v */
                            if (!(v instanceof PHI)) {
                                newIdom = intersectIDoms(edge.node(), newIdom);
                            }
                        }
                    }
                }
                if (newIdom == null) {
                    throw new AssertionError("newIDom == null !, for " + v);
                }
                if (!newIdom.equals(oldIdom)) {
                    changed = true;
                    this.idom.put(v, newIdom);
                }
            }
        } while (changed);
    }

    public Node getIDom(final Node node) {
        return idom.get(node);
    }

    private Node intersectIDoms(Node v1, Node v2) {
        while (v1 != v2) {
            if (preOrder.indexOf(v1) < preOrder.indexOf(v2)) {
                v2 = getIDom(v2);
            } else {
                v1 = getIDom(v1);
            }
        }
        return v1;
    }

    /**
     * Check whether a node dominates another one.
     *
     * @return true, if <code>dominator</code> dominates <code>dominated</code> w.r.t to the entry node
     */
    public boolean dominates(final Node dominator, final Node dominated) {
        if(dominator.equals(dominated)) {
            return true; // Domination is reflexive ;)
        }
        Node dom = getIDom(dominated);
        // as long as dominated >= dominator
        while(dom != null && preOrder.indexOf(dom) >= preOrder.indexOf(dominator) && ! dom.equals(dominator)) {
            dom = getIDom(dom);
        }
        return dominator.equals(dom);
    }

    public Set<Node> getStrictDominators(final Node n) {
        final Set<Node> strictDoms = new HashSet<>();
        Node dominated = n;
        Node iDom = getIDom(n);
        while(iDom != dominated) {
            strictDoms.add(iDom);
            dominated = iDom;
            iDom = getIDom(dominated);
        }
        return strictDoms;
    }

    public Set<Node> immediatelyDominatedNodesOf(final Node n) {
        final Set<Node> result = new HashSet<>();
        for (final Map.Entry<Node, Node> entry : idom.entrySet()) {
            if (entry.getValue() == n) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Set<Node> domSetOf(final Node n) {
        final Set<Node> theDomSet = new HashSet<>();
        addToDomSet(n, theDomSet);
        return theDomSet;
    }

    private void addToDomSet(final Node n, final Set<Node> domset) {
        domset.add(n);
        for (final Map.Entry<Node, Node> idomEntry : idom.entrySet()) {
            if (idomEntry.getValue() == n && preOrder.indexOf(idomEntry.getKey()) > preOrder.indexOf(n)) {
                addToDomSet(idomEntry.getKey(), domset);
            }
        }
    }
}
