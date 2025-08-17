package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Optional;

public class ExceptionGuard extends TupleNode {

    public record Catches(Optional<ClassDesc> catchType) {
    }

    ExceptionGuard(final List<Catches> catches) {
        for (int i = 0; i < catches.size(); i++) {
            final Catches catchEntry = catches.get(i);
            if (catchEntry.catchType().isPresent()) {
                final ClassDesc catchType = catchEntry.catchType().get();

                // TODO: Find a better naming convention: is the exception type still relevant, as the index should be unique?
                // Naming should match the catch projection name
                registerAs("catch:" + i + ":" + catchEntry.catchType().get().displayName(), controlFlowsTo(new CatchProjection(i, catchType), ControlType.FORWARD).controlFlowsTo(new Catch(catchType, this), ControlType.FORWARD));
            } else {

                registerAs("catch:" + i + ":any", controlFlowsTo(new CatchProjection(i), ControlType.FORWARD).controlFlowsTo(new Catch(ClassDesc.of(Throwable.class.getName()), this), ControlType.FORWARD));
            }
        }
        registerAs("default", controlFlowsTo(new ExtractControlFlowProjection("default"), ControlType.FORWARD));
        registerAs("exit", controlFlowsTo(new ExtractControlFlowProjection("exit"), ControlType.FORWARD));
    }

    public Node exitNode() {
        return getNamedNode("exit");
    }

    @Override
    public String debugDescription() {
        return "ExceptionGuard";
    }
}
