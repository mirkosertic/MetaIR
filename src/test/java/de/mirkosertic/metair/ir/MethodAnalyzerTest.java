package de.mirkosertic.metair.ir;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
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
        public class ARRAYLOAD {

            @Test
            public void baload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_boolean, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.BALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final Extend al = (Extend) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(1);
                assertThat(al.uses.getFirst().node()).isSameAs(frame.out.control);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_int);
                assertThat(al.extendType).isEqualTo(Extend.ExtendType.SIGN);
            }

            @Test
            public void caload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_char, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.CALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final Extend al = (Extend) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(1);
                assertThat(al.uses.getFirst().node()).isSameAs(frame.out.control);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_int);
                assertThat(al.extendType).isEqualTo(Extend.ExtendType.ZERO);
            }

            @Test
            public void saload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_short, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.SALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final Extend al = (Extend) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(1);
                assertThat(al.uses.getFirst().node()).isSameAs(frame.out.control);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_int);
                assertThat(al.extendType).isEqualTo(Extend.ExtendType.SIGN);
            }

            @Test
            public void iaload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_int, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.IALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final ArrayLoad al = (ArrayLoad) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(4);
                assertThat(al.uses.get(0).node()).isSameAs(array);
                assertThat(al.uses.get(1).node()).isSameAs(index);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_int);
            }

            @Test
            public void iaload_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitArrayLoadInstruction(Opcode.IALOAD, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void laload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_long, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.LALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final ArrayLoad al = (ArrayLoad) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(4);
                assertThat(al.uses.get(0).node()).isSameAs(array);
                assertThat(al.uses.get(1).node()).isSameAs(index);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_long);
            }

            @Test
            public void faload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_float, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.FALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final ArrayLoad al = (ArrayLoad) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(4);
                assertThat(al.uses.get(0).node()).isSameAs(array);
                assertThat(al.uses.get(1).node()).isSameAs(index);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_float);
            }

            @Test
            public void daload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_double, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.DALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final ArrayLoad al = (ArrayLoad) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(4);
                assertThat(al.uses.get(0).node()).isSameAs(array);
                assertThat(al.uses.get(1).node()).isSameAs(index);
                assertThat(al.type).isEqualTo(ConstantDescs.CD_double);
            }

            @Test
            public void aaload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_Object, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);

                frame.in.push(array);
                frame.in.push(index);

                analyzer.visitArrayLoadInstruction(Opcode.AALOAD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ArrayLoad.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayLoad.class);

                final ArrayLoad al = (ArrayLoad) frame.out.stack.getFirst();
                assertThat(al.uses).hasSize(4);
                assertThat(al.uses.get(0).node()).isSameAs(array);
                assertThat(al.uses.get(1).node()).isSameAs(index);
            }

            @Test
            public void aaload_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitArrayLoadInstruction(Opcode.AALOAD, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }
        }

        @Nested
        public class ARRAYSTORE {

            @Test
            public void aastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_Object, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveInt(10);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.AASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isSameAs(value);
            }

            @Test
            public void aastore_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitArrayStoreInstruction(Opcode.AASTORE, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 3 is required, but only 0 is available!");
            }

            @Test
            public void castore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_char, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveInt(10);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.CASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isInstanceOf(Truncate.class);
            }

            @Test
            public void castore_fail_wrongvalue() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    final Value array = new NewArray(ConstantDescs.CD_char, new PrimitiveInt(100));
                    final Value index = new PrimitiveInt(1);
                    final Value value = new PrimitiveLong(10);

                    frame.in.push(array);
                    frame.in.push(index);
                    frame.in.push(value);

                    analyzer.visitArrayStoreInstruction(Opcode.CASTORE, frame);
                    fail("Exception expected");
                }).withMessage("Expected value of type int for CASTORE");
            }

            @Test
            public void bastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_boolean, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveInt(10);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.BASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isInstanceOf(Truncate.class);
            }

            @Test
            public void sastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_short, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveInt(10);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.SASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isInstanceOf(Truncate.class);
            }

            @Test
            public void iastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_int, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveInt(10);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.IASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isSameAs(value);
            }

            @Test
            public void iastore_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitArrayStoreInstruction(Opcode.IASTORE, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 3 is required, but only 0 is available!");
            }

            @Test
            public void lastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_long, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveLong(10L);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.LASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isSameAs(value);
            }

            @Test
            public void fastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_float, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveFloat(10.0F);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.FASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isSameAs(value);
            }

            @Test
            public void dastore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value array = new NewArray(ConstantDescs.CD_double, new PrimitiveInt(100));
                final Value index = new PrimitiveInt(1);
                final Value value = new PrimitiveDouble(10.0d);

                frame.in.push(array);
                frame.in.push(index);
                frame.in.push(value);

                analyzer.visitArrayStoreInstruction(Opcode.DASTORE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ArrayStore.class);
                assertThat(frame.out.memory).isInstanceOf(ArrayStore.class);

                final ArrayStore as = (ArrayStore) frame.out.memory;
                assertThat(as.uses).hasSize(5);
                assertThat(as.uses.get(0).node()).isSameAs(array);
                assertThat(as.uses.get(1).node()).isSameAs(index);
                assertThat(as.uses.get(2).node()).isSameAs(value);
            }

        }

        @Nested
        public class INVOCATION {

            @Test
            public void invokestatic() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));

                analyzer.visitInvokeInstruction(Opcode.INVOKESTATIC, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isSameAs(frame.out.stack.getFirst());
                assertThat(frame.out.memory).isSameAs(frame.out.stack.getFirst());

                assertThat(frame.out.stack.getFirst()).isInstanceOf(InvocationStatic.class);
            }

            @Test
            public void invokespecial() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new StringConstant("hello"));
                frame.in.push(new PrimitiveInt(10));

                analyzer.visitInvokeInstruction(Opcode.INVOKESPECIAL, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isSameAs(frame.out.stack.getFirst());
                assertThat(frame.out.memory).isSameAs(frame.out.stack.getFirst());

                assertThat(frame.out.stack.getFirst()).isInstanceOf(InvocationSpecial.class);
            }

            @Test
            public void invokespecial_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitInvokeInstruction(Opcode.INVOKESPECIAL, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void invokevirtual() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new StringConstant("hello"));
                frame.in.push(new PrimitiveInt(10));

                analyzer.visitInvokeInstruction(Opcode.INVOKEVIRTUAL, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isSameAs(frame.out.stack.getFirst());
                assertThat(frame.out.memory).isSameAs(frame.out.stack.getFirst());

                assertThat(frame.out.stack.getFirst()).isInstanceOf(InvocationVirtual.class);
            }

            @Test
            public void invokesvirtual_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitInvokeInstruction(Opcode.INVOKEVIRTUAL, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void invokeinterface() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new StringConstant("hello"));
                frame.in.push(new PrimitiveInt(10));

                analyzer.visitInvokeInstruction(Opcode.INVOKEINTERFACE, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isSameAs(frame.out.stack.getFirst());
                assertThat(frame.out.memory).isSameAs(frame.out.stack.getFirst());

                assertThat(frame.out.stack.getFirst()).isInstanceOf(InvocationInterface.class);
            }

            @Test
            public void invokeinterface_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitInvokeInstruction(Opcode.INVOKEINTERFACE, ConstantDescs.CD_String, "name", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int), frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }
        }

        @Nested
        public class IINC {

            @Test
            public void iinc() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new PrimitiveInt(10));

                analyzer.parse_IINC(1, 10, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.getLocal(1)).isInstanceOf(Add.class);
            }

            @Test
            public void iinc_fail_null() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.parse_IINC(1, 10, frame);
                    fail("Exception expected");
                }).withMessage("No local value for slot 1");
            }

            @Test
            public void iinc_fail_wrongtype() {
                assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.setLocal(1, new PrimitiveDouble(10.0d));

                    analyzer.parse_IINC(1, 10, frame);
                    fail("Exception expected");
                }).withMessage("Cannot add non int value double for arg1");
            }

        }

        @Nested
        public class FIELD {

            @Test
            public void putstatic() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);

                analyzer.visitFieldInstruction(Opcode.PUTSTATIC, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(PutStatic.class);
                assertThat(frame.out.memory).isInstanceOf(PutStatic.class);

                final PutStatic put = (PutStatic) frame.out.memory;
                assertThat(put.uses).hasSize(4);
                assertThat(put.uses.get(1).node()).isSameAs(value);
                assertThat(put.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void putstatic_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitFieldInstruction(Opcode.PUTSTATIC, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void putfield() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value target = new StringConstant("target");
                final Value value = new PrimitiveInt(10);

                frame.in.push(target);
                frame.in.push(value);

                analyzer.visitFieldInstruction(Opcode.PUTFIELD, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(PutField.class);
                assertThat(frame.out.memory).isInstanceOf(PutField.class);

                final PutField put = (PutField) frame.out.memory;
                assertThat(put.uses).hasSize(4);
                assertThat(put.uses.get(0).node()).isSameAs(target);
                assertThat(put.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(put.uses.get(1).node()).isSameAs(value);
                assertThat(put.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void putfield_fail_stacksize() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitFieldInstruction(Opcode.PUTFIELD, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void getfield() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new StringConstant("hello"));
                analyzer.visitFieldInstruction(Opcode.GETFIELD, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_int);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);

                assertThat(node).isInstanceOf(GetField.class).matches(t -> ((GetField) t).fieldName.equals("fieldname"));
                assertThat(node.uses.size()).isEqualTo(2);
            }

            @Test
            public void getfield_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitFieldInstruction(Opcode.GETFIELD, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void getstatic() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                analyzer.visitFieldInstruction(Opcode.GETSTATIC, ConstantDescs.CD_String, ConstantDescs.CD_int, "fieldname", frame);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_int);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.control).isInstanceOf(ClassInitialization.class);
                assertThat(frame.out.memory).isSameAs(node);

                assertThat(node).isInstanceOf(GetStatic.class).matches(t -> ((GetStatic) t).fieldName.equals("fieldname"));
                assertThat(node.uses.size()).isEqualTo(2);
            }
        }

        @Nested
        public class THROW {

            @Test
            public void athrow() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new StringConstant("hello"));
                analyzer.visitThrowInstruction(frame);

                final Node node = frame.out.control;

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isSameAs(node);
                assertThat(frame.out.memory).isSameAs(node);

                assertThat(node).isInstanceOf(Throw.class);
                assertThat(node.uses.size()).isEqualTo(3);
            }

            @Test
            public void athrow_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitThrowInstruction(frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }
        }

        @Nested
        public class MULTIARRAY {

            @Test
            public void multiarray_dim1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewMultiArray(ConstantDescs.CD_Object.arrayType(), 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_Object.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void multiarray_dim2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewMultiArray(ConstantDescs.CD_Object.arrayType().arrayType(), 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_Object.arrayType().arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void multiarray_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitNewMultiArray(ConstantDescs.CD_Object, 1, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }
        }

        @Nested
        public class ARRAY {

            @Test
            public void newObjectArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewObjectArray(ConstantDescs.CD_Object, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_Object.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newObjectArray_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitNewObjectArray(ConstantDescs.CD_Object, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void newByteArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.BYTE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_byte.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newByteArray_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitNewPrimitiveArray(TypeKind.BYTE, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void newCharArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.CHAR, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_char.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newShortArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.SHORT, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_short.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newBooleanArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.BOOLEAN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_boolean.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newIntArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.INT, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_int.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newLongArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.LONG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_long.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newFloatArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.FLOAT, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_float.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

            @Test
            public void newDoubleArray() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                analyzer.visitNewPrimitiveArray(TypeKind.DOUBLE, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final Value node = frame.out.stack.getFirst();
                assertThat(node.type).isEqualTo(ConstantDescs.CD_double.arrayType());

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(node);
            }

        }

        @Nested
        public class NOP {

            @Test
            public void nop() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                analyzer.visitNopInstruction(frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }
        }

        @Nested
        public class STORE {

            @Test
            public void astore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new StringConstant("helloe");

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ASTORE, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void astore_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStoreInstruction(Opcode.ASTORE, 0, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void astore_fail_wrongtype() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.push(new PrimitiveLong(10L));

                    analyzer.visitStoreInstruction(Opcode.ASTORE, 0, frame);
                    fail("Exception expected");
                }).withMessage("Cannot store primitive value long");
            }

            @Test
            public void astore_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new StringConstant("hello");

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ASTORE_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void astore_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new StringConstant("hello");

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ASTORE_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(1)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void astore_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new StringConstant("hello");

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ASTORE_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(2)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void astore_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new StringConstant("hello");

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ASTORE_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(3)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStoreInstruction(Opcode.ISTORE, 0, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void istore_fail_wrongtype() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.push(new PrimitiveLong(10L));

                    analyzer.visitStoreInstruction(Opcode.ISTORE, 0, frame);
                    fail("Exception expected");
                }).withMessage("Cannot store non int value long for slot 0");
            }

            @Test
            public void istore_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(1)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(2)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void istore_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveInt(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.ISTORE_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(3)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10L);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10L);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10L);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10L);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(1)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10L);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(2)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lstore_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveLong(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.LSTORE_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(3)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10.0f);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10.0f);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10.0F);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10.0F);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(1)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10.0F);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(2)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fstore_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveFloat(10);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.FSTORE_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(3)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0d);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0d);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0D);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(0)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0D);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(1)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0D);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(2)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dstore_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value = new PrimitiveDouble(10.0D);

                frame.in.push(value);
                analyzer.visitStoreInstruction(Opcode.DSTORE_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.getLocal(3)).isSameAs(value);

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }
        }

        @Nested
        public class LOAD {

            @Test
            public void iload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void iload_fail_wrong_type() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.setLocal(0, new PrimitiveInt(10));

                    analyzer.visitLoadInstruction(Opcode.FLOAD, 0, frame);
                    fail("Exception expected");
                }).withMessage("Cannot load int from slot 0 as float is expected!");
            }

            @Test
            public void iload_fail_null() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitLoadInstruction(Opcode.FLOAD, 0, frame);
                    fail("Exception expected");
                }).withMessage("Slot 0 is null");
            }

            @Test
            public void iload_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void iload_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void iload_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(1));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void iload_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(2, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(2));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void iload_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(3, new PrimitiveInt(10));

                analyzer.visitLoadInstruction(Opcode.ILOAD_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(3));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveLong(10L));

                analyzer.visitLoadInstruction(Opcode.LLOAD, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveLong(10L));

                analyzer.visitLoadInstruction(Opcode.LLOAD_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveLong(10L));

                analyzer.visitLoadInstruction(Opcode.LLOAD_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new PrimitiveLong(10L));

                analyzer.visitLoadInstruction(Opcode.LLOAD_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(1));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(2, new PrimitiveLong(10L));

                analyzer.visitLoadInstruction(Opcode.LLOAD_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(2));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void lload_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(3, new PrimitiveLong(10));

                analyzer.visitLoadInstruction(Opcode.LLOAD_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(3));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(1));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(2, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(2));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void fload_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(3, new PrimitiveFloat(10.0f));

                analyzer.visitLoadInstruction(Opcode.FLOAD_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(3));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload_w() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD_W, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(1));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(2, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(2));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void dload_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(3, new PrimitiveDouble(10.0d));

                analyzer.visitLoadInstruction(Opcode.DLOAD_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(3));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void aload() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new StringConstant("hello"));

                analyzer.visitLoadInstruction(Opcode.ALOAD, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void aload_fail_primitive() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.setLocal(0, new PrimitiveInt(10));

                    analyzer.visitLoadInstruction(Opcode.ALOAD, 0, frame);
                    fail("Exception expected");
                }).withMessage("Cannot load primitive value int for slot 0");
            }

            @Test
            public void aload_fail_null() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(10);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitLoadInstruction(Opcode.ALOAD, 0, frame);
                    fail("Exception expected");
                }).withMessage("Slot 0 is null");
            }

            @Test
            public void aload_0() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(0, new StringConstant("hello"));

                analyzer.visitLoadInstruction(Opcode.ALOAD_0, 0, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(0));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void aload_1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(1, new StringConstant("hello"));

                analyzer.visitLoadInstruction(Opcode.ALOAD_1, 1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(1));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void aload_2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(2, new StringConstant("hello"));

                analyzer.visitLoadInstruction(Opcode.ALOAD_2, 2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(2));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

            @Test
            public void aload_3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(10);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.setLocal(3, new StringConstant("hello"));

                analyzer.visitLoadInstruction(Opcode.ALOAD_3, 3, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                assertThat(frame.out.stack.getFirst()).isSameAs(frame.in.getLocal(3));

                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);
            }

        }

        @Nested
        public class NEWOBJECT {

            @Test
            public void new_() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                analyzer.visitNewObjectInstruction(Opcode.NEW, ConstantDescs.CD_Object, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);

                final New n = (New) frame.out.stack.getFirst();
                assertThat(n.type).isEqualTo(ConstantDescs.CD_Object);
                assertThat(n.uses).hasSize(2);

                assertThat(frame.out.control).isInstanceOf(ClassInitialization.class);
                assertThat(frame.out.memory).isSameAs(n);
            }
        }

        @Nested
        public class STACK {

            @Test
            public void dup2_x2_form4() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10));
                frame.in.stack.push(new PrimitiveLong(20L));

                analyzer.visitStackInstruction(Opcode.DUP2_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(3);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
            }

            @Test
            public void dup2_x2_form3() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10L));
                frame.in.stack.push(new PrimitiveInt(20));
                frame.in.stack.push(new PrimitiveInt(30));

                analyzer.visitStackInstruction(Opcode.DUP2_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(5);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(3)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(4)).isSameAs(frame.in.stack.get(2));
            }

            @Test
            public void dup2_x2_form2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(20));
                frame.in.stack.push(new PrimitiveLong(30L));

                analyzer.visitStackInstruction(Opcode.DUP2_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(4);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(3)).isSameAs(frame.in.stack.get(2));
            }

            @Test
            public void dup2_x2_form1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(20));
                frame.in.stack.push(new PrimitiveInt(30));
                frame.in.stack.push(new PrimitiveInt(40));

                analyzer.visitStackInstruction(Opcode.DUP2_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(6);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(3));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(3)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(4)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(5)).isSameAs(frame.in.stack.get(3));
            }

            @Test
            public void dup2_x1_form2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveLong(20L));

                analyzer.visitStackInstruction(Opcode.DUP2_X1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(3);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
            }

            @Test
            public void dup2_x1_form2_fail_wrongstack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveInt(10));

                    analyzer.visitStackInstruction(Opcode.DUP2_X1, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 1 is available!");
            }

            @Test
            public void dup2_x1_form1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(20));
                frame.in.stack.push(new PrimitiveInt(30));

                analyzer.visitStackInstruction(Opcode.DUP2_X1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(5);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.getFirst());
            }

            @Test
            public void dup2_x1_form1_fail_wrongstack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveInt(10));
                    frame.in.stack.push(new PrimitiveInt(20));

                    analyzer.visitStackInstruction(Opcode.DUP2_X1, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void dup_x2_form2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(20L));
                frame.in.stack.push(new PrimitiveInt(10));

                analyzer.visitStackInstruction(Opcode.DUP_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(3);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
            }

            @Test
            public void dup_x2_form2_fail_wrongstack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveLong(20L));

                    analyzer.visitStackInstruction(Opcode.DUP_X2, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 1 is available!");
            }

            @Test
            public void dup_x2_form1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(20));
                frame.in.stack.push(new PrimitiveInt(30));

                analyzer.visitStackInstruction(Opcode.DUP_X2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(4);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(2));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(3)).isSameAs(frame.in.stack.get(2));
            }

            @Test
            public void dup_x2_form1_fail_wrongstack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveInt(10));
                    frame.in.stack.push(new PrimitiveInt(20));

                    analyzer.visitStackInstruction(Opcode.DUP_X2, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void dup_x1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10L));
                frame.in.stack.push(new PrimitiveLong(10L));

                analyzer.visitStackInstruction(Opcode.DUP_X1, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(3);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(1));
            }

            @Test
            public void dup_x1_fail_stacksize() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveLong(10L));

                    analyzer.visitStackInstruction(Opcode.DUP_X1, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 1 is available!");
            }

            @Test
            public void dup2_form2() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10L));

                analyzer.visitStackInstruction(Opcode.DUP2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(2);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.getFirst());
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.getFirst());
            }

            @Test
            public void dup2_form1() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(20));

                analyzer.visitStackInstruction(Opcode.DUP2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(4);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(2)).isSameAs(frame.in.stack.get(0));
                assertThat(frame.out.stack.get(3)).isSameAs(frame.in.stack.get(1));
            }

            @Test
            public void dup2_form1_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveInt(10));

                    analyzer.visitStackInstruction(Opcode.DUP2, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void swap() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                frame.in.stack.push(new StringConstant("hello"));
                analyzer.visitStackInstruction(Opcode.SWAP, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(2);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.get(1));
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.get(0));
            }

            @Test
            public void swap_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStackInstruction(Opcode.SWAP, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void dup() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                analyzer.visitStackInstruction(Opcode.DUP, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).hasSize(2);
                assertThat(frame.out.stack.get(0)).isSameAs(frame.in.stack.getFirst());
                assertThat(frame.out.stack.get(1)).isSameAs(frame.in.stack.getFirst());
            }

            @Test
            public void dup_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStackInstruction(Opcode.DUP, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void pop() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                analyzer.visitStackInstruction(Opcode.POP, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).isEmpty();
            }

            @Test
            public void pop_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStackInstruction(Opcode.POP, frame);
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void pop2_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitStackInstruction(Opcode.POP2, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void pop2_long() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveLong(10L));
                analyzer.visitStackInstruction(Opcode.POP2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).isEmpty();
            }

            @Test
            public void pop2_int() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));
                frame.in.stack.push(new PrimitiveInt(10));
                analyzer.visitStackInstruction(Opcode.POP2, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.control).isSameAs(frame.in.control);
                assertThat(frame.out.memory).isSameAs(frame.in.memory);

                assertThat(frame.out.stack).isEmpty();
            }

            @Test
            public void pop2_int_fail() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.stack.push(new PrimitiveInt(10));
                    analyzer.visitStackInstruction(Opcode.POP2, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }
        }


        @Nested
        public class MONITOR {

            @Test
            public void monitorEnter() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                analyzer.visitMonitorInstruction(Opcode.MONITORENTER, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(MonitorEnter.class);

                final Node node = frame.out.control;
                assertThat(node.uses).hasSize(2);
                assertThat(node.uses.get(0).node()).isSameAs(frame.in.stack.getFirst());
                assertThat(node.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(node.uses.get(1).node()).isSameAs(frame.in.control);
            }

            @Test
            public void monitorEnter_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitMonitorInstruction(Opcode.MONITORENTER, frame);
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }

            @Test
            public void monitorExit() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                analyzer.visitMonitorInstruction(Opcode.MONITOREXIT, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(MonitorExit.class);

                final Node node = frame.out.control;
                assertThat(node.uses).hasSize(2);
                assertThat(node.uses.get(0).node()).isSameAs(frame.in.stack.getFirst());
                assertThat(node.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(node.uses.get(1).node()).isSameAs(frame.in.control);
            }

            @Test
            public void monitorExit_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitMonitorInstruction(Opcode.MONITOREXIT, frame);
                }).withMessage("A minimum stack size of 1 is required, but only 0 is available!");
            }
        }

        @Nested
        public class TYPECHECK {

            @Test
            public void checkcast_fail_emptystack() {
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
            public void i2c_fail_emptystack() {
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
            public void aconst_null() {
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
            public void ldc_String() {
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
            public void ldc_int() {
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
            public void ldc_long() {
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
            public void ldc_float() {
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
            public void ldc_double() {
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
            public void ldc_Class() {
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
        public class OPERATOR {

            @Test
            public void fcmpg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveFloat(10.0f);
                final Value value2 = new PrimitiveFloat(10.0f);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.FCMPG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(NumericCompare.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final NumericCompare op = (NumericCompare) frame.out.stack.getFirst();
                assertThat(op.compareType).isEqualTo(ConstantDescs.CD_float);
                assertThat(op.mode).isEqualTo(NumericCompare.Mode.NAN_IS_1);
                assertThat(op.uses).hasSize(2);
                assertThat(op.uses.get(0).node()).isEqualTo(value1);
                assertThat(op.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(op.uses.get(1).node()).isEqualTo(value2);
                assertThat(op.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void fcmpg_fail_emmptystack() {
                assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitOperatorInstruction(Opcode.FCMPG, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void fcmpl() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveFloat(10.0f);
                final Value value2 = new PrimitiveFloat(10.0f);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.FCMPL, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(NumericCompare.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final NumericCompare op = (NumericCompare) frame.out.stack.getFirst();
                assertThat(op.compareType).isEqualTo(ConstantDescs.CD_float);
                assertThat(op.mode).isEqualTo(NumericCompare.Mode.NAN_IS_MINUS_1);
                assertThat(op.uses).hasSize(2);
                assertThat(op.uses.get(0).node()).isEqualTo(value1);
                assertThat(op.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(op.uses.get(1).node()).isEqualTo(value2);
                assertThat(op.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void dcmpg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveDouble(10.0d);
                final Value value2 = new PrimitiveDouble(10.0d);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.DCMPG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(NumericCompare.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final NumericCompare op = (NumericCompare) frame.out.stack.getFirst();
                assertThat(op.compareType).isEqualTo(ConstantDescs.CD_double);
                assertThat(op.mode).isEqualTo(NumericCompare.Mode.NAN_IS_1);
                assertThat(op.uses).hasSize(2);
                assertThat(op.uses.get(0).node()).isEqualTo(value1);
                assertThat(op.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(op.uses.get(1).node()).isEqualTo(value2);
                assertThat(op.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void dcmpl() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveDouble(10.0d);
                final Value value2 = new PrimitiveDouble(10.0d);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.DCMPL, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(NumericCompare.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final NumericCompare op = (NumericCompare) frame.out.stack.getFirst();
                assertThat(op.compareType).isEqualTo(ConstantDescs.CD_double);
                assertThat(op.mode).isEqualTo(NumericCompare.Mode.NAN_IS_MINUS_1);
                assertThat(op.uses).hasSize(2);
                assertThat(op.uses.get(0).node()).isEqualTo(value1);
                assertThat(op.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(op.uses.get(1).node()).isEqualTo(value2);
                assertThat(op.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void lcmp() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveLong(10L);
                final Value value2 = new PrimitiveLong(10L);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.LCMP, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(NumericCompare.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final NumericCompare op = (NumericCompare) frame.out.stack.getFirst();
                assertThat(op.compareType).isEqualTo(ConstantDescs.CD_long);
                assertThat(op.mode).isEqualTo(NumericCompare.Mode.NONFLOATINGPOINT);
                assertThat(op.uses).hasSize(2);
                assertThat(op.uses.get(0).node()).isEqualTo(value1);
                assertThat(op.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(op.uses.get(1).node()).isEqualTo(value2);
                assertThat(op.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void arraylength_fail_emptystack() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    assertThat(frame.in.stack).isEmpty();

                    analyzer.visitOperatorInstruction(Opcode.ARRAYLENGTH, frame);
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

                analyzer.visitOperatorInstruction(Opcode.ARRAYLENGTH, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(ArrayLength.class).matches(t -> t.type.equals(ConstantDescs.CD_int));
            }

            @Test
            public void ineg() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new PrimitiveInt(10));

                analyzer.visitOperatorInstruction(Opcode.INEG, frame);

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

                analyzer.visitOperatorInstruction(Opcode.LNEG, frame);

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

                analyzer.visitOperatorInstruction(Opcode.FNEG, frame);

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

                analyzer.visitOperatorInstruction(Opcode.DNEG, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Negate.class).matches(t -> t.type.equals(ConstantDescs.CD_double));
            }

            @Test
            public void isub() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveInt(10);
                final Value value2 = new PrimitiveInt(10);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.ISUB, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Sub.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final Sub sub = (Sub) frame.out.stack.getFirst();
                assertThat(sub.uses).hasSize(2);
                assertThat(sub.uses.get(0).node()).isEqualTo(value1);
                assertThat(sub.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(sub.uses.get(1).node()).isEqualTo(value2);
                assertThat(sub.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void isub_fail_emptystack() {
                assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitOperatorInstruction(Opcode.ISUB, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void lsub() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveLong(10L);
                final Value value2 = new PrimitiveLong(10L);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.LSUB, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Sub.class).matches(t -> t.type.equals(ConstantDescs.CD_long));

                final Sub sub = (Sub) frame.out.stack.getFirst();
                assertThat(sub.uses).hasSize(2);
                assertThat(sub.uses.get(0).node()).isEqualTo(value1);
                assertThat(sub.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(sub.uses.get(1).node()).isEqualTo(value2);
                assertThat(sub.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void fsub() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveFloat(10.0F);
                final Value value2 = new PrimitiveFloat(10.0F);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.FSUB, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Sub.class).matches(t -> t.type.equals(ConstantDescs.CD_float));

                final Sub sub = (Sub) frame.out.stack.getFirst();
                assertThat(sub.uses).hasSize(2);
                assertThat(sub.uses.get(0).node()).isEqualTo(value1);
                assertThat(sub.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(sub.uses.get(1).node()).isEqualTo(value2);
                assertThat(sub.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void dsub() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveDouble(10.0D);
                final Value value2 = new PrimitiveDouble(10.0D);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.DSUB, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Sub.class).matches(t -> t.type.equals(ConstantDescs.CD_double));

                final Sub sub = (Sub) frame.out.stack.getFirst();
                assertThat(sub.uses).hasSize(2);
                assertThat(sub.uses.get(0).node()).isEqualTo(value1);
                assertThat(sub.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(sub.uses.get(1).node()).isEqualTo(value2);
                assertThat(sub.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void iadd() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveInt(10);
                final Value value2 = new PrimitiveInt(10);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.IADD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Add.class).matches(t -> t.type.equals(ConstantDescs.CD_int));

                final Add add = (Add) frame.out.stack.getFirst();
                assertThat(add.uses).hasSize(2);
                assertThat(add.uses.get(0).node()).isEqualTo(value1);
                assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(add.uses.get(1).node()).isEqualTo(value2);
                assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void iadd_fail_emptystack() {
                assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitOperatorInstruction(Opcode.IADD, frame);
                    fail("Exception expected");
                }).withMessage("A minimum stack size of 2 is required, but only 0 is available!");
            }

            @Test
            public void ladd() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveLong(10L);
                final Value value2 = new PrimitiveLong(10L);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.LADD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Add.class).matches(t -> t.type.equals(ConstantDescs.CD_long));

                final Add add = (Add) frame.out.stack.getFirst();
                assertThat(add.uses).hasSize(2);
                assertThat(add.uses.get(0).node()).isEqualTo(value1);
                assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(add.uses.get(1).node()).isEqualTo(value2);
                assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void fadd() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveFloat(10.0F);
                final Value value2 = new PrimitiveFloat(10.0F);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.FADD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Add.class).matches(t -> t.type.equals(ConstantDescs.CD_float));

                final Add add = (Add) frame.out.stack.getFirst();
                assertThat(add.uses).hasSize(2);
                assertThat(add.uses.get(0).node()).isEqualTo(value1);
                assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(add.uses.get(1).node()).isEqualTo(value2);
                assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }

            @Test
            public void dadd() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                final Value value1 = new PrimitiveDouble(10.0D);
                final Value value2 = new PrimitiveDouble(10.0D);

                frame.in.stack.push(value1); // Value 1
                frame.in.stack.push(value2); // Value 2

                analyzer.visitOperatorInstruction(Opcode.DADD, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).hasSize(1);
                assertThat(frame.out.stack.getFirst()).isInstanceOf(Add.class).matches(t -> t.type.equals(ConstantDescs.CD_double));

                final Add add = (Add) frame.out.stack.getFirst();
                assertThat(add.uses).hasSize(2);
                assertThat(add.uses.get(0).node()).isEqualTo(value1);
                assertThat(add.uses.get(0).use()).isEqualTo(new ArgumentUse(0));
                assertThat(add.uses.get(1).node()).isEqualTo(value2);
                assertThat(add.uses.get(1).use()).isEqualTo(new ArgumentUse(1));
            }
        }

        @Nested
        public class RETURN {

            @Test
            public void areturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.stack.push(new StringConstant("hello"));
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_String)).visitReturnInstruction(Opcode.ARETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void areturn_fail_empty_stack() {
                assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_String)).visitReturnInstruction(Opcode.ARETURN, frame);
                    fail("Exception expected");
                }).withMessage("Expecting only one value on the stack");
            }

            @Test
            public void return_() {
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
            public void return_fail_emptystack() {
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
            public void ireturn_fail_emptystack() {
                Assertions.assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    analyzer.visitReturnInstruction(Opcode.IRETURN, frame);
                    fail("Exception expected");
                }).withMessage("Expecting only one value on the stack");
            }

            @Test
            public void ireturn() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_int)).visitReturnInstruction(Opcode.IRETURN, frame);

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
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_double)).visitReturnInstruction(Opcode.DRETURN, frame);

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
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_float)).visitReturnInstruction(Opcode.FRETURN, frame);

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
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_long)).visitReturnInstruction(Opcode.LRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void ireturn_with_truncation() {
                final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                frame.in = new MethodAnalyzer.Status(0);
                frame.in.control = new LabelNode("control");
                frame.in.memory = new LabelNode("memory");

                frame.in.push(new PrimitiveInt(10));
                new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_boolean)).visitReturnInstruction(Opcode.IRETURN, frame);

                assertThat(frame.out).isNotNull().isNotSameAs(frame.in);
                assertThat(frame.out.stack).isEmpty();
                assertThat(frame.out.control).isInstanceOf(ReturnValue.class);
                assertThat(frame.in.control.usedBy).containsExactly(frame.out.control);
            }

            @Test
            public void ireturn_with_truncation_fail_wrongtype() {
                assertThatExceptionOfType(IllegalParsingStateException.class).isThrownBy(() -> {
                    final MethodAnalyzer.Frame frame = new MethodAnalyzer.Frame(0, null);
                    frame.in = new MethodAnalyzer.Status(0);
                    frame.in.control = new LabelNode("control");
                    frame.in.memory = new LabelNode("memory");

                    frame.in.push(new PrimitiveLong(10));
                    new MethodAnalyzer(MethodTypeDesc.of(ConstantDescs.CD_boolean)).visitReturnInstruction(Opcode.IRETURN, frame);
                    fail("Exception expected");
                }).withMessage("Cannot return non int value long as int is expected for truncation to boolean");
            }
        }
    }
}
