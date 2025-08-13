package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

import java.util.List;

@MetaIRTest
public class InvokeDynamicTest {

    public long countList(final List<Integer> list, final int min) {
        return list.stream().filter(t -> t >= min).count();
    }

    public String concat(final int a, final int b) {
        return "hello " + a + " world " + b;
    }
}
