package de.mirkosertic.metair.opencl;

public interface OpenCLConstants {

    // cl_platform.h constants
    int CL_CHAR_BIT         = 8;
    int CL_SCHAR_MAX        = 127;
    int CL_SCHAR_MIN        = (-127-1);
    int CL_CHAR_MAX         = CL_SCHAR_MAX;
    int CL_CHAR_MIN         = CL_SCHAR_MIN;
    int CL_UCHAR_MAX        = 255;
    int CL_SHRT_MAX         = 32767;
    int CL_SHRT_MIN         = (-32767-1);
    int CL_USHRT_MAX        = 65535;
    int CL_INT_MAX          = 2147483647;
    int CL_INT_MIN          = (-2147483647-1);
    long CL_UINT_MAX        = 0xffffffff;
    long CL_LONG_MAX        = 0x7FFFFFFFFFFFFFFFL;
    long CL_LONG_MIN        = -0x7FFFFFFFFFFFFFFFL - 1L;
    long CL_ULONG_MAX       = 0xFFFFFFFFFFFFFFFFL;

    int CL_FLT_DIG          = 6;
    int CL_FLT_MANT_DIG     = 24;
    int CL_FLT_MAX_10_EXP   = 38;
    int CL_FLT_MAX_EXP      = 128;
    int CL_FLT_MIN_10_EXP   = -37;
    int CL_FLT_MIN_EXP      = -125;
    int CL_FLT_RADIX        = 2;
    float CL_FLT_MAX        = 0x1.fffffep127f;
    float CL_FLT_MIN        = 0x1.0p-126f;
    float CL_FLT_EPSILON    = 0x1.0p-23f;

    int CL_DBL_DIG          = 15;
    int CL_DBL_MANT_DIG     = 53;
    int CL_DBL_MAX_10_EXP   = 308;
    int CL_DBL_MAX_EXP      = 1024;
    int CL_DBL_MIN_10_EXP   = -307;
    int CL_DBL_MIN_EXP      = -1021;
    int CL_DBL_RADIX        = 2;
    double CL_DBL_MAX       = 0x1.fffffffffffffp1023;
    double CL_DBL_MIN       = 0x1.0p-1022;
    double CL_DBL_EPSILON   = 0x1.0p-52;



    //=== Constants ==========================================================

    // Error codes
    int CL_SUCCESS                                  = 0;
    int CL_DEVICE_NOT_FOUND                         = -1;
    int CL_DEVICE_NOT_AVAILABLE                     = -2;
    int CL_COMPILER_NOT_AVAILABLE                   = -3;
    int CL_MEM_OBJECT_ALLOCATION_FAILURE            = -4;
    int CL_OUT_OF_RESOURCES                         = -5;
    int CL_OUT_OF_HOST_MEMORY                       = -6;
    int CL_PROFILING_INFO_NOT_AVAILABLE             = -7;
    int CL_MEM_COPY_OVERLAP                         = -8;
    int CL_IMAGE_FORMAT_MISMATCH                    = -9;
    int CL_IMAGE_FORMAT_NOT_SUPPORTED               = -10;
    int CL_BUILD_PROGRAM_FAILURE                    = -11;
    int CL_MAP_FAILURE                              = -12;
    int CL_MISALIGNED_SUB_BUFFER_OFFSET             = -13;
    int CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST= -14;
    // OPENCL_1_2
    int CL_COMPILE_PROGRAM_FAILURE                  = -15;
    int CL_LINKER_NOT_AVAILABLE                     = -16;
    int CL_LINK_PROGRAM_FAILURE                     = -17;
    int CL_DEVICE_PARTITION_FAILED                  = -18;
    int CL_KERNEL_ARG_INFO_NOT_AVAILABLE            = -19;

    int CL_INVALID_VALUE                            = -30;
    int CL_INVALID_DEVICE_TYPE                      = -31;
    int CL_INVALID_PLATFORM                         = -32;
    int CL_INVALID_DEVICE                           = -33;
    int CL_INVALID_CONTEXT                          = -34;
    int CL_INVALID_QUEUE_PROPERTIES                 = -35;
    int CL_INVALID_COMMAND_QUEUE                    = -36;
    int CL_INVALID_HOST_PTR                         = -37;
    int CL_INVALID_MEM_OBJECT                       = -38;
    int CL_INVALID_IMAGE_FORMAT_DESCRIPTOR          = -39;
    int CL_INVALID_IMAGE_SIZE                       = -40;
    int CL_INVALID_SAMPLER                          = -41;
    int CL_INVALID_BINARY                           = -42;
    int CL_INVALID_BUILD_OPTIONS                    = -43;
    int CL_INVALID_PROGRAM                          = -44;
    int CL_INVALID_PROGRAM_EXECUTABLE               = -45;
    int CL_INVALID_KERNEL_NAME                      = -46;
    int CL_INVALID_KERNEL_DEFINITION                = -47;
    int CL_INVALID_KERNEL                           = -48;
    int CL_INVALID_ARG_INDEX                        = -49;
    int CL_INVALID_ARG_VALUE                        = -50;
    int CL_INVALID_ARG_SIZE                         = -51;
    int CL_INVALID_KERNEL_ARGS                      = -52;
    int CL_INVALID_WORK_DIMENSION                   = -53;
    int CL_INVALID_WORK_GROUP_SIZE                  = -54;
    int CL_INVALID_WORK_ITEM_SIZE                   = -55;
    int CL_INVALID_GLOBAL_OFFSET                    = -56;
    int CL_INVALID_EVENT_WAIT_LIST                  = -57;
    int CL_INVALID_EVENT                            = -58;
    int CL_INVALID_OPERATION                        = -59;
    int CL_INVALID_GL_OBJECT                        = -60;
    int CL_INVALID_BUFFER_SIZE                      = -61;
    int CL_INVALID_MIP_LEVEL                        = -62;
    int CL_INVALID_GLOBAL_WORK_SIZE                 = -63;
    // OPENCL_1_2
    int CL_INVALID_PROPERTY                         = -64;
    int CL_INVALID_IMAGE_DESCRIPTOR                 = -65;
    int CL_INVALID_COMPILER_OPTIONS                 = -66;
    int CL_INVALID_LINKER_OPTIONS                   = -67;
    int CL_INVALID_DEVICE_PARTITION_COUNT           = -68;

    // OPENCL_2_0
    int CL_INVALID_PIPE_SIZE                        = -69;
    int CL_INVALID_DEVICE_QUEUE                     = -70;


    int CL_JOCL_INTERNAL_ERROR                      = -16384;
    int CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR      = -1000;
    int CL_PLATFORM_NOT_FOUND_KHR                   = -1001;


    // cl_bool
    boolean CL_TRUE = true;
    boolean CL_FALSE = false;
    // OPENCL_1_2
    boolean CL_BLOCKING = CL_TRUE;
    boolean CL_NON_BLOCKING = CL_FALSE;

    // cl_platform_info
    int CL_PLATFORM_PROFILE = 0x0900;
    int CL_PLATFORM_VERSION = 0x0901;
    int CL_PLATFORM_NAME = 0x0902;
    int CL_PLATFORM_VENDOR = 0x0903;
    int CL_PLATFORM_EXTENSIONS = 0x0904;
    // CL_EXT
    int CL_PLATFORM_ICD_SUFFIX_KHR = 0x0920;

    // cl_device_type - bitfield
    long CL_DEVICE_TYPE_DEFAULT = (1 << 0);
    long CL_DEVICE_TYPE_CPU = (1 << 1);
    long CL_DEVICE_TYPE_GPU = (1 << 2);
    long CL_DEVICE_TYPE_ACCELERATOR = (1 << 3);
    long CL_DEVICE_TYPE_ALL = 0xFFFFFFFF;
    // OPENCL_1_2
    long CL_DEVICE_TYPE_CUSTOM = (1 << 4);

    // cl_device_info
    int CL_DEVICE_TYPE = 0x1000;
    int CL_DEVICE_VENDOR_ID = 0x1001;
    int CL_DEVICE_MAX_COMPUTE_UNITS = 0x1002;
    int CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS = 0x1003;
    int CL_DEVICE_MAX_WORK_GROUP_SIZE = 0x1004;
    int CL_DEVICE_MAX_WORK_ITEM_SIZES = 0x1005;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_CHAR = 0x1006;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_SHORT = 0x1007;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_INT = 0x1008;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_LONG = 0x1009;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_FLOAT = 0x100A;
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_DOUBLE = 0x100B;
    int CL_DEVICE_MAX_CLOCK_FREQUENCY = 0x100C;
    int CL_DEVICE_ADDRESS_BITS = 0x100D;
    int CL_DEVICE_MAX_READ_IMAGE_ARGS = 0x100E;
    int CL_DEVICE_MAX_WRITE_IMAGE_ARGS = 0x100F;
    int CL_DEVICE_MAX_MEM_ALLOC_SIZE = 0x1010;
    int CL_DEVICE_IMAGE2D_MAX_WIDTH = 0x1011;
    int CL_DEVICE_IMAGE2D_MAX_HEIGHT = 0x1012;
    int CL_DEVICE_IMAGE3D_MAX_WIDTH = 0x1013;
    int CL_DEVICE_IMAGE3D_MAX_HEIGHT = 0x1014;
    int CL_DEVICE_IMAGE3D_MAX_DEPTH = 0x1015;
    int CL_DEVICE_IMAGE_SUPPORT = 0x1016;
    int CL_DEVICE_MAX_PARAMETER_SIZE = 0x1017;
    int CL_DEVICE_MAX_SAMPLERS = 0x1018;
    int CL_DEVICE_MEM_BASE_ADDR_ALIGN = 0x1019;
    int CL_DEVICE_MIN_DATA_TYPE_ALIGN_SIZE = 0x101A;
    int CL_DEVICE_SINGLE_FP_CONFIG = 0x101B;
    int CL_DEVICE_GLOBAL_MEM_CACHE_TYPE = 0x101C;
    int CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE = 0x101D;
    int CL_DEVICE_GLOBAL_MEM_CACHE_SIZE = 0x101E;
    int CL_DEVICE_GLOBAL_MEM_SIZE = 0x101F;
    int CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE = 0x1020;
    int CL_DEVICE_MAX_CONSTANT_ARGS = 0x1021;
    int CL_DEVICE_LOCAL_MEM_TYPE = 0x1022;
    int CL_DEVICE_LOCAL_MEM_SIZE = 0x1023;
    int CL_DEVICE_ERROR_CORRECTION_SUPPORT = 0x1024;
    int CL_DEVICE_PROFILING_TIMER_RESOLUTION = 0x1025;
    int CL_DEVICE_ENDIAN_LITTLE = 0x1026;
    int CL_DEVICE_AVAILABLE = 0x1027;
    int CL_DEVICE_COMPILER_AVAILABLE = 0x1028;
    int CL_DEVICE_EXECUTION_CAPABILITIES = 0x1029;

    /**
     * @deprecated As of OpenCL 2.0, replaced by
     * {@link #CL_DEVICE_QUEUE_ON_HOST_PROPERTIES}
     */

    int CL_DEVICE_QUEUE_PROPERTIES = 0x102A;
    int CL_DEVICE_NAME = 0x102B;
    int CL_DEVICE_VENDOR = 0x102C;
    int CL_DRIVER_VERSION = 0x102D;
    int CL_DEVICE_PROFILE = 0x102E;
    int CL_DEVICE_VERSION = 0x102F;
    int CL_DEVICE_EXTENSIONS = 0x1030;
    int CL_DEVICE_PLATFORM = 0x1031;

    // OPENCL_1_1
    int CL_DEVICE_PREFERRED_VECTOR_WIDTH_HALF       = 0x1034;
    /**
     * @deprecated As of OpenCL 2.0
     */
    int CL_DEVICE_HOST_UNIFIED_MEMORY               = 0x1035;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_CHAR          = 0x1036;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_SHORT         = 0x1037;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_INT           = 0x1038;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_LONG          = 0x1039;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_FLOAT         = 0x103A;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_DOUBLE        = 0x103B;
    int CL_DEVICE_NATIVE_VECTOR_WIDTH_HALF          = 0x103C;
    int CL_DEVICE_OPENCL_C_VERSION                  = 0x103D;

    // OPENCL_1_2
    int CL_DEVICE_LINKER_AVAILABLE                  = 0x103E;
    int CL_DEVICE_BUILT_IN_KERNELS                  = 0x103F;
    int CL_DEVICE_IMAGE_MAX_BUFFER_SIZE             = 0x1040;
    int CL_DEVICE_IMAGE_MAX_ARRAY_SIZE              = 0x1041;
    int CL_DEVICE_PARENT_DEVICE                     = 0x1042;
    int CL_DEVICE_PARTITION_MAX_SUB_DEVICES         = 0x1043;
    int CL_DEVICE_PARTITION_PROPERTIES              = 0x1044;
    int CL_DEVICE_PARTITION_AFFINITY_DOMAIN         = 0x1045;
    int CL_DEVICE_PARTITION_TYPE                    = 0x1046;
    int CL_DEVICE_REFERENCE_COUNT                   = 0x1047;
    int CL_DEVICE_PREFERRED_INTEROP_USER_SYNC       = 0x1048;
    int CL_DEVICE_PRINTF_BUFFER_SIZE                = 0x1049;

    // OPENCL_2_0
    int CL_DEVICE_QUEUE_ON_HOST_PROPERTIES             = 0x102A;
    int CL_DEVICE_IMAGE_PITCH_ALIGNMENT                = 0x104A;
    int CL_DEVICE_IMAGE_BASE_ADDRESS_ALIGNMENT         = 0x104B;
    int CL_DEVICE_MAX_READ_WRITE_IMAGE_ARGS            = 0x104C;
    int CL_DEVICE_MAX_GLOBAL_VARIABLE_SIZE             = 0x104D;
    int CL_DEVICE_QUEUE_ON_DEVICE_PROPERTIES           = 0x104E;
    int CL_DEVICE_QUEUE_ON_DEVICE_PREFERRED_SIZE       = 0x104F;
    int CL_DEVICE_QUEUE_ON_DEVICE_MAX_SIZE             = 0x1050;
    int CL_DEVICE_MAX_ON_DEVICE_QUEUES                 = 0x1051;
    int CL_DEVICE_MAX_ON_DEVICE_EVENTS                 = 0x1052;
    int CL_DEVICE_SVM_CAPABILITIES                     = 0x1053;
    int CL_DEVICE_GLOBAL_VARIABLE_PREFERRED_TOTAL_SIZE = 0x1054;
    int CL_DEVICE_MAX_PIPE_ARGS                        = 0x1055;
    int CL_DEVICE_PIPE_MAX_ACTIVE_RESERVATIONS         = 0x1056;
    int CL_DEVICE_PIPE_MAX_PACKET_SIZE                 = 0x1057;
    int CL_DEVICE_PREFERRED_PLATFORM_ATOMIC_ALIGNMENT  = 0x1058;
    int CL_DEVICE_PREFERRED_GLOBAL_ATOMIC_ALIGNMENT    = 0x1059;
    int CL_DEVICE_PREFERRED_LOCAL_ATOMIC_ALIGNMENT     = 0x105A;

    // CL_EXT
    int CL_DEVICE_DOUBLE_FP_CONFIG                  = 0x1032;
    int CL_DEVICE_HALF_FP_CONFIG                    = 0x1033;


    // cl_device_address_info - bitfield
    long CL_DEVICE_ADDRESS_32_BITS = (1 << 0);
    long CL_DEVICE_ADDRESS_64_BITS = (1 << 1);

    // cl_device_fp_config - bitfield
    long CL_FP_DENORM = (1 << 0);
    long CL_FP_INF_NAN = (1 << 1);
    long CL_FP_ROUND_TO_NEAREST = (1 << 2);
    long CL_FP_ROUND_TO_ZERO = (1 << 3);
    long CL_FP_ROUND_TO_INF = (1 << 4);
    long CL_FP_FMA = (1 << 5);
    long CL_FP_SOFT_FLOAT = (1 << 6);
    // OPENCL_1_2
    long CL_FP_CORRECTLY_ROUNDED_DIVIDE_SQRT = (1 << 7);

    // cl_device_mem_cache_type
    int CL_NONE = 0x0;
    int CL_READ_ONLY_CACHE = 0x1;
    int CL_READ_WRITE_CACHE = 0x2;

    // cl_device_local_mem_type
    int CL_LOCAL = 0x1;
    int CL_GLOBAL = 0x2;

    // cl_device_exec_capabilities - bitfield
    long CL_EXEC_KERNEL = (1 << 0);
    long CL_EXEC_NATIVE_KERNEL = (1 << 1);

    // cl_command_queue_properties - bitfield
    long CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE = (1 << 0);
    long CL_QUEUE_PROFILING_ENABLE = (1 << 1);
    // OPENCL_2_0
    long CL_QUEUE_ON_DEVICE = (1 << 2);
    long CL_QUEUE_ON_DEVICE_DEFAULT =(1 << 3);


    // cl_context_info
    int CL_CONTEXT_REFERENCE_COUNT = 0x1080;
    int CL_CONTEXT_DEVICES         = 0x1081;
    int CL_CONTEXT_PROPERTIES      = 0x1082;
    int CL_CONTEXT_NUM_DEVICES     = 0x1083;

    // cl_context_properties
    int CL_CONTEXT_PLATFORM          = 0x1084;
    // OPENCL_1_2
    int CL_CONTEXT_INTEROP_USER_SYNC = 0x1085;

    // OPENCL_1_2
    /* cl_device_partition_property */
    int  CL_DEVICE_PARTITION_EQUALLY                = 0x1086;
    int  CL_DEVICE_PARTITION_BY_COUNTS              = 0x1087;
    int  CL_DEVICE_PARTITION_BY_COUNTS_LIST_END     = 0x0;
    int  CL_DEVICE_PARTITION_BY_AFFINITY_DOMAIN     = 0x1088;

    // OPENCL_1_2
    /* cl_device_affinity_domain - bitfield */
    long CL_DEVICE_AFFINITY_DOMAIN_NUMA               = (1 << 0);
    long CL_DEVICE_AFFINITY_DOMAIN_L4_CACHE           = (1 << 1);
    long CL_DEVICE_AFFINITY_DOMAIN_L3_CACHE           = (1 << 2);
    long CL_DEVICE_AFFINITY_DOMAIN_L2_CACHE           = (1 << 3);
    long CL_DEVICE_AFFINITY_DOMAIN_L1_CACHE           = (1 << 4);
    long CL_DEVICE_AFFINITY_DOMAIN_NEXT_PARTITIONABLE = (1 << 5);

    // OPENCL_2_0
    /* cl_device_svm_capabilities - bitfield */
    long CL_DEVICE_SVM_COARSE_GRAIN_BUFFER           = (1 << 0);
    long CL_DEVICE_SVM_FINE_GRAIN_BUFFER             = (1 << 1);
    long CL_DEVICE_SVM_FINE_GRAIN_SYSTEM             = (1 << 2);
    long CL_DEVICE_SVM_ATOMICS                       = (1 << 3);


    // cl_command_queue_info
    int CL_QUEUE_CONTEXT = 0x1090;
    int CL_QUEUE_DEVICE = 0x1091;
    int CL_QUEUE_REFERENCE_COUNT = 0x1092;
    int CL_QUEUE_PROPERTIES = 0x1093;
    // OPENCL_2_0
    int CL_QUEUE_SIZE = 0x1094;

    // cl_mem_flags - bitfield
    long CL_MEM_READ_WRITE = (1 << 0);
    long CL_MEM_WRITE_ONLY = (1 << 1);
    long CL_MEM_READ_ONLY = (1 << 2);
    long CL_MEM_USE_HOST_PTR = (1 << 3);
    long CL_MEM_ALLOC_HOST_PTR = (1 << 4);
    long CL_MEM_COPY_HOST_PTR = (1 << 5);
    // OPENCL_1_2
    long  CL_MEM_HOST_WRITE_ONLY =(1 << 7);
    long  CL_MEM_HOST_READ_ONLY = (1 << 8);
    long  CL_MEM_HOST_NO_ACCESS = (1 << 9);
    // OPENCL_2_0
    long CL_MEM_SVM_FINE_GRAIN_BUFFER = (1 << 10);   /* used by cl_svm_mem_flags only */
    long CL_MEM_SVM_ATOMICS = (1 << 11);   /* used by cl_svm_mem_flags only */

    // OPENCL_1_2
    /* cl_mem_migration_flags - bitfield */
    long  CL_MIGRATE_MEM_OBJECT_HOST              = (1 << 0);
    long  CL_MIGRATE_MEM_OBJECT_CONTENT_UNDEFINED = (1 << 1);

    // cl_channel_order
    int CL_R = 0x10B0;
    int CL_A = 0x10B1;
    int CL_RG = 0x10B2;
    int CL_RA = 0x10B3;
    int CL_RGB = 0x10B4;
    int CL_RGBA = 0x10B5;
    int CL_BGRA = 0x10B6;
    int CL_ARGB = 0x10B7;
    int CL_INTENSITY = 0x10B8;
    int CL_LUMINANCE = 0x10B9;
    // OPENCL_1_1
    int CL_Rx                                       = 0x10BA;
    int CL_RGx                                      = 0x10BB;
    int CL_RGBx                                     = 0x10BC;
    // OPENCL_2_0
    int CL_DEPTH                                    = 0x10BD;
    int CL_DEPTH_STENCIL                            = 0x10BE;
    int CL_sRGB                                     = 0x10BF;
    int CL_sRGBx                                    = 0x10C0;
    int CL_sRGBA                                    = 0x10C1;
    int CL_sBGRA                                    = 0x10C2;
    int CL_ABGR                                     = 0x10C3;


    // cl_channel_type
    int CL_SNORM_INT8 = 0x10D0;
    int CL_SNORM_INT16 = 0x10D1;
    int CL_UNORM_INT8 = 0x10D2;
    int CL_UNORM_INT16 = 0x10D3;
    int CL_UNORM_SHORT_565 = 0x10D4;
    int CL_UNORM_SHORT_555 = 0x10D5;
    int CL_UNORM_INT_101010 = 0x10D6;
    int CL_SIGNED_INT8 = 0x10D7;
    int CL_SIGNED_INT16 = 0x10D8;
    int CL_SIGNED_INT32 = 0x10D9;
    int CL_UNSIGNED_INT8 = 0x10DA;
    int CL_UNSIGNED_INT16 = 0x10DB;
    int CL_UNSIGNED_INT32 = 0x10DC;
    int CL_HALF_FLOAT = 0x10DD;
    int CL_FLOAT = 0x10DE;
    // OPENCL_2_0
    int CL_UNORM_INT24 = 0x10DF;


    // cl_mem_object_type
    int CL_MEM_OBJECT_BUFFER = 0x10F0;
    int CL_MEM_OBJECT_IMAGE2D = 0x10F1;
    int CL_MEM_OBJECT_IMAGE3D = 0x10F2;
    // OPENCL_1_2
    int CL_MEM_OBJECT_IMAGE2D_ARRAY  = 0x10F3;
    int CL_MEM_OBJECT_IMAGE1D        = 0x10F4;
    int CL_MEM_OBJECT_IMAGE1D_ARRAY  = 0x10F5;
    int CL_MEM_OBJECT_IMAGE1D_BUFFER = 0x10F6;
    // OPENCL_2_0
    int CL_MEM_OBJECT_PIPE = 0x10F7;


    // cl_mem_info
    int CL_MEM_TYPE = 0x1100;
    int CL_MEM_FLAGS = 0x1101;
    int CL_MEM_SIZE = 0x1102;
    int CL_MEM_HOST_PTR = 0x1103;
    int CL_MEM_MAP_COUNT = 0x1104;
    int CL_MEM_REFERENCE_COUNT = 0x1105;
    int CL_MEM_CONTEXT = 0x1106;
    // OPENCL_1_1
    int CL_MEM_ASSOCIATED_MEMOBJECT                 = 0x1107;
    int CL_MEM_OFFSET                               = 0x1108;
    // OPENCL_2_0
    int CL_MEM_USES_SVM_POINTER                     = 0x1109;


    // cl_image_info
    int CL_IMAGE_FORMAT = 0x1110;
    int CL_IMAGE_ELEMENT_SIZE = 0x1111;
    int CL_IMAGE_ROW_PITCH = 0x1112;
    int CL_IMAGE_SLICE_PITCH = 0x1113;
    int CL_IMAGE_WIDTH = 0x1114;
    int CL_IMAGE_HEIGHT = 0x1115;
    int CL_IMAGE_DEPTH = 0x1116;
    // OPENCL_1_2
    int CL_IMAGE_ARRAY_SIZE     = 0x1117;
    int CL_IMAGE_BUFFER         = 0x1118;
    int CL_IMAGE_NUM_MIP_LEVELS = 0x1119;
    int CL_IMAGE_NUM_SAMPLES    = 0x111A;

    // OPENCL_2_0
    // cl_pipe_info - uint
    int CL_PIPE_PACKET_SIZE                         = 0x1120;
    int CL_PIPE_MAX_PACKETS                         = 0x1121;


    // cl_addressing_mode
    int CL_ADDRESS_NONE = 0x1130;
    int CL_ADDRESS_CLAMP_TO_EDGE = 0x1131;
    int CL_ADDRESS_CLAMP = 0x1132;
    int CL_ADDRESS_REPEAT = 0x1133;
    // OPENCL_1_1
    int CL_ADDRESS_MIRRORED_REPEAT                  = 0x1134;

    // cl_filter_mode
    int CL_FILTER_NEAREST = 0x1140;
    int CL_FILTER_LINEAR = 0x1141;

    // cl_sampler_info
    int CL_SAMPLER_REFERENCE_COUNT = 0x1150;
    int CL_SAMPLER_CONTEXT = 0x1151;
    int CL_SAMPLER_NORMALIZED_COORDS = 0x1152;
    int CL_SAMPLER_ADDRESSING_MODE = 0x1153;
    int CL_SAMPLER_FILTER_MODE = 0x1154;
    // OPENCL_2_0
    int CL_SAMPLER_MIP_FILTER_MODE                  = 0x1155;
    int CL_SAMPLER_LOD_MIN                          = 0x1156;
    int CL_SAMPLER_LOD_MAX                          = 0x1157;


    // cl_map_flags - bitfield
    long CL_MAP_READ = (1 << 0);
    long CL_MAP_WRITE = (1 << 1);
    // OPENCL_1_2
    long CL_MAP_WRITE_INVALIDATE_REGION = (1 << 2);

    // cl_program_info
    int CL_PROGRAM_REFERENCE_COUNT = 0x1160;
    int CL_PROGRAM_CONTEXT = 0x1161;
    int CL_PROGRAM_NUM_DEVICES = 0x1162;
    int CL_PROGRAM_DEVICES = 0x1163;
    int CL_PROGRAM_SOURCE = 0x1164;
    int CL_PROGRAM_BINARY_SIZES = 0x1165;
    int CL_PROGRAM_BINARIES = 0x1166;
    // OPENCL_1_2
    int CL_PROGRAM_NUM_KERNELS  = 0x1167;
    int CL_PROGRAM_KERNEL_NAMES = 0x1168;

    // cl_program_build_info
    int CL_PROGRAM_BUILD_STATUS = 0x1181;
    int CL_PROGRAM_BUILD_OPTIONS = 0x1182;
    int CL_PROGRAM_BUILD_LOG = 0x1183;
    // OPENCL_1_2
    int CL_PROGRAM_BINARY_TYPE = 0x1184;
    // OPENCL_2_0
    int CL_PROGRAM_BUILD_GLOBAL_VARIABLE_TOTAL_SIZE = 0x1185;


    /* cl_program_binary_type */
    // OPENCL_1_2
    int CL_PROGRAM_BINARY_TYPE_NONE            = 0x0;
    int CL_PROGRAM_BINARY_TYPE_COMPILED_OBJECT = 0x1;
    int CL_PROGRAM_BINARY_TYPE_LIBRARY         = 0x2;
    int CL_PROGRAM_BINARY_TYPE_EXECUTABLE      = 0x4;

    // cl_build_status
    int CL_BUILD_SUCCESS = 0;
    int CL_BUILD_NONE = -1;
    int CL_BUILD_ERROR = -2;
    int CL_BUILD_IN_PROGRESS = -3;

    // cl_kernel_info
    int CL_KERNEL_FUNCTION_NAME = 0x1190;
    int CL_KERNEL_NUM_ARGS = 0x1191;
    int CL_KERNEL_REFERENCE_COUNT = 0x1192;
    int CL_KERNEL_CONTEXT = 0x1193;
    int CL_KERNEL_PROGRAM = 0x1194;
    // OPENCL_1_2
    int CL_KERNEL_ATTRIBUTES = 0x1195;

    /* cl_kernel_arg_info */
    // OPENCL_1_2
    int CL_KERNEL_ARG_ADDRESS_QUALIFIER = 0x1196;
    int CL_KERNEL_ARG_ACCESS_QUALIFIER  = 0x1197;
    int CL_KERNEL_ARG_TYPE_NAME         = 0x1198;
    int CL_KERNEL_ARG_TYPE_QUALIFIER    = 0x1199;
    int CL_KERNEL_ARG_NAME              = 0x119A;

    /* cl_kernel_arg_address_qualifier */
    // OPENCL_1_2
    int CL_KERNEL_ARG_ADDRESS_GLOBAL    = 0x119B;
    int CL_KERNEL_ARG_ADDRESS_LOCAL     = 0x119C;
    int CL_KERNEL_ARG_ADDRESS_CONSTANT  = 0x119D;
    int CL_KERNEL_ARG_ADDRESS_PRIVATE   = 0x119E;

    /* cl_kernel_arg_access_qualifier */
    // OPENCL_1_2
    int CL_KERNEL_ARG_ACCESS_READ_ONLY  = 0x11A0;
    int CL_KERNEL_ARG_ACCESS_WRITE_ONLY = 0x11A1;
    int CL_KERNEL_ARG_ACCESS_READ_WRITE = 0x11A2;
    int CL_KERNEL_ARG_ACCESS_NONE       = 0x11A3;

    /* cl_kernel_arg_type_qualifer - bitfield */
    // OPENCL_1_2
    long CL_KERNEL_ARG_TYPE_NONE      = 0;
    long CL_KERNEL_ARG_TYPE_CONST     = (1 << 0);
    long CL_KERNEL_ARG_TYPE_RESTRICT  = (1 << 1);
    long CL_KERNEL_ARG_TYPE_VOLATILE  = (1 << 2);
    // OPENCL_2_0
    long CL_KERNEL_ARG_TYPE_PIPE      = (1 << 3);


    // cl_kernel_work_group_info
    int CL_KERNEL_WORK_GROUP_SIZE = 0x11B0;
    int CL_KERNEL_COMPILE_WORK_GROUP_SIZE = 0x11B1;
    int CL_KERNEL_LOCAL_MEM_SIZE = 0x11B2;
    // OPENCL_1_1
    int CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE = 0x11B3;
    int CL_KERNEL_PRIVATE_MEM_SIZE                   = 0x11B4;
    // OPENCL_1_2
    int CL_KERNEL_GLOBAL_WORK_SIZE = 0x11B5;

    // OPENCL_2_0
    /* cl_kernel_exec_info - uint */
    int CL_KERNEL_EXEC_INFO_SVM_PTRS               = 0x11B6;
    int CL_KERNEL_EXEC_INFO_SVM_FINE_GRAIN_SYSTEM  = 0x11B7;


    // cl_event_info
    int CL_EVENT_COMMAND_QUEUE = 0x11D0;
    int CL_EVENT_COMMAND_TYPE = 0x11D1;
    int CL_EVENT_REFERENCE_COUNT = 0x11D2;
    int CL_EVENT_COMMAND_EXECUTION_STATUS = 0x11D3;
    // OPENCL_1_1
    int CL_EVENT_CONTEXT                            = 0x11D4;

    // cl_command_type
    int CL_COMMAND_NDRANGE_KERNEL = 0x11F0;
    int CL_COMMAND_TASK = 0x11F1;
    int CL_COMMAND_NATIVE_KERNEL = 0x11F2;
    int CL_COMMAND_READ_BUFFER = 0x11F3;
    int CL_COMMAND_WRITE_BUFFER = 0x11F4;
    int CL_COMMAND_COPY_BUFFER = 0x11F5;
    int CL_COMMAND_READ_IMAGE = 0x11F6;
    int CL_COMMAND_WRITE_IMAGE = 0x11F7;
    int CL_COMMAND_COPY_IMAGE = 0x11F8;
    int CL_COMMAND_COPY_IMAGE_TO_BUFFER = 0x11F9;
    int CL_COMMAND_COPY_BUFFER_TO_IMAGE = 0x11FA;
    int CL_COMMAND_MAP_BUFFER = 0x11FB;
    int CL_COMMAND_MAP_IMAGE = 0x11FC;
    int CL_COMMAND_UNMAP_MEM_OBJECT = 0x11FD;
    int CL_COMMAND_MARKER = 0x11FE;
    int CL_COMMAND_ACQUIRE_GL_OBJECTS = 0x11FF;
    int CL_COMMAND_RELEASE_GL_OBJECTS = 0x1200;
    // OPENCL_1_1
    int CL_COMMAND_READ_BUFFER_RECT                 = 0x1201;
    int CL_COMMAND_WRITE_BUFFER_RECT                = 0x1202;
    int CL_COMMAND_COPY_BUFFER_RECT                 = 0x1203;
    int CL_COMMAND_USER                             = 0x1204;
    // OPENCL_1_2
    int CL_COMMAND_BARRIER                          = 0x1205;
    int CL_COMMAND_MIGRATE_MEM_OBJECTS              = 0x1206;
    int CL_COMMAND_FILL_BUFFER                      = 0x1207;
    int CL_COMMAND_FILL_IMAGE                       = 0x1208;
    // OPENCL_2_0
    int CL_COMMAND_SVM_FREE                         = 0x1209;
    int CL_COMMAND_SVM_MEMCPY                       = 0x120A;
    int CL_COMMAND_SVM_MEMFILL                      = 0x120B;
    int CL_COMMAND_SVM_MAP                          = 0x120C;
    int CL_COMMAND_SVM_UNMAP                        = 0x120D;


    // command execution status
    int CL_COMPLETE = 0x0;
    int CL_RUNNING = 0x1;
    int CL_SUBMITTED = 0x2;
    int CL_QUEUED = 0x3;


    // cl_buffer_create_type
    // OPENCL_1_1
    int CL_BUFFER_CREATE_TYPE_REGION                = 0x1220;

    // cl_profiling_info
    int CL_PROFILING_COMMAND_QUEUED = 0x1280;
    int CL_PROFILING_COMMAND_SUBMIT = 0x1281;
    int CL_PROFILING_COMMAND_START = 0x1282;
    int CL_PROFILING_COMMAND_END = 0x1283;
    // OPENCL_2_0
    int CL_PROFILING_COMMAND_COMPLETE = 0x1284;

    // cl_gl_object_type
    int CL_GL_OBJECT_BUFFER             = 0x2000;
    int CL_GL_OBJECT_TEXTURE2D          = 0x2001;
    int CL_GL_OBJECT_TEXTURE3D          = 0x2002;
    int CL_GL_OBJECT_RENDERBUFFER       = 0x2003;
    // OPENCL_1_2
    int  CL_GL_OBJECT_TEXTURE2D_ARRAY   = 0x200E;
    int  CL_GL_OBJECT_TEXTURE1D         = 0x200F;
    int  CL_GL_OBJECT_TEXTURE1D_ARRAY   = 0x2010;
    int  CL_GL_OBJECT_TEXTURE_BUFFER    = 0x2011;

    // cl_gl_texture_info
    int CL_GL_TEXTURE_TARGET            = 0x2004;
    int CL_GL_MIPMAP_LEVEL              = 0x2005;
    // OPENCL_2_0
    int CL_GL_NUM_SAMPLES               = 0x2012;

    // cl_khr_gl_sharing
    int CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR  =  0x2006;
    int CL_DEVICES_FOR_GL_CONTEXT_KHR         =  0x2007;
    int CL_GL_CONTEXT_KHR               = 0x2008;
    int CL_EGL_DISPLAY_KHR              = 0x2009;
    int CL_GLX_DISPLAY_KHR              = 0x200A;
    int CL_WGL_HDC_KHR                  = 0x200B;
    int CL_CGL_SHAREGROUP_KHR           = 0x200C;

    // cl_APPLE_gl_sharing
    int CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE = 0x10000000;
    int CL_CGL_DEVICE_FOR_CURRENT_VIRTUAL_SCREEN_APPLE = 0x10000002;
    int CL_CGL_DEVICES_FOR_SUPPORTED_VIRTUAL_SCREENS_APPLE = 0x10000003;
    int CL_INVALID_GL_CONTEXT_APPLE = -1000;
}
