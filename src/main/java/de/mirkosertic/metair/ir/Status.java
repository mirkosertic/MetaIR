package de.mirkosertic.metair.ir;

import java.util.Stack;

public class Status {

    protected final static int UNDEFINED_LINE_NUMBER = -1;

    protected int lineNumber;
    protected final Value[] locals;
    protected final Stack<Value> stack;
    protected Node control;
    protected Node memory;

    protected Status(final int maxLocals) {
        this.locals = new Value[maxLocals];
        this.stack = new Stack<>();
        this.lineNumber = UNDEFINED_LINE_NUMBER;
    }

    protected int numberOfLocals() {
        return locals.length;
    }

    protected Value getLocal(final int slot) {
        if (slot > 0) {
            if (locals[slot - 1] != null && TypeUtils.isCategory2(locals[slot - 1].type)) {
                // This is an illegal state!
                throw new IllegalStateException("Slot " + (slot - 1) + " is already set to a category 2 value, so cannot read slot " + slot);
            }
        }
        return locals[slot];
    }

    protected void setLocal(final int slot, final Value value) {
        locals[slot] = value;
        if (slot > 0) {
            if (locals[slot - 1] != null && TypeUtils.isCategory2(locals[slot - 1].type)) {
                // This is an illegal state!
                throw new IllegalStateException("Slot " + (slot - 1) + " is already set to a category 2 value, so cannot set slot " + slot);
            }
        }
    }

    protected Status copy() {
        final Status result = new Status(locals.length);
        result.lineNumber = lineNumber;
        System.arraycopy(locals, 0, result.locals, 0, locals.length);
        result.stack.addAll(stack);
        result.control = control;
        result.memory = memory;
        return result;
    }

    protected Value pop() {
        return stack.pop();
    }

    protected Value peek() {
        return stack.peek();
    }

    protected void push(final Value value) {
        stack.push(value);
    }
}
