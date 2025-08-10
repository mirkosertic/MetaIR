package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class StaticFieldAccessTest {

    public static class Source {
        public static byte byte_field;
        public static boolean boolean_field;
        public static short short_field;
        public static char char_field;
        public static int int_field;
        public static long long_field;
        public static float float_field;
        public static double double_field;
        public static String string_field;
        public static int[] array_field;
    }

    public static class Destination {
        public static byte byte_field;
        public static boolean boolean_field;
        public static short short_field;
        public static char char_field;
        public static int int_field;
        public static long long_field;
        public static float float_field;
        public static double double_field;
        public static String string_field;
        public static int[] array_field;
    }

    public void copy_byte() {
        Destination.byte_field = Source.byte_field;
    }

    public void copy_boolean() {
        Destination.boolean_field = Source.boolean_field;
    }

    public void copy_short() {
        Destination.short_field = Source.short_field;
    }

    public void copy_char() {
        Destination.char_field = Source.char_field;
    }

    public void copy_int() {
        Destination.int_field = Source.int_field;
    }

    public void copy_long() {
        Destination.long_field = Source.long_field;
    }

    public void copy_float() {
        Destination.float_field = Source.float_field;
    }

    public void copy_double() {
        Destination.double_field = Source.double_field;
    }

    public void copy_string() {
        Destination.string_field = Source.string_field;
    }

    public void copy_array() {
        Destination.array_field = Source.array_field;
    }

    public boolean return_boolean() {
        return Source.boolean_field;
    }

    public short return_short() {
        return Source.short_field;
    }

    public char return_char() {
        return Source.char_field;
    }

    public int return_int() {
        return Source.int_field;
    }

    public long return_long() {
        return Source.long_field;
    }

    public float return_float() {
        return Source.float_field;
    }

    public double return_double() {
        return Source.double_field;
    }

    public String return_string() {
        return Source.string_field;
    }

    public int[] return_array() {
        return Source.array_field;
    }
}