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

public class OpcodeANEWARRAYTest {

    @Test
    public void test_ANEWARRAY_Object() throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            // New array
            codeBuilder.bipush(10);
            codeBuilder.anewarray(ConstantDescs.CD_Object);
            codeBuilder.astore(1);
            // Set
            codeBuilder.aload(1);
            codeBuilder.iconst_0(); // Index
            codeBuilder.aconst_null(); // Value
            codeBuilder.aastore();
            // Get
            codeBuilder.aload(1);
            codeBuilder.iconst_1(); // Index
            codeBuilder.aaload();
            // Array length
            codeBuilder.aload(1);
            codeBuilder.arraylength();
            codeBuilder.pop();

            // Finish
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method.get());
        // We did not crash :-)
    }
}