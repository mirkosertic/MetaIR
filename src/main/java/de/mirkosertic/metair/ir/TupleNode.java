package de.mirkosertic.metair.ir;

import java.util.HashMap;
import java.util.Map;

public abstract class TupleNode extends Node {

    private final Map<String, Node> namedNodes;

    public TupleNode() {
        this.namedNodes = new HashMap<>();
    }

    protected void registerAs(final String name, final Node value) {
        namedNodes.put(name, value);
    }

    public Node getNamedNode(final String name) {
        final Node r = namedNodes.get(name);
        if (r == null) {
            throw new IllegalArgumentException("No such value " + name + " in " + this);
        }
        return r;
    }
}
