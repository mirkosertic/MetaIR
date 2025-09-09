package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;

public class CaughtExceptionProjection extends Value implements Projection {

    CaughtExceptionProjection(final Catch source) {
        // TODO: Do we neet the meet operator here?
        super(source.exceptionTypes.size() != 1 ? ClassDesc.of(Throwable.class.getName()) : source.exceptionTypes.getFirst());

        use(source, new ArgumentUse(0));
    }

    @Override
    public String name() {
        return "exception";
    }

    @Override
    public String debugDescription() {
        return "exception : " + TypeUtils.toString(type);
    }

    @Override
    public boolean isConstant() {
        return true;
    }
}
