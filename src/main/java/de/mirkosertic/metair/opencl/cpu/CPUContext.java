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
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;

import java.util.stream.IntStream;

import static de.mirkosertic.metair.opencl.api.GlobalFunctions.set_global_id;
import static de.mirkosertic.metair.opencl.api.GlobalFunctions.set_global_size;

public class CPUContext implements Context {

    private final OpenCLOptions openCLOptions;

    public CPUContext(final OpenCLOptions aOptions) {
        this.openCLOptions = aOptions;
    }

    @Override
    public void compute(final int numberOfStreams, final Kernel kernel) {

        IntStream.range(0, numberOfStreams)
        .parallel()
        .forEach(workItemId->{
            try {
                set_global_size(0, numberOfStreams);
                set_global_id(0, workItemId);
                kernel.processWorkItem();
            } catch (final Exception e) {
                throw new IllegalStateException("Kernel execution (single work item) failed.", e);
            }
        }); // blocks until all work-items are complete

    }

    @Override
    public void close() {
        // no-op
    }
}
