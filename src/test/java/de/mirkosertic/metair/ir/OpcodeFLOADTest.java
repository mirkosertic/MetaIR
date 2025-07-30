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

public class OpcodeFLOADTest {

    @Test
    public void test_FLOAD() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.fconst_2();
            codeBuilder.fstore(0);
            codeBuilder.fload(0);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(1);
            codeBuilder.fload(1);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(2);
            codeBuilder.fload(2);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(3);
            codeBuilder.fload(3);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(4);
            codeBuilder.fload(4);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(5);
            codeBuilder.fload(5);
            codeBuilder.pop();
            codeBuilder.fconst_2();
            codeBuilder.fstore(7);
            codeBuilder.fload(7);
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
