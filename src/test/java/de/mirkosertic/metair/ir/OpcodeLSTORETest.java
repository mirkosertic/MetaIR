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
public class OpcodeLSTORETest {

    @Test
    public void test_LSTORE(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.lconst_1();
            codeBuilder.lstore(0);
            codeBuilder.lconst_1();
            codeBuilder.lstore(2);
            codeBuilder.lconst_1();
            codeBuilder.lstore(4);
            codeBuilder.lconst_1();
            codeBuilder.lstore(6);
            codeBuilder.lconst_1();
            codeBuilder.lstore(8);
            codeBuilder.lconst_1();
            codeBuilder.lstore(10);
            codeBuilder.lconst_1();
            codeBuilder.lstore(12);
            codeBuilder.lconst_1();
            codeBuilder.lstore(7000);
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }
}
