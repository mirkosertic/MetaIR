package de.mirkosertic.metair.ir;

import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import de.mirkosertic.metair.ir.test.MetaIRTestHelperResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MetaIRTestHelperResolver.class)
public class OpcodeTryCatchTest {

    @Test
    public void test_SimpleCatch(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.trying(tryHandler -> {
                tryHandler.iconst_3();
                tryHandler.istore(1);
            }, catchHandler -> {
                catchHandler.catching(ClassDesc.of(IllegalArgumentException.class.getName()), CodeBuilder::pop);
            });
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }

    @Test
    public void test_SimpleCatchAll(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.trying(tryHandler -> {
                tryHandler.iconst_3();
                tryHandler.istore(1);
            }, catchHandler -> {
                catchHandler.catchingAll(CodeBuilder::pop);
            });
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }

    @Test
    public void test_CatchOfTwo(final MetaIRTestHelper testHelper) throws IOException {
        final ClassModel model = ClassModelFactory.createModelFrom(classBuilder -> classBuilder.withMethod("test", MethodTypeDesc.of(ConstantDescs.CD_void), AccessFlag.PUBLIC.mask(), methodBuilder -> methodBuilder.withCode(codeBuilder -> {
            codeBuilder.trying(tryHandler -> {
                tryHandler.iconst_3();
                tryHandler.istore(1);
            }, catchHandler -> {
                catchHandler.catching(ClassDesc.of(IllegalArgumentException.class.getName()), builder -> {
                    builder.astore(2);
                });
                catchHandler.catching(ClassDesc.of(RuntimeException.class.getName()), builder -> {
                    builder.astore(2);
                });
            });
            codeBuilder.return_();
        })));
        final Optional<MethodModel> method = model.methods().stream().filter(m -> "test".contentEquals(m.methodName())).findFirst();
        assertThat(method).isPresent();

        testHelper.analyzeAndReport(model, method.get());
    }
}
