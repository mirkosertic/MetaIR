package de.mirkosertic.metair.opencl;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class OpenCL implements OpenCLConstants {

    private static final String[] WINDOWS_NAMES = {
            "OpenCL.dll",
            "OpenCL"
    };

    private static final String[] LINUX_NAMES = {
            "libOpenCL.so.1",
            "libOpenCL.so",
            "OpenCL"
    };

    private static final String[] MACOS_NAMES = {
            "OpenCL.framework/OpenCL",
            "/System/Library/Frameworks/OpenCL.framework/OpenCL",
            "libOpenCL.dylib",
            "OpenCL"
    };

    private SymbolLookup opencl;
    private Linker linker;

    private MethodHandle clGetPlatformIDs;
    private MethodHandle clGetPlatformInfo;
    private MethodHandle clGetDeviceIDs;
    private MethodHandle clGetDeviceInfo;
    private MethodHandle clCreateContext;
    private MethodHandle clCreateCommandQueue;
    private MethodHandle clCreateProgramWithSource;
    private MethodHandle clBuildProgram;
    private MethodHandle clGetProgramBuildInfo;
    private MethodHandle clCreateKernel;
    private MethodHandle clReleaseKernel;
    private MethodHandle clReleaseProgram;
    private MethodHandle clReleaseCommandQueue;
    private MethodHandle clReleaseContext;
    private MethodHandle clFinish;
    private MethodHandle clCreateBuffer;
    private MethodHandle clSetKernelArg;
    private MethodHandle clEnqueueNDRangeKernel;
    private MethodHandle clReleaseMemObject;
    private MethodHandle clEnqueueReadBuffer;
    private MethodHandle clEnqueueWriteBuffer;

    public OpenCL() {
        // Perform auto discovery of the OpenCL library
        final String os = System.getProperty("os.name").toLowerCase();
        final String[] libNames;

        if (os.contains("win")) {
            libNames = WINDOWS_NAMES;
        } else if (os.contains("mac") || os.contains("darwin")) {
            libNames = MACOS_NAMES;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            libNames = LINUX_NAMES;
        } else {
            throw new UnsatisfiedLinkError("Unsupported operating system: " + os);
        }

        // Try each library name until one loads successfully
        for (final String libName : libNames) {
            try {
                final SymbolLookup lookup = SymbolLookup.libraryLookup(libName, Arena.global());
                init(lookup);
                return;
            } catch (final IllegalArgumentException e) {
                // Library not found, try next
                continue;
            }
        }

        throw new UnsatisfiedLinkError(
                "Could not load OpenCL library. Tried: " + String.join(", ", libNames)
        );
    }

    public OpenCL(final SymbolLookup opencl) {
        init(opencl);
    }

    private void init(final SymbolLookup opencl) {
        this.opencl = opencl;
        this.linker = Linker.nativeLinker();

        initclGetPlatformIDs();
        initclGetPlatformInfo();
        initclGetDeviceIDs();
        initclGetDeviceInfo();
        initclCreateContext();
        initclCreateCommandQueue();
        initclCreateProgramWithSource();
        initclBuildProgram();
        initclGetProgramBuildInfo();
        initclCreateKernel();
        initclReleaseKernel();
        initclReleaseProgram();
        initclReleaseCommandQueue();
        initclReleaseContext();
        initclFinish();
        initclCreateBuffer();
        initclSetKernelArg();
        initclEnqueueNDRangeKernel();
        initclReleaseMemObject();
        initclEnqueueReadBuffer();
        initclEnqueueWriteBuffer();
    }

    private void initclGetPlatformIDs() {
        final MemorySegment symbol = opencl.find("clGetPlatformIDs")
                .orElseThrow(() -> new RuntimeException("clGetPlatformIDs not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,           // return type (cl_int)
                ValueLayout.JAVA_INT,           // num_entries
                ValueLayout.ADDRESS,            // platforms (can be NULL)
                ValueLayout.ADDRESS             // num_platforms
        );

        clGetPlatformIDs = linker.downcallHandle(symbol, descriptor);
    }

    private void initclGetPlatformInfo() {
        final MemorySegment symbol = opencl.find("clGetPlatformInfo")
                .orElseThrow(() -> new RuntimeException("clGetPlatformInfo not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,           // return type
                ValueLayout.JAVA_LONG,          // platform
                ValueLayout.JAVA_INT,           // param_name
                ValueLayout.JAVA_LONG,          // param_value_size
                ValueLayout.ADDRESS,            // param_value
                ValueLayout.ADDRESS             // param_value_size_ret
        );

        clGetPlatformInfo = linker.downcallHandle(symbol, descriptor);
    }

    private void initclGetDeviceIDs() {
        final MemorySegment symbol = opencl.find("clGetDeviceIDs")
                .orElseThrow(() -> new RuntimeException("clGetDeviceIDs not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG, // platform
                ValueLayout.JAVA_LONG, // device type
                ValueLayout.JAVA_INT, // bum entries
                ValueLayout.ADDRESS, // devices
                ValueLayout.ADDRESS // num devices
        );

        clGetDeviceIDs = linker.downcallHandle(symbol, descriptor);
    }

    private void initclGetDeviceInfo() {
        final MemorySegment symbol = opencl.find("clGetDeviceInfo")
                .orElseThrow(() -> new RuntimeException("clGetDeviceIDs not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG, // device id
                ValueLayout.JAVA_INT,  // param_name
                ValueLayout.JAVA_LONG, // param_value_size
                ValueLayout.ADDRESS,   // param_value
                ValueLayout.ADDRESS    // param_value_size_ret
        );

        clGetDeviceInfo = linker.downcallHandle(symbol, descriptor);
    }

    private void initclCreateContext() {
        final MemorySegment symbol = opencl.find("clCreateContext")
                .orElseThrow(() -> new RuntimeException("clCreateContext not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, // cl_context_properties id
                ValueLayout.JAVA_INT, // num_devices
                ValueLayout.ADDRESS, // cl_device_id
                ValueLayout.ADDRESS, // callback
                ValueLayout.ADDRESS, // user_data
                ValueLayout.ADDRESS // errcode_ret
        );

        clCreateContext = linker.downcallHandle(symbol, descriptor);
    }

    private void initclCreateCommandQueue() {
        final MemorySegment symbol = opencl.find("clCreateCommandQueue")
                .orElseThrow(() -> new RuntimeException("clCreateCommandQueue not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, // context
                ValueLayout.JAVA_LONG, // cl_device_id
                ValueLayout.ADDRESS, // cl_command_queue_properties
                ValueLayout.ADDRESS // errcode_ret
        );

        clCreateCommandQueue = linker.downcallHandle(symbol, descriptor);
    }

    private void initclCreateProgramWithSource() {
        final MemorySegment symbol = opencl.find("clCreateProgramWithSource")
                .orElseThrow(() -> new RuntimeException("clCreateProgramWithSource not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, // context
                ValueLayout.JAVA_INT, // count
                ValueLayout.ADDRESS, // strings
                ValueLayout.ADDRESS, // lengths
                ValueLayout.ADDRESS // errcode_ret
        );

        clCreateProgramWithSource = linker.downcallHandle(symbol, descriptor);
    }

    private void initclBuildProgram() {
        final MemorySegment symbol = opencl.find("clBuildProgram")
                .orElseThrow(() -> new RuntimeException("clBuildProgram not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // program
                ValueLayout.JAVA_INT, // num_devices
                ValueLayout.ADDRESS, // device_list
                ValueLayout.ADDRESS, // options
                ValueLayout.ADDRESS, // callback
                ValueLayout.ADDRESS // user_data
        );

        clBuildProgram = linker.downcallHandle(symbol, descriptor);
    }

    private void initclGetProgramBuildInfo() {
        final MemorySegment symbol = opencl.find("clGetProgramBuildInfo")
                .orElseThrow(() -> new RuntimeException("clGetProgramBuildInfo not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // program
                ValueLayout.JAVA_LONG, // device
                ValueLayout.JAVA_INT, // param_name
                ValueLayout.JAVA_LONG, // param_value_size
                ValueLayout.ADDRESS, // param_value
                ValueLayout.ADDRESS  // param_value_size_ret
        );

        clGetProgramBuildInfo = linker.downcallHandle(symbol, descriptor);
    }

    private void initclCreateKernel() {
        final MemorySegment symbol = opencl.find("clCreateKernel")
                .orElseThrow(() -> new RuntimeException("clCreateKernel not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,  // program
                ValueLayout.ADDRESS, // kernel_name
                ValueLayout.ADDRESS // errcode_ret
        );

        clCreateKernel = linker.downcallHandle(symbol, descriptor);
    }

    private void initclReleaseKernel() {
        final MemorySegment symbol = opencl.find("clReleaseKernel")
                .orElseThrow(() -> new RuntimeException("clReleaseKernel not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // kernel
        );

        clReleaseKernel = linker.downcallHandle(symbol, descriptor);
    }

    private void initclReleaseProgram() {
        final MemorySegment symbol = opencl.find("clReleaseProgram")
                .orElseThrow(() -> new RuntimeException("clReleaseProgram not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // program
        );

        clReleaseProgram = linker.downcallHandle(symbol, descriptor);
    }

    private void initclReleaseCommandQueue() {
        final MemorySegment symbol = opencl.find("clReleaseCommandQueue")
                .orElseThrow(() -> new RuntimeException("clReleaseCommandQueue not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // command queue
        );

        clReleaseCommandQueue = linker.downcallHandle(symbol, descriptor);
    }

    private void initclReleaseContext() {
        final MemorySegment symbol = opencl.find("clReleaseContext")
                .orElseThrow(() -> new RuntimeException("clReleaseContext not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // context
        );

        clReleaseContext = linker.downcallHandle(symbol, descriptor);
    }

    private void initclFinish() {
        final MemorySegment symbol = opencl.find("clFinish")
                .orElseThrow(() -> new RuntimeException("clFinish not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // command queue
        );

        clFinish = linker.downcallHandle(symbol, descriptor);
    }

    private void initclCreateBuffer() {
        final MemorySegment symbol = opencl.find("clCreateBuffer")
                .orElseThrow(() -> new RuntimeException("clCreateBuffer not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,  // cl_context
                ValueLayout.JAVA_LONG, // flags,
                ValueLayout.JAVA_LONG, // size,
                ValueLayout.ADDRESS, // host_ptr,
                ValueLayout.ADDRESS // errcode_ret,
        );

        clCreateBuffer = linker.downcallHandle(symbol, descriptor);
    }

    private void initclSetKernelArg() {
        final MemorySegment symbol = opencl.find("clSetKernelArg")
                .orElseThrow(() -> new RuntimeException("clSetKernelArg not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // kernel
                ValueLayout.JAVA_INT, // arg_index,
                ValueLayout.JAVA_LONG, // arg_size,
                ValueLayout.ADDRESS // arg_value,
        );

        clSetKernelArg = linker.downcallHandle(symbol, descriptor);
    }

    private void initclEnqueueNDRangeKernel() {
        final MemorySegment symbol = opencl.find("clEnqueueNDRangeKernel")
                .orElseThrow(() -> new RuntimeException("clEnqueueNDRangeKernel not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // command_queue
                ValueLayout.ADDRESS,  // kernel
                ValueLayout.JAVA_INT, // work_dim,
                ValueLayout.ADDRESS, // global_work_offset,
                ValueLayout.ADDRESS, // global_work_size,
                ValueLayout.ADDRESS, // local_work_size,
                ValueLayout.JAVA_INT, // num_events_in_wait_list,
                ValueLayout.ADDRESS, // event_wait_list,
                ValueLayout.ADDRESS // event,
        );

        clEnqueueNDRangeKernel = linker.downcallHandle(symbol, descriptor);
    }

    private void initclReleaseMemObject() {
        final MemorySegment symbol = opencl.find("clReleaseMemObject")
                .orElseThrow(() -> new RuntimeException("clReleaseMemObject not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS  // memobject
        );

        clReleaseMemObject = linker.downcallHandle(symbol, descriptor);

    }

    private void initclEnqueueReadBuffer() {
        final MemorySegment symbol = opencl.find("clEnqueueReadBuffer")
                .orElseThrow(() -> new RuntimeException("clEnqueueReadBuffer not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // command_queue
                ValueLayout.ADDRESS, // buffer
                ValueLayout.JAVA_BOOLEAN, // blocking_read
                ValueLayout.JAVA_LONG, // offset
                ValueLayout.JAVA_LONG, // size
                ValueLayout.ADDRESS, // ptr
                ValueLayout.JAVA_INT, // num_events_in_wait_list
                ValueLayout.ADDRESS, // event_wait_list
                ValueLayout.ADDRESS // event
        );

        clEnqueueReadBuffer = linker.downcallHandle(symbol, descriptor);
    }

    private void initclEnqueueWriteBuffer() {
        final MemorySegment symbol = opencl.find("clEnqueueWriteBuffer")
                .orElseThrow(() -> new RuntimeException("clEnqueueWriteBuffer not found"));

        final FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,  // command_queue
                ValueLayout.ADDRESS, // buffer
                ValueLayout.JAVA_BOOLEAN, // blocking_write
                ValueLayout.JAVA_LONG, // offset
                ValueLayout.JAVA_LONG, // size
                ValueLayout.ADDRESS, // ptr
                ValueLayout.JAVA_INT, // num_events_in_wait_list
                ValueLayout.ADDRESS, // event_wait_list
                ValueLayout.ADDRESS // event
        );

        clEnqueueWriteBuffer = linker.downcallHandle(symbol, descriptor);
    }

    /**
     * Obtain the list of available OpenCL platforms.
     *
     * @param count        The number of cl_platform_id entries that can be added to platforms. If platforms is not NULL, the count must be greater than zero.
     * @param platforms    Returns a list of OpenCL platforms found. The cl_platform_id values returned in platforms can be used to identify a specific OpenCL platform.
     * @param numPlatforms Returns the number of OpenCL platforms available. If numPlatforms is NULL, this argument is ignored.
     * @return CL_SUCCESS if the function is executed successfully, or CL_INVALID_VALUE if count is equal to zero and platforms is not NULL
     */
    public int clGetPlatformIDs(final int count, final MemorySegment platforms, final MemorySegment numPlatforms) {
        try {
            return (int) clGetPlatformIDs.invokeExact(count, platforms, numPlatforms);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Get information about the OpenCL platform.
     *
     * @param platform          The platform ID
     * @param paramName         The parameter name being queried
     * @param paramValueSize    The size in bytes of memory pointed to by paramValue
     * @param paramValue        A pointer to memory where the result is returned
     * @param paramValueSizeRet Returns the actual size in bytes of data copied to paramValue
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clGetPlatformInfo(final long platform, final int paramName, final long paramValueSize, final MemorySegment paramValue, final MemorySegment paramValueSizeRet) {
        try {
            return (int) clGetPlatformInfo.invokeExact(platform, paramName, paramValueSize, paramValue, paramValueSizeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Obtain the list of devices available on a platform.
     *
     * @param platform   The platform ID returned by clGetPlatformIDs
     * @param deviceType A bitfield identifying the type of OpenCL device
     * @param count      The number of cl_device_id entries that can be added to devices
     * @param devices    Returns a list of OpenCL devices found
     * @param numDevices Returns the number of OpenCL devices available that match deviceType
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clGetDeviceIDs(final long platform, final long deviceType, final int count, final MemorySegment devices, final MemorySegment numDevices) {
        try {
            return (int) clGetDeviceIDs.invokeExact(platform, deviceType, count, devices, numDevices);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Get information about an OpenCL device.
     *
     * @param device            The device ID
     * @param paramName         The parameter name being queried
     * @param paramValueSize    The size in bytes of memory pointed to by paramValue
     * @param paramValue        A pointer to memory where the result is returned
     * @param paramValueSizeRet Returns the actual size in bytes of data copied to paramValue
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clGetDeviceInfo(final long device, final int paramName, final long paramValueSize, final MemorySegment paramValue, final MemorySegment paramValueSizeRet) {
        try {
            return (int) clGetDeviceInfo.invokeExact(device, paramName, paramValueSize, paramValue, paramValueSizeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates an OpenCL context.
     *
     * @param properties Specifies a list of context property names and their corresponding values
     * @param numDevices Number of devices specified in devices
     * @param devices    A pointer to a list of unique devices returned by clGetDeviceIDs
     * @param callback   A callback function used by OpenCL to report information about errors
     * @param userData   Passed as the user_data argument when callback is called
     * @param errcodeRet Returns an error code
     * @return If successful, returns a valid OpenCL context. Otherwise, returns NULL.
     */
    public MemorySegment clCreateContext(final MemorySegment properties, final int numDevices, final MemorySegment devices, final MemorySegment callback, final MemorySegment userData, final MemorySegment errcodeRet) {
        try {
            return (MemorySegment) clCreateContext.invokeExact(properties, numDevices, devices, callback, userData, errcodeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a command-queue on a specific device.
     *
     * @param context    A valid OpenCL context
     * @param deviceId   A device associated with context
     * @param properties Specifies properties for the command-queue
     * @param errcodeRet Returns an error code
     * @return If successful, returns a valid OpenCL command-queue. Otherwise, returns NULL.
     */
    public MemorySegment clCreateCommandQueue(final MemorySegment context, final long deviceId, final MemorySegment properties, final MemorySegment errcodeRet) {
        try {
            return (MemorySegment) clCreateCommandQueue.invokeExact(context, deviceId, properties, errcodeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a program object for a context.
     *
     * @param context    A valid OpenCL context
     * @param count      The number of strings in strings
     * @param strings    An array of count pointers to optionally null-terminated character strings
     * @param lengths    An array with the number of chars in each string
     * @param errcodeRet Returns an error code
     * @return If successful, returns a valid OpenCL program object. Otherwise, returns NULL.
     */
    public MemorySegment clCreateProgramWithSource(final MemorySegment context, final int count, final MemorySegment strings, final MemorySegment lengths, final MemorySegment errcodeRet) {
        try {
            return (MemorySegment) clCreateProgramWithSource.invokeExact(context, count, strings, lengths, errcodeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Builds (compiles and links) a program executable from the program source or binary.
     *
     * @param program    The program object
     * @param numDevices The number of devices listed in device_list
     * @param deviceList A pointer to a list of device IDs
     * @param options    A string of OpenCL compiler options
     * @param callback   A function pointer to a notification routine
     * @param userData   Passed as an argument when callback is called
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clBuildProgram(final MemorySegment program, final int numDevices, final MemorySegment deviceList, final MemorySegment options, final MemorySegment callback, final MemorySegment userData) {
        try {
            return (int) clBuildProgram.invokeExact(program, numDevices, deviceList, options, callback, userData);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Returns build information for each device in the program object.
     *
     * @param program           The program object being queried
     * @param device            The device for which build information is being queried
     * @param paramName         The parameter name being queried
     * @param paramValueSize    The size in bytes of memory pointed to by paramValue
     * @param paramValue        A pointer to memory where the result is returned
     * @param paramValueSizeRet Returns the actual size in bytes of data copied to paramValue
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clGetProgramBuildInfo(final MemorySegment program, final long device, final int paramName, final long paramValueSize, final MemorySegment paramValue, final MemorySegment paramValueSizeRet) {
        try {
            return (int) clGetProgramBuildInfo.invokeExact(program, device, paramName, paramValueSize, paramValue, paramValueSizeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a kernel object.
     *
     * @param program    A program object with a successfully built executable
     * @param kernelName A function name in the program declared with the __kernel qualifier
     * @param errcodeRet Returns an error code
     * @return If successful, returns a valid kernel object. Otherwise, returns NULL.
     */
    public MemorySegment clCreateKernel(final MemorySegment program, final MemorySegment kernelName, final MemorySegment errcodeRet) {
        try {
            return (MemorySegment) clCreateKernel.invokeExact(program, kernelName, errcodeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decrements the kernel reference count.
     *
     * @param kernel The kernel object to release
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clReleaseKernel(final MemorySegment kernel) {
        try {
            return (int) clReleaseKernel.invokeExact(kernel);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decrements the program reference count.
     *
     * @param program The program object to release
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clReleaseProgram(final MemorySegment program) {
        try {
            return (int) clReleaseProgram.invokeExact(program);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decrements the command-queue reference count.
     *
     * @param commandQueue The command-queue to release
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clReleaseCommandQueue(final MemorySegment commandQueue) {
        try {
            return (int) clReleaseCommandQueue.invokeExact(commandQueue);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decrements the context reference count.
     *
     * @param context The context to release
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clReleaseContext(final MemorySegment context) {
        try {
            return (int) clReleaseContext.invokeExact(context);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Blocks until all previously queued OpenCL commands in a command-queue are issued and completed.
     *
     * @param commandQueue The command-queue to flush
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clFinish(final MemorySegment commandQueue) {
        try {
            return (int) clFinish.invokeExact(commandQueue);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a buffer object.
     *
     * @param context    A valid OpenCL context
     * @param flags      A bit-field that is used to specify allocation and usage information
     * @param size       The size in bytes of the buffer memory object to be allocated
     * @param hostPtr    A pointer to the buffer data that may already be allocated by the application
     * @param errcodeRet Returns an error code
     * @return If successful, returns a valid buffer object. Otherwise, returns NULL.
     */
    public MemorySegment clCreateBuffer(final MemorySegment context, final long flags, final long size, final MemorySegment hostPtr, final MemorySegment errcodeRet) {
        try {
            return (MemorySegment) clCreateBuffer.invokeExact(context, flags, size, hostPtr, errcodeRet);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Sets a kernel argument.
     *
     * @param kernel   The kernel object
     * @param argIndex The argument index
     * @param argSize  The size of the argument value
     * @param argValue A pointer to data that should be used as the argument value
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clSetKernelArg(final MemorySegment kernel, final int argIndex, final long argSize, final MemorySegment argValue) {
        try {
            return (int) clSetKernelArg.invokeExact(kernel, argIndex, argSize, argValue);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Enqueues a command to execute a kernel on a device.
     *
     * @param commandQueue        A valid command-queue
     * @param kernel              A valid kernel object
     * @param workDim             The number of dimensions used to specify the global work-items and work-items in the work-group
     * @param globalWorkOffset    Can be used to specify an array of workDim unsigned values that describe the offset used to calculate the global ID of a work-item
     * @param globalWorkSize      Points to an array of workDim unsigned values that describe the number of global work-items in workDim dimensions
     * @param localWorkSize       Points to an array of workDim unsigned values that describe the number of work-items that make up a work-group
     * @param numEventsInWaitList The number of events in eventWaitList
     * @param eventWaitList       Specifies events that need to complete before this particular command can be executed
     * @param event               Returns an event object that identifies this particular kernel execution instance
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clEnqueueNDRangeKernel(final MemorySegment commandQueue, final MemorySegment kernel, final int workDim, final MemorySegment globalWorkOffset, final MemorySegment globalWorkSize, final MemorySegment localWorkSize, final int numEventsInWaitList, final MemorySegment eventWaitList, final MemorySegment event) {
        try {
            return (int) clEnqueueNDRangeKernel.invokeExact(commandQueue, kernel, workDim, globalWorkOffset, globalWorkSize, localWorkSize, numEventsInWaitList, eventWaitList, event);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Decrements the memory object reference count.
     *
     * @param memObject The memory object to release
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clReleaseMemObject(final MemorySegment memObject) {
        try {
            return (int) clReleaseMemObject.invokeExact(memObject);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Enqueues a command to read from a buffer object to host memory.
     *
     * @param commandQueue        A valid command-queue
     * @param buffer              A valid buffer object
     * @param blockingRead        Indicates if the read is blocking or non-blocking
     * @param offset              The offset in bytes in the buffer object to read from
     * @param size                The size in bytes of data being read
     * @param ptr                 The pointer to buffer in host memory to read into
     * @param numEventsInWaitList The number of events in eventWaitList
     * @param eventWaitList       Specifies events that need to complete before this particular command can be executed
     * @param event               Returns an event object that identifies this particular read command
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clEnqueueReadBuffer(final MemorySegment commandQueue, final MemorySegment buffer, final boolean blockingRead, final long offset, final long size, final MemorySegment ptr, final int numEventsInWaitList, final MemorySegment eventWaitList, final MemorySegment event) {
        try {
            return (int) clEnqueueReadBuffer.invokeExact(commandQueue, buffer, blockingRead, offset, size, ptr, numEventsInWaitList, eventWaitList, event);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Enqueues a command to write to a buffer object from host memory.
     *
     * @param commandQueue        A valid command-queue
     * @param buffer              A valid buffer object
     * @param blockingWrite       Indicates if the write is blocking or non-blocking
     * @param offset              The offset in bytes in the buffer object to write to
     * @param size                The size in bytes of data being written
     * @param ptr                 The pointer to buffer in host memory where data is to be written from
     * @param numEventsInWaitList The number of events in eventWaitList
     * @param eventWaitList       Specifies events that need to complete before this particular command can be executed
     * @param event               Returns an event object that identifies this particular write command
     * @return CL_SUCCESS if successful. Otherwise, returns error code.
     */
    public int clEnqueueWriteBuffer(final MemorySegment commandQueue, final MemorySegment buffer, final boolean blockingWrite, final long offset, final long size, final MemorySegment ptr, final int numEventsInWaitList, final MemorySegment eventWaitList, final MemorySegment event) {
        try {
            return (int) clEnqueueWriteBuffer.invokeExact(commandQueue, buffer, blockingWrite, offset, size, ptr, numEventsInWaitList, eventWaitList, event);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }
}