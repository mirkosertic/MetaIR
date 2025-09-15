package de.mirkosertic.metair.ir;

public record FrameNamedProjection(String name) {
    public static final FrameNamedProjection DEFAULT = new FrameNamedProjection("default");
}
