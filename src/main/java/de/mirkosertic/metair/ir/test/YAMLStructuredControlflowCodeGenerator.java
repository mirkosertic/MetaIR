package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.ExceptionGuard;
import de.mirkosertic.metair.ir.Goto;
import de.mirkosertic.metair.ir.If;
import de.mirkosertic.metair.ir.LookupSwitch;
import de.mirkosertic.metair.ir.Method;
import de.mirkosertic.metair.ir.Return;
import de.mirkosertic.metair.ir.ReturnValue;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.ir.StructuredControlflowCodeGenerator;
import de.mirkosertic.metair.ir.TableSwitch;
import de.mirkosertic.metair.ir.Throw;

import java.lang.constant.ClassDesc;
import java.util.Optional;

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
    public void startBlock(final Sequencer.Block b) {
    }

    @Override
    public void startIfWithTrueBlock(final If node) {
    }

    @Override
    public void startIfElseBlock(final If node) {
    }

    @Override
    public void finishIfBlock() {
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

    @Override
    public void startLookupSwitch(final LookupSwitch node) {
    }

    @Override
    public void writeSwitchCase(final int key) {
    }

    @Override
    public void finishSwitchCase() {
    }

    @Override
    public void writeSwitchDefaultCase() {
    }

    @Override
    public void finishSwitchDefault() {
    }

    @Override
    public void finishLookupSwitch() {
    }

    @Override
    public void startTableSwitch(final TableSwitch node) {
    }

    @Override
    public void startTableSwitchDefaultBlock() {
    }

    @Override
    public void finishTableSwitchDefaultBlock() {
    }

    @Override
    public void finishTableSwitch() {
    }

    @Override
    public void startTryCatch(final ExceptionGuard node) {
    }

    @Override
    public void startCatchBlock() {
    }

    @Override
    public void startCatchHandler(final Optional<ClassDesc> classDesc) {
    }

    @Override
    public void writeRethrowException() {
    }

    @Override
    public void finishCatchHandler() {
    }

    @Override
    public void finishTryCatch() {
    }
}
