package de.mirkosertic.metair.ir;

import java.lang.classfile.CodeElement;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.util.ArrayList;
import java.util.List;

public class Frame {

    protected final CodeElement codeElement;
    protected final List<FrameCFGEdge> predecessors;
    protected Frame immediateDominator;
    protected final int elementIndex;
    protected int indexInTopologicalOrder;
    protected Node entryPoint;
    protected Status in;
    protected Status out;
    protected List<StackMapFrameInfo> verificationInfos;

    public Frame(final int elementIndex, final CodeElement codeElement) {
        this.predecessors = new ArrayList<>();
        this.elementIndex = elementIndex;
        this.indexInTopologicalOrder = -1;
        this.codeElement = codeElement;
    }

    public Status copyIncomingToOutgoing() {
        out = in.copy();
        return out;
    }
}