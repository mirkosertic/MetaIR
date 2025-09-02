package de.mirkosertic.metair.ir;

import java.util.ArrayDeque;
import java.util.Deque;
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
}