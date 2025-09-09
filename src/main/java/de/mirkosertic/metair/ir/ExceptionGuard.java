package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;

public class ExceptionGuard extends TupleNode {

    public record Catches(int index, List<ClassDesc> catchTypes) {
    }

    final String startLabel;
    final List<Catches> catches;

    ExceptionGuard(final String startLabel, final List<Catches> catches) {

        this.catches = catches;
        this.startLabel = startLabel;

        for (final Catches catchEntry : catches) {
            final CatchProjection projection = new CatchProjection(catchEntry.index, catchEntry.catchTypes);
            registerAs(projection.name(), controlFlowsTo(projection, FlowType.FORWARD).controlFlowsTo(new Catch(catchEntry.catchTypes, this), FlowType.FORWARD));
        }
        registerAs("default", controlFlowsTo(new ExtractControlFlowProjection("default"), FlowType.FORWARD));
        registerAs("exit", controlFlowsTo(new ExtractControlFlowProjection("exit"), FlowType.FORWARD));
    }

    public ExtractControlFlowProjection guardedBlock() {
        return (ExtractControlFlowProjection) getNamedNode("default");
    }

    public Catch catchProjection(final int index) {
        final Catches catchEntry = catches.get(index);
        final CatchProjection p = new CatchProjection(index, catchEntry.catchTypes);
        return (Catch) getNamedNode(p.name());
    }

    public ExtractControlFlowProjection exitNode() {
        return (ExtractControlFlowProjection) getNamedNode("exit");
    }

    @Override
    public String debugDescription() {
        return "ExceptionGuard";
    }
}
