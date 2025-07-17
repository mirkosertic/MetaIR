package de.mirkosertic.metair.ir;

public class MethodTest {

/*    @Test
    public void testDefineRuntimeClassReference() {
        ClassFile
        final Method method = new Method(new MethodNode());

        final RuntimeclassReference r1 = method.defineRuntimeclassReference(ConstantDescs.CD_String);
        final RuntimeclassReference r2 = method.defineRuntimeclassReference(ConstantDescs.CD_String);

        assertThat(r1).isSameAs(r2);
        assertThat(r1.usedBy).isEmpty();

        assertThat(r1.uses.size()).isEqualTo(1);
        assertThat(r1.uses.getFirst().node).isSameAs(method);
        assertThat(r1.uses.getFirst().use).isSameAs(DefinedByUse.INSTANCE);
    }

    @Test
    public void testMarkBackEdges() {
        final Method method = new Method(new MethodNode());
        final If iff = new If(new PrimitiveBoolean(true));
        final Return ret = new Return();

        method.controlFlowsTo(iff, ControlType.FORWARD);
        iff.controlFlowsTo(iff, ControlType.FORWARD);
        iff.controlFlowsTo(ret, ControlType.FORWARD);

        method.markBackEdges();

        assertThat(method.uses).isEmpty();
        assertThat(method.usedBy).containsExactly(iff);

        assertThat(iff.uses.size()).isEqualTo(3);
        assertThat(iff.uses.stream().map(t -> t.node).collect(Collectors.toSet())).contains(iff, method);
        assertThat(iff.usedBy).containsExactlyInAnyOrder(iff, ret);

        assertThat(ret.uses.size()).isEqualTo(1);
        assertThat(ret.uses.stream().map(t -> t.node).collect(Collectors.toSet())).contains(iff);
        assertThat(ret.usedBy).isEmpty();
    }*/
}