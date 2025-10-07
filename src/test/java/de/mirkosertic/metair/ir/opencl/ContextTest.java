package de.mirkosertic.metair.ir.opencl;

import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.Float2;
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;
import de.mirkosertic.metair.opencl.api.Platform;
import de.mirkosertic.metair.opencl.api.PlatformFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static de.mirkosertic.metair.opencl.api.Float2.float2;
import static de.mirkosertic.metair.opencl.api.GlobalFunctions.get_global_id;
import static de.mirkosertic.metair.opencl.api.VectorFunctions.normalize;

@Disabled
public class ContextTest {

    @Test
    public void testSimpleAdd() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final float a = theA[id];
                    final float b = theB[id];
                    theResult[id] = a + b;
                }
            });
        }

        for (final float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testSimpleAddWithInlineMethod() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {

                private float add(final float a, final float b) {
                    return a + b;
                }

                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final float a = theA[id];
                    final float b = theB[id];
                    theResult[id] = add(a, b);
                }
            });
        }

        for (final float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testVectorNormalize() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final Float2[] theA = {float2(10f, 20f)};
        final Float2[] theResult = new Float2[] {float2(-1f, -1f)};

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(theA.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final Float2 theVec = normalize(theA[id]);
                    theResult[id].s1 = theVec.s1;
                }
            });
        }

        for (final Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testSimpleCopy() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final Float2[] theA = {float2(10f, 20f), float2(30f, 40f)};
        final Float2[] theResult = new Float2[] {float2(0f, 0f), float2(0f, 0f)};

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(theA.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final Float2 a = theA[id];
                    theResult[id] = float2(a.s0, a.s1);
                }
            });
        }

        for (final Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testSimpleCopyWithPrimitiveInput() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final float adder = 10;
        final Float2[] theA = {float2(10f, 20f), float2(30f, 40f)};
        final Float2[] theResult = new Float2[] {float2(0f, 0f), float2(0f, 0f)};

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(theA.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final Float2 a = theA[id];
                    theResult[id] = float2(a.s0 + adder, a.s1 + adder);
                }
            });
        }

        for (final Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testMandelbrot() throws Exception {

        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());
        // final Platform thePlatform = new CPUPlatform(new Slf4JLogger());

        final int iteration = 30;
        final float cellSize = 0.00625f;
        final int width = 440;
        final int height = 350;

        final int[] imageData = new int[width * height];

        final long startTime = System.currentTimeMillis();

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(imageData.length, new Kernel() {

                private int checkC(final double reC, final double imC) {
                    double reZ=0,imZ=0,reZ_minus1=0,imZ_minus1=0;
                    int i;
                    for (i=0;i<iteration;i++) {
                        imZ=2*reZ_minus1*imZ_minus1+imC;
                        reZ=reZ_minus1*reZ_minus1-imZ_minus1*imZ_minus1+reC;
                        if (reZ*reZ+imZ*imZ>4) return i;
                        reZ_minus1=reZ;
                        imZ_minus1=imZ;
                    }
                    return i;
                }

                @Override
                public void processWorkItem() {
                    // Get Parallel Index
                    final int pixelIndex = get_global_id(0);
                    final int x = pixelIndex % width;
                    final int y = pixelIndex / width;

                    final float reC = -2.1f + (x * cellSize);
                    final float imC = -2.1f + (y * cellSize);

                    imageData[pixelIndex] = checkC(reC, imC);
                }
            });
        }

        final long duration = System.currentTimeMillis() - startTime;

        System.out.println("Took " + duration + "ms");
    }
}