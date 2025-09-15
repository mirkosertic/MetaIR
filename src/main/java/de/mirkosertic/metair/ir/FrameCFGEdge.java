package de.mirkosertic.metair.ir;

public record FrameCFGEdge(int fromIndex, FrameNamedProjection projection, FlowType flowType) {
}