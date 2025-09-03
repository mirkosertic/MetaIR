package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Optional;

public class ExceptionGuard extends TupleNode {

    public record Catches(Optional<ClassDesc> catchType) {
    }

    final List<Catches> catches;

    ExceptionGuard(final List<Catches> catches) {

        this.catches = catches;

        for (int i = 0; i < catches.size(); i++) {
            final Catches catchEntry = catches.get(i);
            if (catchEntry.catchType().isPresent()) {
                final ClassDesc catchType = catchEntry.catchType().get();
                registerAs("catch:" + i + ":" + catchType.descriptorString(), controlFlowsTo(new CatchProjection(i, catchType), FlowType.FORWARD).controlFlowsTo(new Catch(catchType, this), FlowType.FORWARD));
            } else {
                registerAs("catch:" + i + ":any", controlFlowsTo(new CatchProjection(i), FlowType.FORWARD).controlFlowsTo(new Catch(ClassDesc.of(Throwable.class.getName()), this), FlowType.FORWARD));
            }
        }
        registerAs("default", controlFlowsTo(new ExtractControlFlowProjection("default"), FlowType.FORWARD));
        registerAs("exit", controlFlowsTo(new ExtractControlFlowProjection("exit"), FlowType.FORWARD));
    }

    public ExtractControlFlowProjection guardedBlock() {
        return (ExtractControlFlowProjection) getNamedNode("default");
    }

    public Catch catchProjection(final int index) {
        final Catches catchEntry = catches.get(index);
        if (catchEntry.catchType().isPresent()) {
            final ClassDesc catchType = catchEntry.catchType().get();
            return (Catch) getNamedNode("catch:" + index + ":" + catchType.descriptorString());
        }
        return (Catch) getNamedNode("catch:" + index + ":any");
    }

    public ExtractControlFlowProjection exitNode() {
        return (ExtractControlFlowProjection) getNamedNode("exit");
    }

    @Override
    public String debugDescription() {
        return "ExceptionGuard";
    }
}
