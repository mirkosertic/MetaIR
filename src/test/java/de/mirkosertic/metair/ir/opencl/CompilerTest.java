package de.mirkosertic.metair.ir.opencl;

import de.mirkosertic.metair.opencl.api.Kernel;

import static de.mirkosertic.metair.opencl.api.GlobalFunctions.get_global_id;

public class CompilerTest {

    private Kernel createKernel() {
        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        return new Kernel() {
            @Override public void processWorkItem() {
                final int id = get_global_id(0);
                final float a = theA[id];
                final float b = theB[id];

                final byte b1 = (byte) 1;
                final short s2 = (short) 2;
                final int s3 = 3;
                final long s4 = 4;
                final float s5 = 5f;
                final double s6 = 6d;

                final int d;
                if (a<b) {
                    d = 100;
                    final int j = 100;
                    final int k = 2300;
                    final int c = 300;
                    final int dsd = 10010101;
                } else {
                    d = 200;
                    final int j = d * d * d * d;
                    final int k = 2300;
                    final int c = 300;
                    final int dsd = 10010101;
                }

                for (int lala = 100; 200 > lala; lala++) {
                    final int k = 100;
                }

                theResult[id] = a + b + d;
            }
        };
    }

    /*
    @Test
    public void testSimpleKernel() throws IOException {
        final AnalysisStack analysisStack = new AnalysisStack();
        final OpenCLCompileBackend backend = new OpenCLCompileBackend();
        final CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true, "opencl", 512, 512, false, false, Allocator.passthru, new String[0], new String[0], false);

        final Kernel theKernel = createKernel();
        final Class<? extends Kernel> theKernelClass = theKernel.getClass();
        System.out.println(theKernelClass);

        final Method[] theMethods = theKernelClass.getDeclaredMethods();
        if (1 != theMethods.length) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        final Method theMethod = theMethods[0];

        final BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = backend.newLinkerContext(theLoader, compileOptions.getLogger());
        final OpenCLCompileResult compiledKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature, analysisStack);
        final OpenCLCompileResult.OpenCLContent content = (OpenCLCompileResult.OpenCLContent) compiledKernel.getContent()[0];

        System.out.println(content.asString());
    }

    @Test
    public void testKernelWithComplexType() throws IOException {
        final AnalysisStack analysisStack = new AnalysisStack();
        final OpenCLCompileBackend backend = new OpenCLCompileBackend();
        final CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true, "opencl", 512, 512, false, false, Allocator.passthru, new String[0], new String[0], false);

        final Float2[] theIn = new Float2[10];
        final Float2[] theOut = new Float2[10];
        final Kernel theKernel = new Kernel() {
            @Override public void processWorkItem() {
                final int theIndex = get_global_id(0);
                final Float2 a = theIn[theIndex];
                final Float2 b = theOut[theIndex];
                b.s0 = a.s0;
                b.s1 = a.s1;
            }
        };
        final Class<? extends Kernel> theKernelClass = theKernel.getClass();
        System.out.println(theKernelClass);

        final Method[] theMethods = theKernelClass.getDeclaredMethods();
        if (1 != theMethods.length) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        final Method theMethod = theMethods[0];

        final BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = backend.newLinkerContext(theLoader, compileOptions.getLogger());
        final OpenCLCompileResult compiledKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature, analysisStack);
        final OpenCLCompileResult.OpenCLContent content = (OpenCLCompileResult.OpenCLContent) compiledKernel.getContent()[0];

        System.out.println(content.asString());
    }*/
}
