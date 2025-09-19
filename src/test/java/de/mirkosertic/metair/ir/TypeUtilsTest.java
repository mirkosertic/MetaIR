package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeUtilsTest {

    @Test
    public void bytesIsCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_byte)).isFalse();
    }

    @Test
    public void charsIsCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_char)).isFalse();
    }

    @Test
    public void intIsCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_int)).isFalse();
    }

    @Test
    public void booleanCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_boolean)).isFalse();
    }

    @Test
    public void floatIsCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_float)).isFalse();
    }

    @Test
    public void ObjectIsCategory1() {
        assertThat(TypeUtils.isCategory2(IRType.CD_Object)).isFalse();
    }

    @Test
    public void longIsCategory2() {
        assertThat(TypeUtils.isCategory2(IRType.CD_long)).isTrue();
    }

    @Test
    public void doubleIsCategory2() {
        assertThat(TypeUtils.isCategory2(IRType.CD_double)).isTrue();
    }

    @Test
    public void jvmInternalTypeOf() {
        assertThat(TypeUtils.jvmInternalTypeOf(IRType.CD_byte)).isEqualTo(IRType.CD_int);
        assertThat(TypeUtils.jvmInternalTypeOf(IRType.CD_char)).isEqualTo(IRType.CD_int);
        assertThat(TypeUtils.jvmInternalTypeOf(IRType.CD_boolean)).isEqualTo(IRType.CD_int);
        assertThat(TypeUtils.jvmInternalTypeOf(IRType.CD_short)).isEqualTo(IRType.CD_int);
        assertThat(TypeUtils.jvmInternalTypeOf(IRType.CD_Object)).isEqualTo(IRType.CD_Object);
    }
}