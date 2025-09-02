package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.Goto;
import de.mirkosertic.metair.ir.Method;
import de.mirkosertic.metair.ir.Return;
import de.mirkosertic.metair.ir.ReturnValue;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.ir.StructuredControlflowCodeGenerator;
import de.mirkosertic.metair.ir.Throw;

public class YAMLStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    @Override
    public void begin(final Method method) {

    }

    @Override
    public void writeBreakTo(final String label) {

    }

    @Override
    public void writeContinueTo(final String label) {

    }

    @Override
    public void finishBlock(final Sequencer.Block block, final boolean lastBlock) {

    }

    @Override
    public void write(final Return node) {

    }

    @Override
    public void write(final ReturnValue node) {

    }

    @Override
    public void write(final Goto node) {

    }

    @Override
    public void write(final Throw node) {

    }
}
