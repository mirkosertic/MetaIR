package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class ReturnTest {

    public byte returnByte() {
        return 10;
    }

    public boolean returnBoolean() {
        return true;
    }

    public char returnChar() {
        return 'a';
    }

    public short returnShort() {
        return 10;
    }

    public int returnInt() {
        return 10;
    }

    public long returnLong() {
        return 10;
    }

    public float returnFloat() {
        return 10.0f;
    }

    public double returnDouble() {
        return 10.0d;
    }

    public Object returnObject() {
        return this;
    }

    public Object[] returnArray() {
        return new Object[10];
    }
}
