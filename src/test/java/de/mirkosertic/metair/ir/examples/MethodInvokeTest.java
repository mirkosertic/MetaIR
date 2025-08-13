package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class MethodInvokeTest {

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

    public static void simple_method(final byte byte_arg,
                                     final boolean boolean_arg,
                                     final short short_arg,
                                     final char char_arg,
                                     final int int_arg,
                                     final long long_arg,
                                     final float float_arg,
                                     final double double_arg,
                                     final String string_arg,
                                     final int[] array_arg) {
    }

    public static byte simple_method_return_byte(final byte byte_arg,
                                                 final boolean boolean_arg,
                                                 final short short_arg,
                                                 final char char_arg,
                                                 final int int_arg,
                                                 final long long_arg,
                                                 final float float_arg,
                                                 final double double_arg,
                                                 final String string_arg,
                                                 final int[] array_arg) {
        return byte_arg;
    }

    public static char simple_method_return_char(final byte byte_arg,
                                                 final boolean boolean_arg,
                                                 final short short_arg,
                                                 final char char_arg,
                                                 final int int_arg,
                                                 final long long_arg,
                                                 final float float_arg,
                                                 final double double_arg,
                                                 final String string_arg,
                                                 final int[] array_arg) {
        return char_arg;
    }

    public static short simple_method_return_short(final byte byte_arg,
                                                   final boolean boolean_arg,
                                                   final short short_arg,
                                                   final char char_arg,
                                                   final int int_arg,
                                                   final long long_arg,
                                                   final float float_arg,
                                                   final double double_arg,
                                                   final String string_arg,
                                                   final int[] array_arg) {
        return short_arg;
    }

    public static boolean simple_method_return_boolean(final byte byte_arg,
                                                       final boolean boolean_arg,
                                                       final short short_arg,
                                                       final char char_arg,
                                                       final int int_arg,
                                                       final long long_arg,
                                                       final float float_arg,
                                                       final double double_arg,
                                                       final String string_arg,
                                                       final int[] array_arg) {
        return boolean_arg;
    }

    public void static_invocation() {
        final Source source = new Source();
        simple_method(source.byte_field, source.boolean_field, source.short_field, source.char_field, source.int_field, source.long_field, source.float_field, source.double_field, source.string_field, source.array_field);
    }

    public byte static_invocation_return_byte() {
        final Source source = new Source();
        return simple_method_return_byte(source.byte_field, source.boolean_field, source.short_field, source.char_field, source.int_field, source.long_field, source.float_field, source.double_field, source.string_field, source.array_field);
    }

    public char static_invocation_return_char() {
        final Source source = new Source();
        return simple_method_return_char(source.byte_field, source.boolean_field, source.short_field, source.char_field, source.int_field, source.long_field, source.float_field, source.double_field, source.string_field, source.array_field);
    }

    public short static_invocation_return_short() {
        final Source source = new Source();
        return simple_method_return_short(source.byte_field, source.boolean_field, source.short_field, source.char_field, source.int_field, source.long_field, source.float_field, source.double_field, source.string_field, source.array_field);
    }

    public boolean static_invocation_return_boolean() {
        final Source source = new Source();
        return simple_method_return_boolean(source.byte_field, source.boolean_field, source.short_field, source.char_field, source.int_field, source.long_field, source.float_field, source.double_field, source.string_field, source.array_field);
    }

    public static boolean staticReturnBoolean(final boolean bool) {
        return !bool;
    }

    public boolean returnBoolean() {
        return staticReturnBoolean(true);
    }

    public static byte staticReturnByte(final byte bt) {
        return bt;
    }

    public byte returnByte() {
        return staticReturnByte((byte) 10);
    }

    public static short staticReturnShort(final short st) {
        return st;
    }

    public short returnShort() {
        return staticReturnShort((short) 10);
    }

    public static char staticReturnChar(final char c) {
        return c;
    }

    public char returnChar() {
        return staticReturnChar((char) 10);
    }
}