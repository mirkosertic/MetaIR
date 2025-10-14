package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.opencl.OpenCL;
import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.DeviceProperties;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;
import de.mirkosertic.metair.opencl.api.Platform;
import de.mirkosertic.metair.opencl.api.PlatformProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HWAPlatform implements Platform {

    private final PlatformProperties platformProperties;
    private final DeviceProperties deviceProperties;
    private final OpenCL openCL;

    public HWAPlatform(final OpenCL openCL, final OpenCLOptions openCLOptions) {
        this.openCL = openCL;

        final Optional<PlatformProperties> platform = getPlatforms().stream().filter(openCLOptions.getPlatformFilter()).findFirst();
        if (platform.isEmpty()) {
            throw new IllegalArgumentException("No platform found that matches the configured filter");
        }
        this.platformProperties = platform.get();

        final Optional<DeviceProperties> devices = getDevicesFor(platformProperties.getId()).stream().sorted(openCLOptions.getPreferredDeviceComparator()).findFirst();
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("No device found that matches the configured filter");
        }

        this.deviceProperties = devices.get();
    }

    private List<PlatformProperties> getPlatforms() {

        final List<PlatformProperties> retplatforms = new ArrayList<>();

        try (final Arena arena = Arena.ofConfined()) {
            final MemorySegment numPlatformsMem = arena.allocate(ValueLayout.JAVA_INT);
            int result = openCL.clGetPlatformIDs(
                    0, // num_entries = 0 to query count
                    MemorySegment.NULL, // platforms = NULL
                    numPlatformsMem // num_platforms
            );
            if (result != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clGetPlatformIDs failed: " + result);
            }

            final int numPlatforms = numPlatformsMem.get(ValueLayout.JAVA_INT, 0);

            if (numPlatforms > 0) {

                final MemorySegment platforms = arena.allocate(ValueLayout.JAVA_LONG, numPlatforms);
                result = openCL.clGetPlatformIDs(
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

                for (int i = 0; i < numPlatforms; i++) {

                    final long platformId = platformIds[i];
                    final MemorySegment sizeRet = arena.allocate(ValueLayout.JAVA_LONG);

                    result = openCL.clGetPlatformInfo(
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

                    result = openCL.clGetPlatformInfo(
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

                    retplatforms.add(new PlatformProperties() {
                        @Override
                        public long getId() {
                            return platformId;
                        }

                        @Override
                        public String getName() {
                            return platformName;
                        }
                    });
                }
            }
        }

        return retplatforms;
    }

    private List<DeviceProperties> getDevicesFor(final long platformId) {
        final List<DeviceProperties> retDevices = new ArrayList<>();
        try (final Arena arena = Arena.ofConfined()) {
            // Obtain the number of devices for the platform
            final MemorySegment numDevicesMem = arena.allocate(ValueLayout.JAVA_INT);
            int result = openCL.clGetDeviceIDs(platformId, OpenCL.CL_DEVICE_TYPE_ALL, 0, MemorySegment.NULL, numDevicesMem);
            if (result != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clGetDeviceIDs failed: " + result);
            }
            final int numDevices = numDevicesMem.get(ValueLayout.JAVA_INT, 0);

            final MemorySegment devices = arena.allocate(ValueLayout.JAVA_LONG, numDevices);
            result = openCL.clGetDeviceIDs(
                    platformId,
                    OpenCL.CL_DEVICE_TYPE_ALL,
                    numDevices,
                    devices,
                    MemorySegment.NULL
            );
            if (result != OpenCL.CL_SUCCESS) {
                throw new RuntimeException("clGetPlatformIDs failed: " + result);
            }

            final long[] deviceIds = new long[numDevices];
            for (int j = 0; j < numDevices; j++) {
                deviceIds[j] = devices.get(ValueLayout.JAVA_LONG, j * 8L);

                // Obtain the length of the string that will be queried
                final MemorySegment paramSizeRet = arena.allocate(ValueLayout.JAVA_LONG, numDevices);
                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_NAME, 0, MemorySegment.NULL, paramSizeRet);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final MemorySegment paramBuffer = arena.allocate(paramSizeRet.get(ValueLayout.JAVA_LONG, 0));
                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_NAME, paramBuffer.byteSize(), paramBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final String deviceName = paramBuffer.getString(0);

                final MemorySegment computeInitsBuffer = arena.allocate(paramSizeRet.get(ValueLayout.JAVA_INT, 0));
                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MAX_COMPUTE_UNITS, computeInitsBuffer.byteSize(), computeInitsBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final int computeUnits = computeInitsBuffer.get(ValueLayout.JAVA_INT, 0);

                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MAX_WORK_GROUP_SIZE, computeInitsBuffer.byteSize(), computeInitsBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final int maxWorkGroupSize = computeInitsBuffer.get(ValueLayout.JAVA_INT, 0);

                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MAX_CLOCK_FREQUENCY, computeInitsBuffer.byteSize(), computeInitsBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final int maxClockFrequency = computeInitsBuffer.get(ValueLayout.JAVA_INT, 0);

                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MEM_BASE_ADDR_ALIGN, computeInitsBuffer.byteSize(), computeInitsBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final int memoryAlignment = computeInitsBuffer.get(ValueLayout.JAVA_INT, 0) / 8;

                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS, computeInitsBuffer.byteSize(), computeInitsBuffer, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final int workItemDimensions = computeInitsBuffer.get(ValueLayout.JAVA_INT, 0);
                final MemorySegment dimensionsSizeRet = arena.allocate(ValueLayout.JAVA_LONG, workItemDimensions);
                result = openCL.clGetDeviceInfo(deviceIds[j], OpenCL.CL_DEVICE_MAX_WORK_ITEM_SIZES, ValueLayout.JAVA_LONG.byteSize() * workItemDimensions, dimensionsSizeRet, MemorySegment.NULL);
                if (result != OpenCL.CL_SUCCESS) {
                    throw new RuntimeException("clGetDeviceInfo failed: " + result);
                }

                final long[] workItemSizes = new long[workItemDimensions];
                for (int k = 0; k < workItemDimensions; k++) {
                    workItemSizes[k] = dimensionsSizeRet.getAtIndex(ValueLayout.JAVA_LONG, k);
                }

                final long deviceId = deviceIds[j];

                retDevices.add(new DeviceProperties() {
                    @Override
                    public long getId() {
                        return deviceId;
                    }

                    @Override
                    public String getName() {
                        return deviceName;
                    }

                    @Override
                    public int getNumberOfComputeUnits() {
                        return computeUnits;
                    }

                    @Override
                    public long[] getMaxWorkItemSizes() {
                        return workItemSizes;
                    }

                    @Override
                    public long getMaxWorkGroupSize() {
                        return maxWorkGroupSize;
                    }

                    @Override
                    public long getClockFrequency() {
                        return maxClockFrequency;
                    }

                    @Override
                    public int memoryAlignment() {
                        return memoryAlignment;
                    }
                });
            }
        }
        return retDevices;
    }

    @Override
    public Context createContext() {
        return new HWAContext(openCL, platformProperties, deviceProperties);
    }

    @Override
    public PlatformProperties getPlatformProperties() {
        return platformProperties;
    }

    @Override
    public DeviceProperties getDeviceProperties() {
        return deviceProperties;
    }
}
