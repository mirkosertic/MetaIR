package de.mirkosertic.metair.opencl.api;

public class GlobalFunctions {

    private static class Context {
        int currentWorkItekId;
        int size;
    }

    private final static ThreadLocal<Context> currentContext = new ThreadLocal<>();

    private static Context current() {
        Context theCurrent = currentContext.get();
        if (theCurrent == null) {
            theCurrent = new Context();
            currentContext.set(theCurrent);
        }
        return theCurrent;
    }

    @OpenCLFunction("get_global_id")
    public static int get_global_id(final int aDimension) {
        return current().currentWorkItekId;
    }

    @OpenCLFunction("get_global_size")
    public static int get_global_size(final int aDimension) {
        return current().size;
    }

    public static void set_global_id(final int aDimension, final int aId) {
        current().currentWorkItekId = aId;
    }

    public static void set_global_size(final int aDimension, final int aSize) {
        current().size = aSize;
    }
}
