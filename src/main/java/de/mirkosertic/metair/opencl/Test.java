package de.mirkosertic.metair.opencl;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.Charset;

public final class Test {

    public static void main(final String[] args) {

        final OpenCL cl = new OpenCL();

        try (final Arena arena = Arena.ofConfined()) {
            // Get platform IDs
            final MemorySegment numPlatformsMem = arena.allocate(ValueLayout.JAVA_INT);
            int result = cl.clGetPlatformIDs(
                    0, // num_entries = 0 to query count
                    MemorySegment.NULL, // platforms = NULL
                    numPlatformsMem // num_platforms
            );
            if (result != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clGetPlatformIDs failed: " + result);
            }

            final int numPlatforms = numPlatformsMem.get(ValueLayout.JAVA_INT, 0);
            System.out.println("Number of OpenCL platforms: " + numPlatforms);

            if (numPlatforms > 0) {

                final MemorySegment platforms = arena.allocate(ValueLayout.JAVA_LONG, numPlatforms);
                result = cl.clGetPlatformIDs(
                        numPlatforms,
                        platforms,
                        MemorySegment.NULL
                );
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetPlatformIDs failed: " + result);
                }

                final long[] platformIds = new long[numPlatforms];
                for (int i = 0; i < numPlatforms; i++) {
                    platformIds[i] = platforms.get(ValueLayout.JAVA_LONG, i * 8L);
                }

                System.out.println("Platform IDs retrieved successfully");

                for (int i = 0; i < numPlatforms; i++) {

                    final long platformId = platformIds[i];
                    final MemorySegment sizeRet = arena.allocate(ValueLayout.JAVA_LONG);

                    result = cl.clGetPlatformInfo(
                            platformId,
                            OpenCL.CL_PLATFORM_NAME,
                            0L,
                            MemorySegment.NULL,
                            sizeRet
                    );

                    if (result != OpenCL.CL_SUCCESS) {
                        throw new RuntimeException("clGetPlatformInfo failed: " + result);
                    }

                    final long size = sizeRet.get(ValueLayout.JAVA_LONG, 0);

                    // Second call to get actual data
                    final MemorySegment nameBuffer = arena.allocate(size);

                    result = cl.clGetPlatformInfo(
                            platformId,
                            OpenCL.CL_PLATFORM_NAME,
                            size,
                            nameBuffer,
                            MemorySegment.NULL
                    );

                    if (result != OpenCL.CL_SUCCESS) {
                        throw new RuntimeException("clGetPlatformInfo failed: " + result);
                    }

                    final String platformName = nameBuffer.getString(0);

                    System.out.println("Platform " + i + " id: " + platformId + " is " + platformName);

                    // Obtain the number of devices for the platform
                    final MemorySegment numDevicesMem = arena.allocate(ValueLayout.JAVA_INT);
                    result = cl.clGetDeviceIDs(platformId, OpenCL.CL_DEVICE_TYPE_ALL, 0, MemorySegment.NULL, numDevicesMem);
                    if (result != OpenCL.CL_SUCCESS) {
                        throw new RuntimeException("clGetDeviceIDs failed: " + result);
                    }
                    final int numDevices = numDevicesMem.get(ValueLayout.JAVA_INT, 0);

                    final MemorySegment devices = arena.allocate(ValueLayout.JAVA_LONG, numDevices);
                    result = cl.clGetDeviceIDs(
                            platformId,
                            OpenCL.CL_DEVICE_TYPE_ALL,
                            numDevices,
                            devices,
                            MemorySegment.NULL
                    );
                    if (result != OpenCL.CL_SUCCESS) {
                        throw new RuntimeException("clGetPlatformIDs failed: " + result);
                    }

                    System.out.println("Number of OpenCL devices for platform " + i + ": " + numDevices);

                    final long[] deviceIds = new long[numDevices];
                    for (int j = 0; j < numDevices; j++) {
                        deviceIds[j] = devices.get(ValueLayout.JAVA_LONG, j * 8L);

                        // Obtain the length of the string that will be queried
                        final MemorySegment paramSizeRet = arena.allocate(ValueLayout.JAVA_LONG, numPlatforms);
                        result = cl.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_NAME, 0, MemorySegment.NULL, paramSizeRet);
                        if (result != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clGetDeviceInfo failed: " + result);
                        }

                        final MemorySegment paramBuffer = arena.allocate(paramSizeRet.get(ValueLayout.JAVA_LONG, 0));
                        result = cl.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_NAME, paramBuffer.byteSize(), paramBuffer, MemorySegment.NULL);
                        if (result != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clGetDeviceInfo failed: " + result);
                        }

                        final String deviceName = paramBuffer.getString(0);

                        System.out.println("Device " + j + " id: " + deviceIds[j] + " is " + deviceName);

                        final MemorySegment properties = arena.allocate(ValueLayout.JAVA_LONG, 3 * 8L);
                        properties.set(ValueLayout.JAVA_LONG, 0, OpenCL.CL_CONTEXT_PLATFORM);
                        properties.set(ValueLayout.JAVA_LONG, 8, platformId);
                        properties.set(ValueLayout.JAVA_LONG, 16, 0);

                        final MemorySegment deviceId = arena.allocateFrom(ValueLayout.JAVA_LONG, deviceIds[j]);
                        final MemorySegment errorRet = arena.allocate(ValueLayout.JAVA_INT);

                        final MemorySegment context = cl.clCreateContext(properties, 1, deviceId, MemorySegment.NULL, MemorySegment.NULL, errorRet);
                        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clCreateContext failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
                        }

                        final MemorySegment commandQueue = cl.clCreateCommandQueue(context, deviceIds[j], MemorySegment.NULL, errorRet);
                        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clCreateContext failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
                        }

                        final MemorySegment strings = arena.allocate(ValueLayout.ADDRESS, 1);
                        strings.setAtIndex(ValueLayout.ADDRESS, 0, arena.allocateFrom("""
__kernel void do_nothing() {
    // Absolutely nothing happens here
}
"""));
                        final MemorySegment clprogram = cl.clCreateProgramWithSource(context, 1, strings, MemorySegment.NULL, errorRet);
                        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clCreateProgramWithSource failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
                        }

                        result = cl.clBuildProgram(clprogram, 0, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
                        if (result != OpenCL.CL_SUCCESS) {
                            final MemorySegment logsize = arena.allocate(ValueLayout.JAVA_LONG);

                            final int r1 = cl.clGetProgramBuildInfo(clprogram, deviceIds[j], OpenCL.CL_PROGRAM_BUILD_LOG, 0, MemorySegment.NULL, logsize);
                            if (r1 != OpenCL.CL_SUCCESS) {
                                throw new RuntimeException("clGetProgramBuildInfo failed: " + r1);
                            }

                            final long logsizeValue = logsize.get(ValueLayout.JAVA_LONG, 0);
                            final MemorySegment log = arena.allocate(ValueLayout.JAVA_CHAR, logsizeValue * 2);
                            final int r2 = cl.clGetProgramBuildInfo(clprogram, deviceIds[j], OpenCL.CL_PROGRAM_BUILD_LOG, log.byteSize(), log, MemorySegment.NULL);
                            if (r2 != OpenCL.CL_SUCCESS) {
                                throw new RuntimeException("clGetProgramBuildInfo failed: " + r2);
                            }

                            final String strlog = log.getString(0, Charset.defaultCharset());

                            throw new RuntimeException("clBuildProgram failed: " + result + " : Log " + strlog);
                        }

                        final MemorySegment kernel = cl.clCreateKernel(clprogram, arena.allocateFrom("do_nothing"), errorRet);
                        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
                            throw new RuntimeException("clCreateKernel failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
                        }

                        cl.clFinish(commandQueue);

                        cl.clReleaseKernel(kernel);
                        cl.clReleaseProgram(clprogram);

                        cl.clReleaseCommandQueue(commandQueue);
                        cl.clReleaseContext(context);
                    }
                }
            }
        }
    }
}
