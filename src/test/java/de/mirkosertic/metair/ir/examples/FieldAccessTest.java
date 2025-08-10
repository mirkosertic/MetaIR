package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class FieldAccessTest {

    public static class Source {
        public byte byte_field;
        public boolean boolean_field;
        public short short_field;
        public char char_field;
        public int int_field;
        public long long_field;
        public float float_field;
        public double double_field;
        public String string_field;
        public int[] array_field;
    }

    public static class Destination {
        public byte byte_field;
        public boolean boolean_field;
        public short short_field;
        public char char_field;
        public int int_field;
        public long long_field;
        public float float_field;
        public double double_field;
        public String string_field;
        public int[] array_field;
    }

    public void copy_byte() {
        new Destination().byte_field = new Source().byte_field;
    }

    public void copy_boolean() {
        new Destination().boolean_field = new Source().boolean_field;
    }

    public void copy_short() {
        new Destination().short_field = new Source().short_field;
    }

    public void copy_char() {
        new Destination().char_field = new Source().char_field;
    }

    public void copy_int() {
        new Destination().int_field = new Source().int_field;
    }

    public void copy_long() {
        new Destination().long_field = new Source().long_field;
    }

    public void copy_float() {
        new Destination().float_field = new Source().float_field;
    }

    public void copy_double() {
        new Destination().double_field = new Source().double_field;
    }

    public void copy_string() {
        new Destination().string_field = new Source().string_field;
    }

    public void copy_array() {
        new Destination().array_field = new Source().array_field;
    }

    public byte return_byte() {
        return new Source().byte_field;
    }

    public boolean return_boolean() {
        return new Source().boolean_field;
    }

    public short return_short() {
        return new Source().short_field;
    }

    public char return_char() {
        return new Source().char_field;
    }

    public int return_int() {
        return new Source().int_field;
    }

    public long return_long() {
        return new Source().long_field;
    }

    public float return_float() {
        return new Source().float_field;
    }

    public double return_double() {
        return new Source().double_field;
    }

    public String return_string() {
        return new Source().string_field;
    }

    public int[] return_array() {
        return new Source().array_field;
    }
}