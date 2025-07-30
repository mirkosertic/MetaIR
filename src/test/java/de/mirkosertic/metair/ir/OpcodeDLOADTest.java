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

public class OpcodeDLOADTest {

    @Test
    public void test_DLOAD() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.dconst_1();
            codeBuilder.dstore(0);
            codeBuilder.dload(0);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(1);
            codeBuilder.dload(1);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(2);
            codeBuilder.dload(2);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(3);
            codeBuilder.dload(3);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(4);
            codeBuilder.dload(4);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(5);
            codeBuilder.dload(5);
            codeBuilder.pop();
            codeBuilder.dconst_1();
            codeBuilder.dstore(7);
            codeBuilder.dload(7);
            codeBuilder.pop();
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
    }
}
