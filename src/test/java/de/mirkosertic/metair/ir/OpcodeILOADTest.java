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

public class OpcodeILOADTest {

    @Test
    public void test_ILOAD() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.iconst_0();
            codeBuilder.istore(0);
            codeBuilder.iload(0);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(1);
            codeBuilder.iload(1);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(2);
            codeBuilder.iload(2);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(3);
            codeBuilder.iload(3);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(4);
            codeBuilder.iload(4);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(5);
            codeBuilder.iload(5);
            codeBuilder.pop();
            codeBuilder.iconst_0();
            codeBuilder.istore(7);
            codeBuilder.iload(7);
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
