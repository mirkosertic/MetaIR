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
public class OpcodeISTORETest {

    @Test
    public void test_ISTORE(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.iconst_0();
            codeBuilder.istore(0);
            codeBuilder.iconst_0();
            codeBuilder.istore(1);
            codeBuilder.iconst_0();
            codeBuilder.istore(2);
            codeBuilder.iconst_0();
            codeBuilder.istore(3);
            codeBuilder.iconst_0();
            codeBuilder.istore(4);
            codeBuilder.iconst_0();
            codeBuilder.istore(5);
            codeBuilder.iconst_0();
            codeBuilder.istore(7);
            codeBuilder.iconst_0();
            codeBuilder.istore(7000);
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }
}
