package de.mirkosertic.metair.ir.opencl;

import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.DeviceProperties;
import de.mirkosertic.metair.opencl.api.Float4;
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;
import de.mirkosertic.metair.opencl.api.Platform;
import de.mirkosertic.metair.opencl.api.PlatformFactory;
import de.mirkosertic.metair.opencl.api.PlatformProperties;
import org.junit.jupiter.api.Test;

import static de.mirkosertic.metair.opencl.api.Float4.float4;
import static de.mirkosertic.metair.opencl.api.GlobalFunctions.get_global_id;
import static de.mirkosertic.metair.opencl.api.GlobalFunctions.get_global_size;
import static de.mirkosertic.metair.opencl.api.VectorFunctions.dot;
import static de.mirkosertic.metair.opencl.api.VectorFunctions.length;

public class AliceBobCarolDaveTest {

    @Test
    public void testSimilarity() throws Exception {
        // The data of our four friends
        final Float4 theAlice = float4(5f, 1f, 0f, 6f);
        final Float4 theBob = float4(0f, 10f, 3f, 0f);
        final Float4 theCarol = float4(2f, 6f, 3f, 2f);
        final Float4 theDave = float4(7f, 2f, 1f, 8f);

        // We need an input for our kernel, a list of vectors
        final Float4[] theInputs = new Float4[] {theAlice, theCarol, theBob, theDave};

        // This is the computed output
        final int[] theMostSimilar = new int[theInputs.length];
        final float[] theMostSimilarity = new float[theInputs.length];

        // We obtain a platform
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());
        // All computation is done within a context. A context is
        // used to cache memory buffers and compiled kernels
        try (final Context theContext = thePlatform.createContext()) {

            // We fire up the computations
            theContext.compute(theInputs.length, new Kernel() {

                // This method is called for every workitem
                @Override
                public void processWorkItem() {
                    // This is the id of the current work item
                    final int theCurrentWorkItemId = get_global_id(0);
                    // This is the total number of work items
                    final int theMax = get_global_size(0);

                    // We obtain the current work item from the list
                    final Float4 theCurrent = theInputs[theCurrentWorkItemId];
                    final float theCurrentLength = length(theCurrent);

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    // And compute the similarities with all other work item
                    // except itself
                    for (int i = 0;i<theMax;i++) {
                        if (i != theCurrentWorkItemId) {
                            final Float4 theOther = theInputs[i];
                            final float theOtherLength = length(theOther);

                            final float theLength = theCurrentLength * theOtherLength;

                            if (theLength != 0) {
                                final float theSimilarity = dot(theCurrent, theOther) / (theLength);

                                if (theSimilarity > theMaxSimilarity) {
                                    theMaxSimilarity = theSimilarity;
                                    theMaxIndex = i;
                                }
                            }
                        }
                    }

                    // The highest similarity is written to the output
                    theMostSimilar[theCurrentWorkItemId] = theMaxIndex;
                    theMostSimilarity[theCurrentWorkItemId] = theMaxSimilarity;
                }
            });
        }

        // Output the results
        for (int i=0;i<theInputs.length;i++) {
            System.out.println("Most similar match for input " + i + " is " + theMostSimilar[i] + " with a similarity of " + theMostSimilarity[i]);
        }
    }

    @Test
    public void testPerformance() throws Exception {
        final int theMaxSize = 100000;
        final Float4[] theInputs = new Float4[theMaxSize];
        for (int i=0;i<theMaxSize;i++) {
            theInputs[i] = float4((float) Math.random() * 10, (float) Math.random() * 10, (float) Math.random() * 10, (float) Math.random() * 10);
        }

        final int[] theMostSimilar = new int[theInputs.length];
        final float[] theMostSimilarity = new float[theInputs.length];

        final long theStart = System.currentTimeMillis();

        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new OpenCLOptions.Builder().build());

        final PlatformProperties thePlatformProps = thePlatform.getPlatformProperties();
        System.out.println("Platform is   : " + thePlatformProps.getName());

        final DeviceProperties theDevProps = thePlatform.getDeviceProperties();
        System.out.println("Device        : " + theDevProps.getName());
        System.out.println(" # CU         : " + theDevProps.getNumberOfComputeUnits());
        System.out.println(" Clock freq.  : " + theDevProps.getClockFrequency());
        System.out.println(" Max workgroup: " + theDevProps.getMaxWorkGroupSize());

        try (final Context theContext = thePlatform.createContext()) {

            theContext.compute(theInputs.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int theCurrentWorkItemId = get_global_id(0);
                    final int theMax = get_global_size(0);

                    final Float4 theCurrent = theInputs[theCurrentWorkItemId];
                    final float theCurrentLength = length(theCurrent);

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    for (int i = 0; i < theMax; i++) {
                        if (i != theCurrentWorkItemId) {
                            final Float4 theOther = theInputs[i];
                            final float theOtherLength = length(theOther);

                            final float theLength = theCurrentLength * theOtherLength;

                            if (theLength != 0) {
                                final float theSimilarity = dot(theCurrent, theOther) / (theLength);

                                if (theSimilarity > theMaxSimilarity) {
                                    theMaxSimilarity = theSimilarity;
                                    theMaxIndex = i;
                                }
                            }
                        }
                    }

                    theMostSimilar[theCurrentWorkItemId] = theMaxIndex;
                    theMostSimilarity[theCurrentWorkItemId] = theMaxSimilarity;
                }
            });
        }

        final long theDuration = System.currentTimeMillis() - theStart;
        System.out.println("Took " + theDuration);
    }
}