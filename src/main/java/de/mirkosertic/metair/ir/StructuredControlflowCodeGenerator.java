package de.mirkosertic.metair.ir;

public interface StructuredControlflowCodeGenerator {

    void begin(Method method);

    void writeBreakTo(final String label);

    void writeContinueTo(final String label);

    void finishBlock(final Sequencer.Block block, boolean lastBlock);

    void write(Return node);

    void write(ReturnValue node);

    void write(Goto node);

    void write(Throw node);
}
