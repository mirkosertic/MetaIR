package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.Add;
import de.mirkosertic.metair.ir.PrimitiveInt;
import org.junit.jupiter.api.Test;

import java.lang.constant.ConstantDescs;

import static org.assertj.core.api.Assertions.assertThat;

class YAMLStructuredControlflowCodeGeneratorTest {

    @Test
    public void test_evaluate_PrimitiveInteger() {
        final YAMLStructuredControlflowCodeGenerator generator = new YAMLStructuredControlflowCodeGenerator();

        generator.emit(new PrimitiveInt(10));
        final String result = generator.toString();
        System.out.println(result);
        assertThat(result).isEqualTo("10");
    }

    @Test
    public void test_evaluate_add_PrimitiveInteger() {
        final YAMLStructuredControlflowCodeGenerator generator = new YAMLStructuredControlflowCodeGenerator();

        generator.emit(new Add(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveInt(20)));
        final String result = generator.toString();
        System.out.println(result);
        assertThat(result).isEqualTo("(10 + 20)");
    }

    @Test
    public void test_evaluate_add_PrimitiveInteger_nested() {
        final YAMLStructuredControlflowCodeGenerator generator = new YAMLStructuredControlflowCodeGenerator();

        generator.emit(new Add(ConstantDescs.CD_int, new PrimitiveInt(10), new Add(ConstantDescs.CD_int, new PrimitiveInt(20), new PrimitiveInt(30))));
        final String result = generator.toString();
        System.out.println(result);
        assertThat(result).isEqualTo("(10 + (20 + 30))");
    }

    @Test
    public void test_evaluate_add_PrimitiveInteger_nested2() {
        final YAMLStructuredControlflowCodeGenerator generator = new YAMLStructuredControlflowCodeGenerator();

        final Add nestedAdd = new Add(ConstantDescs.CD_int, new PrimitiveInt(10), new PrimitiveInt(20));
        generator.emit(new Add(ConstantDescs.CD_int, nestedAdd, nestedAdd));
        final String result = generator.toString();
        System.out.println(result);
        assertThat(result).isEqualTo("int var0 = (10 + 20)\n" +
                "(var0 + var0)");
    }
}