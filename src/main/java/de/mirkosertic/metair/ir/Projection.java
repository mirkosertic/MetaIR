package de.mirkosertic.metair.ir;

public interface Projection extends Comparable<Projection> {

    String name();

    String debugDescription();

    default int compareTo(final Projection o) {
        return name().compareTo(o.name());
    }
}
