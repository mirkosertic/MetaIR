package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import de.mirkosertic.metair.ir.test.MetaIRTestTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MetaIRTestTools.class)
public class OpcodeLOOKUPSWITCHTest {

    @Test
    public void test_LOOKUPSWITCH(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_int), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            final Label defaultLabel = codeBuilder.newLabel();
            final Label case1 = codeBuilder.newLabel();
            codeBuilder.iconst_0();
            codeBuilder.lookupswitch(defaultLabel, List.of(SwitchCase.of(10, case1)));
            codeBuilder.labelBinding(defaultLabel);
            codeBuilder.iconst_1();
            codeBuilder.ireturn();
            codeBuilder.labelBinding(case1);
            codeBuilder.iconst_2();
            codeBuilder.ireturn();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }
}
