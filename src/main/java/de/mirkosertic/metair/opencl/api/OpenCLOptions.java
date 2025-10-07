package de.mirkosertic.metair.opencl.api;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public class OpenCLOptions {

    private final Predicate<PlatformProperties> platformFilter;
    public Predicate<PlatformProperties> getPlatformFilter() {
        return platformFilter;
    }

    private final Comparator<DeviceProperties> preferredDeviceComparator;
    public Comparator<DeviceProperties> getPreferredDeviceComparator() {
        return preferredDeviceComparator;
    }

    /**
     * @apiNote not exposed, use builder to create an instance
     */
    private OpenCLOptions(
            final Predicate<PlatformProperties> platformFilter,
            final Comparator<DeviceProperties> preferredDeviceComparator) {
        this.platformFilter = platformFilter;
        this.preferredDeviceComparator = preferredDeviceComparator;
    }

    // -- DEFAULTS

    public static OpenCLOptions defaults() {
        return OpenCLOptions.builder().build();
    }

    // -- OPTIONS BUILDER

    public static final class Builder {
        private Predicate<PlatformProperties> platformFilter = p -> true;
        private Comparator<DeviceProperties> preferredDeviceComparator = Comparator.comparingInt(DeviceProperties::getNumberOfComputeUnits);

        /**
         * Platforms are rejected if the platformFilter predicate returns false.
         * @param platformFilter
         */
        public Builder platformFilter(final Predicate<PlatformProperties> platformFilter) {
            Objects.requireNonNull(platformFilter);
            this.platformFilter = platformFilter;
            return this;
        }

        /**
         * The device that compares highest is chosen by the {@link PlatformFactory}, unless explicitly
         * overridden by system property {@code OPENCL_DEVICE}.
         * @param preferredDeviceComparator
         */
        public Builder preferredDeviceComparator(final Comparator<DeviceProperties> preferredDeviceComparator) {
            Objects.requireNonNull(preferredDeviceComparator);
            this.preferredDeviceComparator = preferredDeviceComparator;
            return this;
        }

        public OpenCLOptions build() {
            return new OpenCLOptions(platformFilter, preferredDeviceComparator);
        }
    }

    public static Builder builder() {
        return new Builder();
    }



}
