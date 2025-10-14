/*
 * Copyright 2018 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.metair.opencl.cpu;

import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.DeviceProperties;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;
import de.mirkosertic.metair.opencl.api.Platform;
import de.mirkosertic.metair.opencl.api.PlatformProperties;

public class CPUPlatform implements Platform {

    private final OpenCLOptions openCLOptions;
    private final PlatformProperties platformProperties;
    private final DeviceProperties deviceProperties;

    public CPUPlatform(final OpenCLOptions openCLOptions) {
        this.openCLOptions = openCLOptions;
        this.platformProperties = new PlatformProperties() {
            @Override
            public long getId() {
                return -1;
            }

            @Override
            public String getName() {
                return "JVM";
            }
        };
        this.deviceProperties = new DeviceProperties() {

            @Override
            public long getId() {
                return -1;
            }

            @Override
            public String getName() {
                return "System CPU";
            }

            @Override
            public int getNumberOfComputeUnits() {
                return Runtime.getRuntime().availableProcessors();
            }

            @Override
            public long[] getMaxWorkItemSizes() {
                return new long[] {-1, -1 , -1};
            }

            @Override
            public long getMaxWorkGroupSize() {
                return -1;
            }

            @Override
            public long getClockFrequency() {
                return 1;
            }

            @Override
            public int memoryAlignment() {
                return 4;
            }
        };
    }

    @Override
    public Context createContext() {
        return new CPUContext(openCLOptions);
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
