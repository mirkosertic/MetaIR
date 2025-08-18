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
public class OpcodeFMULTest {

    @Test
    public void test_FMUL(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_float), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.fconst_0();
            codeBuilder.fconst_2();
            codeBuilder.fmul();
            codeBuilder.freturn();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        final MethodAnalyzer analyzer = testHelper.analyzeAndReport(model, method.get());
        assertThat(analyzer.ir().usedBy.stream().filter(t -> t instanceof PrimitiveFloat).map(t -> ((PrimitiveFloat) t).value).toList()).contains(0f, 2f);
    }
}
