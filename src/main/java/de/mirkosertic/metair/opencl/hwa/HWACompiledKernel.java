package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.ir.CFGDominatorTree;
import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.IRType;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import de.mirkosertic.metair.ir.ResolvedClass;
import de.mirkosertic.metair.ir.ResolvedField;
import de.mirkosertic.metair.ir.ResolvedMethod;
import de.mirkosertic.metair.ir.ResolverContext;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.opencl.OpenCL;
import de.mirkosertic.metair.opencl.api.DeviceProperties;
import de.mirkosertic.metair.opencl.api.FloatSerializable;
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.OpenCLType;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HWACompiledKernel {

    private static final String KERNEL_METHOD_NAME = "processWorkItem";

    public record KernelArgumentSharedDataDesc(long size, MemorySegment pointer, HWAKernelArgument argument, Object value, MemorySegment clBuffer, IRType arrayElementType) {

    }

    private ResolvedMethod resolvedKernelMethod;
    private MethodAnalyzer analyzer;
    private final List<HWAKernelArgument> arguments;
    private final MemorySegment clContext;
    private final MemorySegment clCommandQueue;
    private final MemorySegment clProgram;
    private final MemorySegment clKernel;
    private final OpenCL openCL;
    private final Kernel kernel;
    private final DeviceProperties deviceProperties;
    private final ResolverContext resolverContext;

    public HWACompiledKernel(final OpenCL openCL, final DeviceProperties deviceProperties, final Kernel kernel, final Arena arena, final MemorySegment clContext, final MemorySegment clCommandQueue) {

        this.kernel = kernel;
        this.openCL = openCL;
        this.clContext = clContext;
        this.clCommandQueue = clCommandQueue;
        this.deviceProperties = deviceProperties;

        final Class<?> origin = kernel.getClass();

        this.resolverContext = new ResolverContext();
        final ResolvedClass resolvedKernelClass = resolverContext.resolveClass(origin.getName());

        final List<ResolvedMethod> kernelMethods = new ArrayList<>();

        final ClassModel model = resolvedKernelClass.classModel();
        // We resolve all methods of the kernel claas, but keep also
        // track of the "processWorkItem" main method"
        for (final MethodModel method : model.methods()) {

            // TODO: Implement better heurisric here
            if (KERNEL_METHOD_NAME.equals(method.methodName().stringValue()) || method.flags().has(AccessFlag.PRIVATE)) {

                final ResolvedMethod m = resolvedKernelClass.resolveMethod(method);
                final MethodAnalyzer ma = m.analyze();

                kernelMethods.add(m);

                if (KERNEL_METHOD_NAME.equals(method.methodName().stringValue())) {
                    resolvedKernelMethod = m;
                    analyzer = ma;
                }
            }
        }

        if (resolvedKernelMethod == null) {
            throw new IllegalArgumentException("The kernel class " + origin.getName() + " does not contain a method processWorkItem");
        }

        final List<HWAKernelArgument> arguments = new ArrayList<>();
        for (final ResolvedField field : resolvedKernelClass.resolvedFields()) {
            try {
                final Field classField = origin.getDeclaredField(field.fieldName());
                // Make it accessible, so we can read the value from the kernel
                classField.setAccessible(true);
                arguments.add(new HWAKernelArgument(field.fieldName(), field.type(), classField));
            } catch (final NoSuchFieldException e) {
                throw new IllegalArgumentException("The kernel class " + origin.getName() + " does not contain a field " + field.fieldName());
            }
        }

        this.arguments = arguments;

        // Generate the code for the kernel
        final HWAStructuredControlflowCodeGenerator controlFlowGenerator = new HWAStructuredControlflowCodeGenerator(resolverContext, this.kernel, this.arguments);
        for (final ResolvedMethod m : kernelMethods) {
            if (!m.isConstructor()) {
                final MethodAnalyzer analyzer = m.analyze();
                new Sequencer<>(m, analyzer.ir(), controlFlowGenerator);
            }
        }

        try {
            final Path outputDirectory = Path.of("target", "openclkernels");
            outputDirectory.toFile().mkdirs();

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir.dot"))));

            final DominatorTree dominatorTree = new DominatorTree(analyzer.ir());

            DOTExporter.writeTo(dominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_dominatortree.dot"))));

            DOTExporter.writeBytecodeCFGTo(analyzer, new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecodecfg.dot"))));

            final CFGDominatorTree cfgDominatorTree = new CFGDominatorTree(analyzer.ir());
            DOTExporter.writeTo(cfgDominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_cfg_dominatortree.dot"))));

            final String kernelCode = controlFlowGenerator.toString();

            final PrintStream sequenced = new PrintStream(Files.newOutputStream(outputDirectory.resolve("sequenced.txt")));
            sequenced.print(kernelCode);

            // Create OpenCL program from Kernel source
            final MemorySegment errorRet = arena.allocate(ValueLayout.JAVA_INT);

            final MemorySegment strings = arena.allocate(ValueLayout.ADDRESS, 1);
            strings.setAtIndex(ValueLayout.ADDRESS, 0, arena.allocateFrom(kernelCode));

            this.clProgram = openCL.clCreateProgramWithSource(clContext, 1, strings, MemorySegment.NULL, errorRet);
            if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clCreateProgramWithSource failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
            }

            final MemorySegment devices = arena.allocate(ValueLayout.JAVA_LONG, 1);
            devices.setAtIndex(ValueLayout.JAVA_LONG, 0, deviceProperties.getId());

            final int result = openCL.clBuildProgram(clProgram, 1, devices, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
            if (result != OpenCL.CL_SUCCESS) {
                final MemorySegment logsize = arena.allocate(ValueLayout.JAVA_LONG);

                final int r1 = openCL.clGetProgramBuildInfo(clProgram, deviceProperties.getId(), OpenCL.CL_PROGRAM_BUILD_LOG, 0, MemorySegment.NULL, logsize);
                if (r1 != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetProgramBuildInfo failed: " + r1);
                }

                final long logsizeValue = logsize.get(ValueLayout.JAVA_LONG, 0);
                final MemorySegment log = arena.allocate(ValueLayout.JAVA_CHAR, logsizeValue * 2);
                final int r2 = openCL.clGetProgramBuildInfo(clProgram, deviceProperties.getId(), OpenCL.CL_PROGRAM_BUILD_LOG, log.byteSize(), log, MemorySegment.NULL);
                if (r2 != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetProgramBuildInfo failed: " + r2);
                }

                final String strlog = log.getString(0, Charset.defaultCharset());

                throw new RuntimeException("clBuildProgram failed: " + result + " : Log = " + strlog);
            }

            this.clKernel = openCL.clCreateKernel(clProgram, arena.allocateFrom(KERNEL_METHOD_NAME), errorRet);
            if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clCreateKernel failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
            }

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void compute(final int numberOfStreams) {
        try (final Arena arena = Arena.ofConfined()) {
            final List<KernelArgumentSharedDataDesc> buffers = new ArrayList<>();

            for (int i = 0; i < arguments.size(); i++) {
                final HWAKernelArgument argument = arguments.get(i);
                try {
                    final Object value = argument.field().get(kernel);
                    final IRType argumentType = argument.type();
                    if (argumentType.equals(IRType.CD_int)) {
                        // Int Value
                        final MemorySegment segment = arena.allocateFrom(ValueLayout.JAVA_INT, (int) value);
                        final int code = openCL.clSetKernelArg(clKernel, i, ValueLayout.JAVA_INT.byteSize(), segment);
                        if (code != OpenCL.CL_SUCCESS) {
                            throw new IllegalStateException("Got response code " + code + " for argument with index " + i + " and type " + argumentType.type());
                        }
                    } else if (argumentType.equals(IRType.CD_long)) {
                        // Long Value
                        final MemorySegment segment = arena.allocateFrom(ValueLayout.JAVA_LONG, (long) value);
                        final int code = openCL.clSetKernelArg(clKernel, i, ValueLayout.JAVA_LONG.byteSize(), segment);
                        if (code != OpenCL.CL_SUCCESS) {
                            throw new IllegalStateException("Got response code " + code + " for argument with index " + i + " and type " + argumentType.type());
                        }
                    } else if (argumentType.equals(IRType.CD_float)) {
                        // Float value
                        final MemorySegment segment = arena.allocateFrom(ValueLayout.JAVA_FLOAT, (float) value);
                        final int code = openCL.clSetKernelArg(clKernel, i, ValueLayout.JAVA_FLOAT.byteSize(), segment);
                        if (code != OpenCL.CL_SUCCESS) {
                            throw new IllegalStateException("Got response code " + code + " for argument with index " + i + " and type " + argumentType.type());
                        }
                    } else if (argumentType.isArray()) {
                        final IRType.MetaClass metaClass = (IRType.MetaClass) argumentType;

                        final MemorySegment buffer;
                        final MemorySegment segment;
                        final KernelArgumentSharedDataDesc kernelArgumentSharedDataDesc;
                        if (metaClass.componentType().equals(IRType.CD_float)) {
                            final float[] array = (float[]) value;
                            final int arrayLength = array.length;
                            final long bufferSize = ValueLayout.JAVA_FLOAT.byteSize() * arrayLength;

                            segment = arena.allocate(bufferSize, deviceProperties.memoryAlignment());
                            MemorySegment.copy(array, 0, segment, ValueLayout.JAVA_FLOAT, 0, arrayLength);

                            final MemorySegment errorCodeRet = arena.allocate(ValueLayout.JAVA_INT, 1);

                            buffer = openCL.clCreateBuffer(clContext,
                                    OpenCL.CL_MEM_READ_WRITE | OpenCL.CL_MEM_USE_HOST_PTR,
                                    bufferSize, segment, errorCodeRet);

                            if (errorCodeRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                                throw new RuntimeException("clCreateBuffer failed: " + errorCodeRet.get(ValueLayout.JAVA_INT, 0));
                            }

                            kernelArgumentSharedDataDesc = new KernelArgumentSharedDataDesc(bufferSize, segment, argument, value, buffer, IRType.CD_float);

                        } else if (metaClass.componentType().equals(IRType.CD_int)) {
                            final int[] array = (int[]) value;
                            final int arrayLength = array.length;
                            final long bufferSize = ValueLayout.JAVA_INT.byteSize() * arrayLength;

                            segment = arena.allocate(bufferSize, deviceProperties.memoryAlignment());
                            MemorySegment.copy(array, 0, segment, ValueLayout.JAVA_INT, 0, arrayLength);

                            final MemorySegment errorCodeRet = arena.allocate(ValueLayout.JAVA_INT, 1);

                            buffer = openCL.clCreateBuffer(clContext,
                                    OpenCL.CL_MEM_READ_WRITE | OpenCL.CL_MEM_USE_HOST_PTR,
                                    bufferSize, segment, errorCodeRet);

                            if (errorCodeRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                                throw new RuntimeException("clCreateBuffer failed: " + errorCodeRet.get(ValueLayout.JAVA_INT, 0));
                            }

                            kernelArgumentSharedDataDesc = new KernelArgumentSharedDataDesc(bufferSize, segment, argument, value, buffer, IRType.CD_int);

                        } else {
                            final ResolvedClass elementType = resolverContext.resolveClass(metaClass.componentType().type());
                            if (elementType.hasInterface(ClassDesc.of(FloatSerializable.class.getName()))) {
                                final FloatSerializable[] array = (FloatSerializable[]) value;
                                final int arrayLength = array.length;

                                final Optional<RuntimeVisibleAnnotationsAttribute> annotations = elementType.classModel().findAttribute(Attributes.runtimeVisibleAnnotations());
                                int elementCount = -1;
                                if (annotations.isPresent()) {
                                    final RuntimeVisibleAnnotationsAttribute attribute = annotations.get();
                                    for (final Annotation annotation : attribute.annotations()) {
                                        if (annotation.classSymbol().equals(ClassDesc.of(OpenCLType.class.getName()))) {
                                            for (final AnnotationElement element : annotation.elements()) {
                                                if ("elementCount".equals(element.name().stringValue())) {
                                                    elementCount = ((AnnotationValue.OfInt) element.value()).resolvedValue();
                                                }
                                            }
                                        }
                                    }
                                }
                                if (elementCount == -1) {
                                    throw new IllegalStateException("Cannot determine elementCount for array type " + metaClass.componentType().type());
                                }

                                final int bufferCapacity = arrayLength * elementCount;
                                final FloatBuffer floatBuffer = FloatBuffer.allocate(bufferCapacity);
                                for (final FloatSerializable element : array) {
                                    element.writeTo(floatBuffer);
                                }

                                final long memorySize = floatBuffer.capacity() * ValueLayout.JAVA_FLOAT.byteSize();
                                floatBuffer.rewind();
                                segment = arena.allocate(memorySize, deviceProperties.memoryAlignment());
                                for (int j = 0; j < floatBuffer.capacity(); j++) {
                                    segment.setAtIndex(ValueLayout.JAVA_FLOAT, j, floatBuffer.get());
                                }

                                final MemorySegment errorCodeRet = arena.allocate(ValueLayout.JAVA_INT, 1);

                                buffer = openCL.clCreateBuffer(clContext,
                                        OpenCL.CL_MEM_READ_WRITE | OpenCL.CL_MEM_USE_HOST_PTR,
                                        memorySize, segment, errorCodeRet);

                                if (errorCodeRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                                    throw new RuntimeException("clCreateBuffer failed: " + errorCodeRet.get(ValueLayout.JAVA_INT, 0));
                                }

                                kernelArgumentSharedDataDesc = new KernelArgumentSharedDataDesc(memorySize, segment, argument, value, buffer, elementType.thisType());

                            } else {
                                throw new IllegalStateException("Unsupported array type " + metaClass.componentType().type());
                            }
                        }

                        final MemorySegment bufferPointer = arena.allocate(ValueLayout.ADDRESS, 1);
                        bufferPointer.setAtIndex(ValueLayout.ADDRESS, 0, buffer);

                        final int code = openCL.clSetKernelArg(clKernel, i, ValueLayout.ADDRESS.byteSize(), bufferPointer);
                        if (code != OpenCL.CL_SUCCESS) {
                            throw new IllegalStateException("Got response code " + code + " for argument with index " + i + " and type " + argumentType.type());
                        }

                        buffers.add(kernelArgumentSharedDataDesc);

                    } else {
                        throw new IllegalStateException("Unsupported argument type " + argumentType.type());
                    }
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("Cannot access field " + argument.field().getName() + " of class " + kernel.getClass().getName(), e);
                }
            }

            // Set the work-item dimensions
            //final long[] global_work_size = {numberOfStreams};
            final MemorySegment global_work_size = arena.allocate(ValueLayout.JAVA_LONG, 1);
            global_work_size.setAtIndex(ValueLayout.JAVA_LONG, 0, numberOfStreams);

            // Let the driver guess the optimal size
            //final long[] local_work_size = null; //new long[] {32};
            final MemorySegment local_work_size = MemorySegment.NULL;

            // Execute the kernel
            openCL.clEnqueueNDRangeKernel(clCommandQueue, clKernel, 1, MemorySegment.NULL,
                    global_work_size, local_work_size, 0, MemorySegment.NULL, MemorySegment.NULL);

            // Wait till everything is done
            openCL.clFinish(clCommandQueue);

            // Read the output data
            for (final KernelArgumentSharedDataDesc sharedData : buffers) {
                openCL.clEnqueueReadBuffer(clCommandQueue, sharedData.clBuffer, OpenCL.CL_TRUE, 0,
                        sharedData.size, sharedData.pointer, 0, MemorySegment.NULL, MemorySegment.NULL);

                if (IRType.CD_int.equals(sharedData.arrayElementType)) {
                    final int[] array = (int[]) sharedData.value;
                    MemorySegment.copy(sharedData.pointer, ValueLayout.JAVA_INT, 0, array, 0, array.length);
                } else if (IRType.CD_float.equals(sharedData.arrayElementType)) {
                    final float[] array = (float[]) sharedData.value;
                    MemorySegment.copy(sharedData.pointer, ValueLayout.JAVA_FLOAT, 0, array, 0, array.length);
                } else {
                    // We assume at this point that we have an array of FloatSerializable
                    final FloatSerializable[] array = (FloatSerializable[]) sharedData.value;
                    final int bufferElementCount = (int) (sharedData.size / ValueLayout.JAVA_FLOAT.byteSize());
                    final FloatBuffer floatBuffer = FloatBuffer.allocate(bufferElementCount);
                    for (int i = 0; i < bufferElementCount; i++) {
                        floatBuffer.put(sharedData.pointer.getAtIndex(ValueLayout.JAVA_FLOAT, i));
                    }
                    floatBuffer.rewind();
                    for (final FloatSerializable floatSerializable : array) {
                        floatSerializable.readFrom(floatBuffer);
                    }
                }
            }

            for (final KernelArgumentSharedDataDesc buffer : buffers) {
                openCL.clReleaseMemObject(buffer.clBuffer);
            }
        }
    }

    public void close() {
        openCL.clReleaseKernel(clKernel);
        openCL.clReleaseProgram(clProgram);
    }
}
