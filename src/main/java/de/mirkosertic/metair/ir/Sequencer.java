package de.mirkosertic.metair.ir;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Sequencer {

    public static class Block {

        public enum Type {
            LOOP, NORMAL
        }

        public final String label;
        public final Type type;

        private final Node continueLeadsTo;
        private final Node breakLeadsTo;

        public Block(final String label, final Type type, final Node continueLeadsTo, final Node breakLeadsTo) {
            this.label = label;
            this.type = type;
            this.continueLeadsTo = continueLeadsTo;
            this.breakLeadsTo = breakLeadsTo;
        }
    }

    private final CFGDominatorTree dominatorTree;

    private final StructuredControlflowCodeGenerator codegenerator;

    public Sequencer(final Method method, final StructuredControlflowCodeGenerator codegenerator) {
        this.dominatorTree = new CFGDominatorTree(method);
        this.codegenerator = codegenerator;

        visitDominationTreeOf(method, new ArrayDeque<>());
    }

    private void visitDominationTreeOf(final Node startNode, final Deque<Block> activeStack) {
        final int startHeight = activeStack.size();

        Node current = startNode;

        final Function<Node, Node> followUpProcessor = node -> {
            for (final Node user : node.usedBy) {
                for (final Node.UseEdge edge : user.uses) {
                    if (edge.node() == node) {
                        if (edge.use() instanceof final ControlFlowUse cfu) {
                            if (dominatorTree.getIDom(user) == node) {
                                // We can continue to the child
                                return user;
                            }
                            generateGOTO(node, user, activeStack);
                        }
                    }
                }
            }
            return null;
        };

        while (current != null) {
            try {
                switch (current) {
                    case final Method method: {
                        codegenerator.begin(method);
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final LabelNode labelNode: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final MergeNode mergeNode: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final ArrayStore arrayStore: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final ArrayLoad arrayLoad: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final ClassInitialization classInitialization: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final PutStatic putStatic: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final PutField putField: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final Div div: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final Rem rem: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final InvokeSpecial invokeSpecial: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final InvokeStatic invokeStatic: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final InvokeInterface invokeInterface: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final InvokeVirtual invokeVirtual: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final InvokeDynamic invokeDynamic: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final MonitorEnter monitorEnter: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final MonitorExit monitorExit: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final CheckCast checkCast: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final Catch catchNode: {
                        visit(catchNode, activeStack, followUpProcessor);
                        current = null;
                        break;
                    }
                    case final ExtractControlFlowProjection extractControlFlowProjection: {
                        // TODO
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final Return ret: {
                        codegenerator.write(ret);
                        // We are finished here
                        current = null;
                        break;
                    }
                    case final ReturnValue returnValue: {
                        codegenerator.write(returnValue);
                        // We are finished here
                        current = null;
                        break;
                    }
                     case final Goto gto: {
                        codegenerator.write(gto);
                        current = followUpProcessor.apply(current);
                        break;
                    }
                    case final Throw th: {
                        codegenerator.write(th);
                        // We are finished here
                        current = null;
                        break;
                    }
                    case final If iff: {
                        // Special-Case : branching
                        visit(iff, activeStack);
                        current = null;
                        break;
                    }
                    case final LookupSwitch lookupSwitch: {
                        // Special-Case : branching
                        visit(lookupSwitch, activeStack);
                        current = null;
                        break;
                    }
                    case final TableSwitch tableSwitch: {
                        // Special-Case : branching
                        visit(tableSwitch, activeStack);
                        current = null;
                        break;
                    }
                    case final LoopHeaderNode loopHeaderNode: {
                        // Special-Case : looping
                        visit(loopHeaderNode, activeStack, followUpProcessor);
                        current = null;
                        break;
                    }
                    case final ExceptionGuard exceptionGuard: {
                        visit(exceptionGuard, activeStack);
                        current = null;
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported node type : " + current.getClass().getName());
                }
            } catch (final IllegalStateException | SequencerException e) {
                throw e;
            } catch (final RuntimeException e) {
                throw new SequencerException("Error processing node", e);
            }
        }

        while (activeStack.size() > startHeight) {
            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());
        }
    }

    private void generateGOTO(final Node currentNode, final Node target, final Deque<Block> activeStack) {
        for (final Block b : activeStack) {
            if (b.breakLeadsTo == target) {
                codegenerator.writeBreakTo(b.label);
                return;
            }
            if (b.continueLeadsTo == target) {
                codegenerator.writeContinueTo(b.label);
                return;
            }
        }
        throw new IllegalStateException("GOTO not properly handled");
    }

    private void visitBranchingNodeTemplate(final Node node, final Deque<Block> activeStack, final Consumer<Deque<Block>> nodeCallback) {

        final List<Node> rpo = dominatorTree.getRpo();
        final List<Node> orderedBlocks = dominatorTree.immediatelyDominatedNodesOf(node)
                .stream()
                // We are only interested in merge nodes
                .filter(t -> t instanceof MergeNode)
                // And sort them in reverse post order
                .sorted((o1, o2) -> {
                    final int a = rpo.indexOf(o1);
                    final int b = rpo.indexOf(o2);
                    if ((a == -1) || (b == 1)) {
                        throw new IllegalStateException("Don't know what to do");
                    }
                    return Integer.compare(b, a);
                })
                .toList();

        final boolean isLoopHeader = node instanceof LoopHeaderNode;

        final String prefix = node.getClass().getSimpleName() + "_";
        final int selfIndex = rpo.indexOf(node);

        if (isLoopHeader) {
            final Block b = new Block(prefix + selfIndex, Block.Type.LOOP, node, null);
            activeStack.push(b);
            codegenerator.startBlock(b);
        }

        for (int i = 0; i < orderedBlocks.size(); i++) {
            final Node target = orderedBlocks.get(i);

            final Block newBlock = new Block(prefix + selfIndex + "_" + i, Block.Type.NORMAL, null, target);
            activeStack.push(newBlock);

            codegenerator.startBlock(newBlock);
        }

        nodeCallback.accept(activeStack);

        for (int i = orderedBlocks.size() - 1; i >= 0; i--) {
            final Node target = orderedBlocks.get(i);

            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());

            visitDominationTreeOf(target, activeStack);
        }

        if (isLoopHeader) {
            codegenerator.finishBlock(activeStack.pop(), activeStack.isEmpty());
        }
    }

    private void visit(final LoopHeaderNode node, final Deque<Block> activeStack, final Function<Node, Node> followUpProcessor) {
        visitBranchingNodeTemplate(node, activeStack, blocks -> visitDominationTreeOf(followUpProcessor.apply(node), blocks));
    }

    private void visit(final If node, final Deque<Block> activeStack) {
        visitBranchingNodeTemplate(node, activeStack, blocks -> {
            codegenerator.startIfWithTrueBlock(node);

            // True block
            visitDominationTreeOf(node.trueProjection(), blocks);

            codegenerator.startIfElseBlock(node);

            // False block
            visitDominationTreeOf(node.falseProjection(), blocks);

            codegenerator.finishIfBlock();
        });
    }

    private void visit(final LookupSwitch node, final Deque<Block> activeStack) {
        visitBranchingNodeTemplate(node, activeStack, blocks -> {
            codegenerator.startLookupSwitch(node);

            for (int i = 0; i < node.cases.size(); i++) {
                codegenerator.writeSwitchCase(i);

                visitDominationTreeOf(node.caseProjection(i), blocks);

                codegenerator.finishSwitchCase();
            }

            codegenerator.writeSwitchDefaultCase();

            visitDominationTreeOf(node.defaultProjection(), blocks);

            codegenerator.finishSwitchDefault();

            codegenerator.finishLookupSwitch();
        });
    }

    private void visit(final TableSwitch node, final Deque<Block> activeStack) {

        visitBranchingNodeTemplate(node, activeStack, blocks -> {
            codegenerator.startTableSwitch(node);

            for (int i = 0; i < node.cases.size(); i++) {
                codegenerator.writeSwitchCase(i);

                visitDominationTreeOf(node.caseProjection(i), blocks);

                codegenerator.finishSwitchCase();
            }

            codegenerator.startTableSwitchDefaultBlock();

            visitDominationTreeOf(node.defaultProjection(), blocks);

            codegenerator.finishTableSwitchDefaultBlock();

            codegenerator.finishTableSwitch();
        });
    }

    private void visit(final ExceptionGuard node, final Deque<Block> as) {

        visitBranchingNodeTemplate(node, as, blocks -> {

            final boolean hasExceptionHandler = !node.catches.isEmpty();
            if (hasExceptionHandler) {
                codegenerator.startTryCatch(node);
            }

            visitDominationTreeOf(node.guardedBlock(), blocks);

            if (hasExceptionHandler) {
                for (int i = 0; i < node.catches.size(); i++) {
                    final ExceptionGuard.Catches catches = node.catches.get(i);

                    if (i == 0) {
                        codegenerator.startCatchBlock();
                    }

                    visitDominationTreeOf(node.catchProjection(i), blocks);
                }

                codegenerator.writeRethrowException();

                codegenerator.finishTryCatch();
            }
        });
    }

    private void visit(final Catch node, final Deque<Block> activeStack, final Function<Node, Node> followUpProcessor) {

        codegenerator.startCatchHandler(node.exceptionType != null ? Optional.of(node.exceptionType) : Optional.empty());

        visitDominationTreeOf(followUpProcessor.apply(node), activeStack);

        codegenerator.finishCatchHandler();
    }

}