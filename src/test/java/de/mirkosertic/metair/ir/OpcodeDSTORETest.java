package de.mirkosertic.metair.ir;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OpcodeDSTORETest {

    @Test
    public void test_DSTORE() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.dconst_1();
            codeBuilder.dstore(0);
            codeBuilder.dconst_1();
            codeBuilder.dstore(1);
            codeBuilder.dconst_1();
            codeBuilder.dstore(2);
            codeBuilder.dconst_1();
            codeBuilder.dstore(3);
            codeBuilder.dconst_1();
            codeBuilder.dstore(4);
            codeBuilder.dconst_1();
            codeBuilder.dstore(5);
            codeBuilder.dconst_1();
            codeBuilder.dstore(7);
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
