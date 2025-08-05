package de.mirkosertic.metair.ir;

public class ExtractControlFlowProjection extends Node implements Projection {

    private final String name;

    ExtractControlFlowProjection(final String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String debugDescription() {
        return name;
    }
}
