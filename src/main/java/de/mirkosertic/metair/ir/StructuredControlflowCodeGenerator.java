package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.Optional;

public interface StructuredControlflowCodeGenerator {

    void begin(final Method method);

    void startBlock(final Sequencer.Block b);

    void writeBreakTo(final String label);

    void writeContinueTo(final String label);

    void finishBlock(final Sequencer.Block block, boolean lastBlock);

    void write(final Return node);

    void write(final ReturnValue node);

    void write(final Goto node);

    void write(final Throw node);

    void startIfWithTrueBlock(final If node);

    void startIfElseBlock(final If node);

    void finishIfBlock();

    void startLookupSwitch(final LookupSwitch node);

    void writeSwitchCase(final int key);

    void finishSwitchCase();

    void writeSwitchDefaultCase();

    void finishSwitchDefault();

    void finishLookupSwitch();

    void startTableSwitch(final TableSwitch node);

    void startTableSwitchDefaultBlock();

    void finishTableSwitchDefaultBlock();

    void finishTableSwitch();

    void startTryCatch(final ExceptionGuard node);

    void startCatchBlock();

    void startCatchHandler(final Optional<ClassDesc> classDesc);

    void writeRethrowException();

    void finishCatchHandler();

    void finishTryCatch();
}
