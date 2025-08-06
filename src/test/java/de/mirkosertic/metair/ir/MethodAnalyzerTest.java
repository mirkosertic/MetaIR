package de.mirkosertic.metair.ir;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.EmptyStackException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class MethodAnalyzerTest {

    @Nested
    public class Helper {

        private MethodAnalyzer analyzer;

        @BeforeEach
        public void setup() {
            analyzer = new MethodAnalyzer();
        }

        @Test
        public void checkIllegalStateStacktrace() {
            try {
                analyzer.illegalState("illegal!");
                fail("Exception expected");
            } catch (final IllegalParsingStateException ex) {
                assertThat(ex.getMessage()).isEqualTo("illegal!");
                assertThat(ex.getAnalyzer()).isSameAs(analyzer);
                final StackTraceElement first = ex.getStackTrace()[0];
                assertThat(first.getClassName()).isEqualTo(Helper.class.getName());
                assertThat(first.getMethodName()).isEqualTo("checkIllegalStateStacktrace");
            } catch (final RuntimeException e) {
                fail("IllegalParsingStateException expected");
            }
        }

        @Test
        public void checkAssertMinimumStackSize() {
            try {
                final MethodAnalyzer.Status status = new MethodAnalyzer.Status(1);
                analyzer.assertMinimumStackSize(status, 10);
                fail("Exception expected");
            } catch (final IllegalParsingStateException ex) {
                assertThat(ex.getMessage()).isEqualTo("A minimum stack size of 10 is required, but only 0 is available!");
                assertThat(ex.getAnalyzer()).isSameAs(analyzer);
                final StackTraceElement first = ex.getStackTrace()[0];
                assertThat(first.getClassName()).isEqualTo(Helper.class.getName());
                assertThat(first.getMethodName()).isEqualTo("checkAssertMinimumStackSize");
            } catch (final RuntimeException e) {
                fail("IllegalParsingStateException expected");
            }
        }

        @Test
        public void checkAssertEmptyStack() {
            try {
                final MethodAnalyzer.Status status = new MethodAnalyzer.Status(1);
                status.push(new PrimitiveInt(10));
                analyzer.assertEmptyStack(status);
                fail("Exception expected");
            } catch (final IllegalParsingStateException ex) {
                assertThat(ex.getMessage()).isEqualTo("The stack should be empty, but it is not! It still has 1 element(s).");
                assertThat(ex.getAnalyzer()).isSameAs(analyzer);
                final StackTraceElement first = ex.getStackTrace()[0];
                assertThat(first.getClassName()).isEqualTo(Helper.class.getName());
                assertThat(first.getMethodName()).isEqualTo("checkAssertEmptyStack");
            } catch (final RuntimeException e) {
                fail("IllegalParsingStateException expected");
            }
        }
    }

    @Nested
    public class ParsingStatus {

        @Test
        public void initialStatus() {
            final MethodAnalyzer.Status status = new MethodAnalyzer.Status(3);
            assertThat(status.stack).isEmpty();
            assertThat(status.lineNumber).isEqualTo(MethodAnalyzer.Status.UNDEFINED_LINE_NUMBER);
            assertThat(status.control).isNull();
            assertThat(status.memory).isNull();
            assertThat(status.numberOfLocals()).isEqualTo(3);
            assertThat(status.getLocal(0)).isNull();
            assertThat(status.getLocal(1)).isNull();
            assertThat(status.getLocal(2)).isNull();
        }

        @Test
        public void failOnEmptyStack() {
            assertThatExceptionOfType(EmptyStackException.class).isThrownBy(() -> {
                final MethodAnalyzer.Status status = new MethodAnalyzer.Status(3);
                status.stack.pop();
            });
        }

        @Test
        public void copy() {
            final MethodAnalyzer.Status status = new MethodAnalyzer.Status(3);
            status.lineNumber = 10;
            status.memory = new LabelNode("mem");
            status.control = new LabelNode("control");
            status.setLocal(0, new PrimitiveInt(1));
            status.setLocal(1, new PrimitiveInt(2));
            status.setLocal(2, new PrimitiveInt(3));
            status.stack.push(new PrimitiveInt(10));
            status.stack.push(new PrimitiveInt(20));
            status.stack.push(new PrimitiveInt(30));
            assertThat(((PrimitiveInt) status.getLocal(0)).value).isEqualTo(1);
            assertThat(((PrimitiveInt) status.getLocal(1)).value).isEqualTo(2);
            assertThat(((PrimitiveInt) status.getLocal(2)).value).isEqualTo(3);
            assertThat(((PrimitiveInt) status.stack.get(0)).value).isEqualTo(10);
            assertThat(((PrimitiveInt) status.stack.get(1)).value).isEqualTo(20);
            assertThat(((PrimitiveInt) status.stack.get(2)).value).isEqualTo(30);

            final MethodAnalyzer.Status copy = status.copy();
            assertThat(copy.stack).hasSize(3);
            assertThat(((PrimitiveInt) status.stack.get(0)).value).isEqualTo(10);
            assertThat(((PrimitiveInt) status.stack.get(1)).value).isEqualTo(20);
            assertThat(((PrimitiveInt) status.stack.get(2)).value).isEqualTo(30);
            assertThat(copy.lineNumber).isEqualTo(10);
            assertThat(copy.memory).isSameAs(status.memory);
            assertThat(copy.control).isSameAs(status.control);
            assertThat(copy.numberOfLocals()).isEqualTo(3);
            assertThat(copy.getLocal(0)).isSameAs(status.getLocal(0));
            assertThat(copy.getLocal(1)).isSameAs(status.getLocal(1));
            assertThat(copy.getLocal(2)).isSameAs(status.getLocal(2));
        }

        @Test
        public void illegalStoreNextToEach() {
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                final MethodAnalyzer.Status copy = new MethodAnalyzer.Status(10);
                copy.setLocal(0, new PrimitiveLong(1));
                copy.setLocal(1, new PrimitiveInt(1));
            }).withMessage("Slot 0 is already set to a category 2 value, so cannot set slot 1");
        }

        @Test
        public void illegalReadNextToEach() {
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                final MethodAnalyzer.Status copy = new MethodAnalyzer.Status(10);
                copy.setLocal(0, new PrimitiveLong(1));
                copy.getLocal(1);
            }).withMessage("Slot 0 is already set to a category 2 value, so cannot read slot 1");
        }
    }

    @Nested
    public class Instructions {

        private MethodAnalyzer analyzer;

        @BeforeEach
        public void setup() {
            analyzer = new MethodAnalyzer();
        }

        @Nested
        public class UNARYOPTERATOR {

            @Test
            public void failOnEmptyStack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    assertThat(frame.in.stack).isEmpty();

                    analyzer.visitUnaryOperatorInstruction(Opcode.ARRAYLENGTH, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void arrayLength() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new NewArray(ConstantDescs.CD_int, new PrimitiveInt(10)));

                analyzer.visitUnaryOperatorInstruction(Opcode.ARRAYLENGTH, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(ArrayLength.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void arrayLength_fail() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new StringConstant("hello"));

                    analyzer.visitUnaryOperatorInstruction(Opcode.ARRAYLENGTH, frame);
                    fail("Exception expected");
                }).withMessage("Cannot get array length of non array value String : hello");
            }

            @Test
            public void neg_fail() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new StringConstant("hello"));

                    analyzer.visitUnaryOperatorInstruction(Opcode.INEG, frame);
                    fail("Exception expected");
                }).withMessage("Cannot negate non int value String : hello of type String");
            }

            @Test
            public void ineg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));

                analyzer.visitUnaryOperatorInstruction(Opcode.INEG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Negate.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void lneg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10));

                analyzer.visitUnaryOperatorInstruction(Opcode.LNEG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Negate.class).matches(t -> t.type.equals(ConstantDescs.CD_long));
            }

            @Test
            public void fneg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveFloat(10.0f));

                analyzer.visitUnaryOperatorInstruction(Opcode.FNEG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Negate.class).matches(t -> t.type.equals(ConstantDescs.CD_float));
            }

            @Test
            public void dneg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveDouble(10.0d));

                analyzer.visitUnaryOperatorInstruction(Opcode.DNEG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Negate.class).matches(t -> t.type.equals(ConstantDescs.CD_double));
            }
        }

        @Nested
        public class TYPECHECK {

            @Test
            public void failOnEmptyStack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    assertThat(frame.in.stack).isEmpty();

                    analyzer.visitTypeCheckInstruction(Opcode.CHECKCAST, ConstantDescs.CD_String, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void checkCast() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));

                analyzer.visitTypeCheckInstruction(Opcode.CHECKCAST, ConstantDescs.CD_String, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(StringConstant.class).matches(t -> t.type.equals(ConstantDescs.CD_String));
            }

            @Test
            public void instanceOf() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));

                analyzer.visitTypeCheckInstruction(Opcode.INSTANCEOF, ConstantDescs.CD_String, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(InstanceOf.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }
        }

        @Nested
        public class CONVERT {

            @Test
            public void failOnEmptyStack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    assertThat(frame.in.stack).isEmpty();

                    analyzer.visitConvertInstruction(Opcode.I2C, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void i2b() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2B, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void i2c() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2C, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void i2s() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2S, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void i2l() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_long));
            }

            @Test
            public void i2f() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2F, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_float));
            }

            @Test
            public void i2d() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.I2D, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_double));
            }

            @Test
            public void l2i() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.L2I, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void l2f() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.L2F, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_float));
            }

            @Test
            public void l2d() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.L2D, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_double));
            }

            @Test
            public void f2i() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveFloat(10.0f));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.F2I, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void f2l() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveFloat(10.0f));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.F2L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_long));
            }

            @Test
            public void f2d() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveFloat(10.0f));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.F2D, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_double));
            }

            @Test
            public void d2i() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveDouble(10.0d));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.D2I, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void d2l() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveDouble(10.0d));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.D2L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_long));
            }

            @Test
            public void d2f() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveDouble(10.0d));
                assertThat(frame.in.stack).hasSize(1);

                analyzer.visitConvertInstruction(Opcode.D2F, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isInstanceOf(Convert.class).matches(t -> t.type.equals(ConstantDescs.CD_float));
            }
        }

        @Nested
        public class CONSTANT {

            @Test
            public void nullConst() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ACONST_NULL, null, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Null.class).matches(t -> t.type.equals(ConstantDescs.CD_Object));
            }

            @Test
            public void sipush() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.SIPUSH, 10, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void bipush() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.BIPUSH, 10, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void iconst_m1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_M1, -1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == -1);
            }

            @Test
            public void iconst_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 0);
            }

            @Test
            public void iconst_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 1);
            }

            @Test
            public void iconst_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 2);
            }

            @Test
            public void iconst_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 3);
            }

            @Test
            public void iconst_4() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_4, 4, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 4);
            }

            @Test
            public void iconst_5() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.ICONST_5, 5, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 5);
            }

            @Test
            public void lconst_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.LCONST_0, 0L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveLong.class).matches(t -> ((PrimitiveLong) t).value == 0L);
            }

            @Test
            public void lconst_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.LCONST_1, 1L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveLong.class).matches(t -> ((PrimitiveLong) t).value == 1L);
            }

            @Test
            public void fconst_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.FCONST_0, 0f, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveFloat.class).matches(t -> ((PrimitiveFloat) t).value == 0f);
            }

            @Test
            public void fconst_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.FCONST_1, 1f, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveFloat.class).matches(t -> ((PrimitiveFloat) t).value == 1f);
            }

            @Test
            public void fconst_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.FCONST_2, 2f, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveFloat.class).matches(t -> ((PrimitiveFloat) t).value == 2f);
            }

            @Test
            public void dconst_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.DCONST_0, 0d, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveDouble.class).matches(t -> ((PrimitiveDouble) t).value == 0d);
            }

            @Test
            public void dconst_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.visitConstantInstruction(Opcode.DCONST_1, 1d, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveDouble.class).matches(t -> ((PrimitiveDouble) t).value == 1d);
            }
        }

        @Nested
        public class LDC {

            @Test
            public void ldcString() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC("hello", frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(StringConstant.class).matches(t -> ((StringConstant) t).value.equals("hello"));
            }

            @Test
            public void ldcInt() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC(10, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveInt.class).matches(t -> ((PrimitiveInt) t).value == 10);
            }

            @Test
            public void ldcLong() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC(10L, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveLong.class).matches(t -> ((PrimitiveLong) t).value == 10L);
            }

            @Test
            public void ldcFloat() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC(1.2f, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveFloat.class).matches(t -> ((PrimitiveFloat) t).value == 1.2f);
            }

            @Test
            public void ldcDouble() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC(1.2d, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(PrimitiveDouble.class).matches(t -> ((PrimitiveDouble) t).value == 1.2d);
            }

            @Test
            public void ldcClass() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThat(frame.in.stack).isEmpty();

                analyzer.parse_LDC(ClassDesc.of(LDC.class.getName()), frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(RuntimeclassReference.class).matches(t -> t.type.equals(ClassDesc.of(LDC.class.getName())));
            }

            @Test
            public void notImplemented() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                    analyzer.parse_LDC(null, frame);
                    fail("Exception expected");
                }).withMessage("Cannot parse LDC instruction with value null");
            }
        }

        @Nested
        public class RETURN {

            @Test
            public void correctReturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                analyzer.visitReturnInstruction(Opcode.RETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(Return.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void failWithEntryOnStack() {
                Assertions.assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.push(new PrimitiveInt(10));
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitReturnInstruction(Opcode.RETURN, frame);
                    fail("Exception expected");
                }).withMessage("The stack should be empty, but it is not! It still has 1 element(s).");
            }

            @Test
            public void ireturn_fail() {
                Assertions.assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.push(new PrimitiveLong(10L));
                    analyzer.visitReturnInstruction(Opcode.IRETURN, frame);
                    fail("Exception expected");
                }).withMessage("Expecting type int on stack, got long");
            }

            @Test
            public void ireturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitReturnInstruction(Opcode.IRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void dreturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveDouble(10d));
                analyzer.visitReturnInstruction(Opcode.DRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void freturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveFloat(10f));
                analyzer.visitReturnInstruction(Opcode.FRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void lreturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveLong(10L));
                analyzer.visitReturnInstruction(Opcode.LRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }
        }
    }
}
