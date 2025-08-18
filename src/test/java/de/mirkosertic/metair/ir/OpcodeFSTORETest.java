package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import de.mirkosertic.metair.ir.test.MetaIRTestTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MetaIRTestTools.class)
public class OpcodeFSTORETest {

    @Test
    public void test_FSTORE(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.fconst_2();
            codeBuilder.fstore(0);
            codeBuilder.fconst_2();
            codeBuilder.fstore(1);
            codeBuilder.fconst_2();
            codeBuilder.fstore(2);
            codeBuilder.fconst_2();
            codeBuilder.fstore(3);
            codeBuilder.fconst_2();
            codeBuilder.fstore(4);
            codeBuilder.fconst_2();
            codeBuilder.fstore(5);
            codeBuilder.fconst_2();
            codeBuilder.fstore(7);
            codeBuilder.fconst_2();
            codeBuilder.fstore(7000);
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }
}
