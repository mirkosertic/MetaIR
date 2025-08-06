package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

class TypeUtilsTest {

    @Test
    public void bytesIsCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_byte)).isFalse();
    }

    @Test
    public void charsIsCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_char)).isFalse();
    }

    @Test
    public void intIsCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_int)).isFalse();
    }

    @Test
    public void booleanCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_boolean)).isFalse();
    }

    @Test
    public void floatIsCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_float)).isFalse();
    }

    @Test
    public void ObjectIsCategory1() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_Object)).isFalse();
    }

    @Test
    public void longIsCategory2() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_long)).isTrue();
    }

    @Test
    public void doubleIsCategory2() {
        assertThat(TypeUtils.isCategory2(ConstantDescs.CD_double)).isTrue();
    }

    @Test
    public void isPrimitive() {
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_byte)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_char)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_short)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_int)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_boolean)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_long)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_float)).isTrue();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_double)).isTrue();

        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_String)).isFalse();
        assertThat(TypeUtils.isPrimitive(ConstantDescs.CD_String.arrayType())).isFalse();
    }
}