# MetaIR — Graph Optimization Guide

This document explains how the MetaIR graph is structured, what invariants it
maintains, how to traverse and mutate it safely, and how to write new
analysis or optimization passes on top of it.

Companion documents:

- [IR Architecture](IR.md) — node types, edge semantics, the parsing pipeline
- [Sequencer & Code Generation](SEQUENCER.md) — how the finished graph is linearised
- [API Reference](API.md) — class-loading and programmatic analysis entry points
- [Visualization](VISUALIZATION.md) — rendering the graph to understand what a pass did

---

## Table of Contents

1. [Why a Graph IR?](#1-why-a-graph-ir)
2. [Graph Invariants](#2-graph-invariants)
3. [Traversal Utilities](#3-traversal-utilities)
   - [DFS2 — Topological Order](#dfs2--topological-order)
   - [DominatorTree](#dominatortree)
   - [CFGDominatorTree](#cfgdominatortree)
4. [Edge System](#4-edge-system)
5. [Safe Mutation Patterns](#5-safe-mutation-patterns)
   - [Rewiring a data edge](#rewiring-a-data-edge)
   - [Eliminating a node](#eliminating-a-node)
   - [Inserting a new node](#inserting-a-new-node)
6. [Where to Insert a Pass](#6-where-to-insert-a-pass)
7. [Example: Constant Folding](#7-example-constant-folding)
8. [Example: Dead Code Elimination](#8-example-dead-code-elimination)
9. [PHI Nodes and Loop-carried Values](#9-phi-nodes-and-loop-carried-values)
10. [Memory Chain Ordering](#10-memory-chain-ordering)
11. [Exception Handling and Guards](#11-exception-handling-and-guards)
12. [Debugging a Pass](#12-debugging-a-pass)

---

## 1. Why a Graph IR?

Traditional IR formats (three-address code, basic-block lists) represent a
program as a sequence of statements where the order is implicit in the program
text. This makes many transformations awkward: to move a computation, you have
to update every statement around it.

MetaIR uses the **Sea-of-Nodes** design (Cliff Click, 1995). In this
representation:

- **Every value is a node.** An integer constant, an arithmetic result, a
  method argument — all are nodes.
- **Dependencies are explicit edges.** A node can only use a value if there
  is an edge from that value's node to the consumer node.
- **Order is derived, not given.** The topological order of the graph defines
  a valid execution order. There is no canonical statement list to maintain.

This makes many passes natural:

- **Constant folding**: replace an arithmetic node with a constant node and
  rewire its consumers. No surrounding code changes.
- **Common subexpression elimination (CSE)**: find two nodes that compute the
  same thing and make all consumers of one point to the other.
- **Dead code elimination**: a node with no consumers (no `usedBy` entries)
  is dead and can be removed.
- **Code motion**: because values have no fixed position, moving a computation
  is just a matter of verifying that its inputs are still available at the new
  position.

---

## 2. Graph Invariants

Every valid MetaIR graph maintains the following invariants. A pass **must**
preserve all of them.

### 1. Edge bidirectionality

Every edge is represented in both directions:

- `node.uses` — outgoing edges (what `node` depends on)
- `node.usedBy` — incoming edges (who depends on `node`)

If you add or remove an edge in one direction you **must** update the other.
The helper methods on `Node` (`addUse`, `removeUse`) maintain this automatically
when used correctly.

### 2. Unique control-flow entry

Every non-root node has exactly one control-flow predecessor in the dominator
tree. Multiple CFG predecessors are allowed (merge points), but there is always
a unique immediate dominator.

### 3. Single memory chain

All memory-affecting operations (`ArrayStore`, `PutField`, `PutStatic`,
`MonitorEnter`, `MonitorExit`, `Invoke`, `Throw`, etc.) are connected in a
single linear chain via `MemoryUse` edges. This chain enforces the observable
order of side effects. Breaking this chain or creating a branch in it makes
the graph invalid.

### 4. PHI nodes at merge points only

A `PHI` node is valid only at a control-flow merge point (a node with two or
more control-flow predecessors). The number of `PHIUse` incoming edges on a
PHI must equal the number of CFG predecessors of the merge block.

### 5. Back edges are marked

Any edge that creates a cycle in the graph (loop back-edge, loop-carried PHI
data dependency) must use `FlowType.BACKWARD`. Forward edges must use
`FlowType.FORWARD`. `DFS2` and the dominator algorithms depend on this to
avoid infinite loops during traversal.

### 6. The root is always `Method`

The single entry point of every method's graph is the `Method` node. All
traversals start here.

---

## 3. Traversal Utilities

### DFS2 — Topological Order

```java
// Full topological order (all edge types except back-edges)
List<Node> order = new DFS2(root).getTopologicalOrder();

// Control-flow-only topological order (ignores data/memory/PHI edges)
List<Node> cfgOrder = new DFS2(root, true).getTopologicalOrder();
```

`DFS2` implements a worklist-based topological sort. A node is scheduled once
all of its forward predecessors have already been scheduled. The algorithm
aborts after 5 000 iterations and throws `IllegalStateException` if a cycle
is detected in the forward edges (which would indicate a graph invariant
violation — see above).

The resulting list is a valid topological order: a node always appears *after*
all the nodes it depends on. This is the safe order to iterate when you want
to process producers before consumers.

### DominatorTree

```java
DominatorTree dt = new DominatorTree(root);

// Immediate dominator of any node
Node idom = dt.idom.get(node);

// Nodes in pre-order (parent before children in the dominator tree)
List<Node> preOrder = dt.preOrder;

// Reverse-post-order (topological order of the full graph)
List<Node> rpo = dt.rpo;
```

`DominatorTree` uses the Cooper-Harvey-Kennedy iterative algorithm on the full
IR graph (all edge types). It is used internally by `DOTExporter` and
available for any pass that needs dominance information.

### CFGDominatorTree

```java
CFGDominatorTree cfgDt = new CFGDominatorTree(root);

Node idom = cfgDt.idom.get(node);
List<Node> preOrder = cfgDt.preOrder;
```

`CFGDominatorTree` runs the same algorithm but considers **only
control-flow edges** (`ControlFlowUse` with `FlowType.FORWARD`). The result
is the control-flow-only dominator tree used by the `Sequencer`.

---

## 4. Edge System

Each edge is represented by a `Node.UseEdge` record stored in the `uses`
list of the consumer node:

```java
record UseEdge(Node node, Use use) { }
```

- `node` — the producer (the node being depended on)
- `use` — the type descriptor for this edge

Edge type hierarchy:

```
Use
├── DataFlowUse          — value dependency
├── ControlFlowUse       — control flow (has FlowType: FORWARD or BACKWARD)
├── MemoryUse            — memory chain
├── PHIUse               — PHI incoming value (has FlowType and origin)
├── DefinedByUse         — scope / declaration link
└── ArgumentUseXxx       — method argument passing (subclasses)
```

The `usedBy` set on a node contains all nodes that have an edge pointing to it
— i.e., all consumers. This set is maintained automatically by `Node` when
edges are added or removed.

---

## 5. Safe Mutation Patterns

> **Warning:** The graph is mutable by design, but mutation must preserve all
> invariants listed in [Section 2](#2-graph-invariants). Always work on the
> result of a completed `MethodAnalyzer.analyze()` call — never mutate during
> analysis.

### Rewiring a data edge

To replace one producer with another for all consumers of a node:

```java
// Replace all uses of `oldNode` with `newNode`
for (Node consumer : new ArrayList<>(oldNode.usedBy)) {
    for (int i = 0; i < consumer.uses.size(); i++) {
        Node.UseEdge e = consumer.uses.get(i);
        if (e.node() == oldNode) {
            // Replace the edge
            consumer.uses.set(i, new Node.UseEdge(newNode, e.use()));
            // Update the back-pointer
            newNode.usedBy.add(consumer);
        }
    }
    oldNode.usedBy.remove(consumer);
}
```

After this, `oldNode.usedBy` is empty. If it also has no control-flow
successors, it is dead and can be removed.

> **Tip:** Always copy `usedBy` before iterating if you plan to modify it
> (use `new ArrayList<>(node.usedBy)` as above) — modifying the set while
> iterating over it causes `ConcurrentModificationException`.

### Eliminating a node

A node is safe to remove when:

1. Its `usedBy` set is empty (no remaining consumers), **and**
2. It has no outgoing control-flow edges (it is not a control node that must
   appear in sequence).

```java
if (node.usedBy.isEmpty() /* && not a control node */) {
    // Remove it from all producer back-pointer sets
    for (Node.UseEdge e : node.uses) {
        e.node().usedBy.remove(node);
    }
    node.uses.clear();
}
```

Removing a control node (a grey-box node) is more involved because you must
also rewire the control-flow chain around it before eliminating it.

### Inserting a new node

1. Create the new node (typically a subclass of `Value` or a control node).
2. Add `UseEdge` entries to its `uses` list for each input it depends on.
3. Add the new node to the `usedBy` set of each of its inputs.
4. Rewire the consumers of the node it replaces (or the control chain if it
   is a new control node) to point to the new node.

---

## 6. Where to Insert a Pass

The cleanest place to run a pass is **after** `MethodAnalyzer.analyze()`
completes and **before** calling `Sequencer` to linearize the graph.

```java
MethodAnalyzer analyzer = resolvedMethod.analyze();

// Run your passes here
myFoldingPass(analyzer.ir());
myEliminationPass(analyzer.ir());

// Then sequence
new Sequencer(analyzer.ir(), backend).sequence();
```

`analyzeAndReport` in `MetaIRTestHelper` runs analysis and immediately writes
artifacts — if you want to compare before/after, call `rm.analyze()` directly,
run your pass, then call `DOTExporter` yourself.

---

## 7. Example: Constant Folding

A constant-folding pass finds `Add` nodes where both inputs are `PrimitiveInt`
constants and replaces them with a single `PrimitiveInt` constant:

```java
void foldConstants(Method root) {
    for (Node n : new DFS2(root).getTopologicalOrder()) {
        if (n instanceof Add add) {
            List<Node> args = add.arguments();
            if (args.get(0) instanceof PrimitiveInt a
                    && args.get(1) instanceof PrimitiveInt b) {

                // Create the folded constant
                PrimitiveInt folded = new PrimitiveInt(a.value() + b.value());

                // Rewire all consumers
                for (Node consumer : new ArrayList<>(add.usedBy)) {
                    for (int i = 0; i < consumer.uses.size(); i++) {
                        Node.UseEdge e = consumer.uses.get(i);
                        if (e.node() == add) {
                            consumer.uses.set(i, new Node.UseEdge(folded, e.use()));
                            folded.usedBy.add(consumer);
                        }
                    }
                    add.usedBy.remove(consumer);
                }

                // The Add node is now dead — disconnect it from its inputs
                for (Node.UseEdge e : add.uses) {
                    e.node().usedBy.remove(add);
                }
                add.uses.clear();
            }
        }
    }
}
```

Note: in a real pass you would also handle `PrimitiveLong`, `PrimitiveFloat`,
and `PrimitiveDouble`, and you would repeat the pass until no more folds are
possible (since folding one constant can expose another).

---

## 8. Example: Dead Code Elimination

After constant folding (or any other simplification), some value nodes may no
longer have any consumers. A dead-code elimination pass removes them:

```java
void eliminateDead(Method root) {
    boolean changed = true;
    while (changed) {
        changed = false;
        for (Node n : new DFS2(root).getTopologicalOrder()) {
            // Only eliminate pure value nodes — never control nodes
            if (n instanceof Value && !(n instanceof Invoke)
                    && n.usedBy.isEmpty()) {
                for (Node.UseEdge e : n.uses) {
                    e.node().usedBy.remove(n);
                }
                n.uses.clear();
                changed = true;
            }
        }
    }
}
```

The loop repeats because eliminating one dead node may make another dead
(e.g., if the only consumer of a constant was the node just eliminated).

---

## 9. PHI Nodes and Loop-carried Values

PHI nodes require special care:

- A PHI has one `PHIUse` edge per incoming CFG path. Each `PHIUse` records
  the `origin` — the control-flow predecessor node that provides the value on
  that path.
- Loop-carried PHI edges use `FlowType.BACKWARD`. These edges form cycles in
  the graph but are safe to traverse because `DFS2` skips back edges.

When you replace the input to a PHI, you must also update the `PHIUse.origin`
if the control predecessor changes:

```java
// Example: the loop-back value changes from `oldVal` to `newVal`
for (int i = 0; i < phi.uses.size(); i++) {
    Node.UseEdge e = phi.uses.get(i);
    if (e.node() == oldVal && e.use() instanceof PHIUse pu
            && pu.type == FlowType.BACKWARD) {
        phi.uses.set(i, new Node.UseEdge(newVal, pu));
        newVal.usedBy.add(phi);
        oldVal.usedBy.remove(phi);
    }
}
```

A PHI with all identical inputs (all incoming values are the same node) is a
**singular PHI** and can be eliminated: replace the PHI with the single input
value everywhere it is used.

---

## 10. Memory Chain Ordering

The memory chain (`MemoryUse` edges) serializes all operations with observable
side effects. When you insert a new side-effecting node, you must splice it
into the chain:

1. Find the memory chain node that currently precedes the insertion point
   (`prevMemory`).
2. Find the node that currently consumes `prevMemory` via `MemoryUse`
   (`nextMemory`).
3. Rewire:
   - new node's `MemoryUse` edge → `prevMemory`
   - `nextMemory`'s `MemoryUse` edge → new node

Conversely, when you eliminate a side-effecting node, you must bridge the
gap by connecting its memory predecessor directly to its memory successor.

---

## 11. Exception Handling and Guards

`ExceptionGuard` nodes define try-regions. A guard node:

- Receives a forward control-flow edge from the instruction that starts the
  guarded region.
- Has a `CatchProjection` that flows to the matching `Catch` handler block.
- Has a `ExtractControlFlowProjection` (the normal-execution path) that
  continues to the next instruction inside the try.

When writing a pass that moves code:

- Code inside a guarded region may throw exceptions that are caught by the
  guard's handler. Moving such code outside the region changes the observable
  behavior.
- Always check whether a node is inside a guarded region before hoisting it.
  Walk the control-flow chain looking for `ExceptionGuard` ancestors.

---

## 12. Debugging a Pass

### Step 1 — Dump the graph before and after

```java
try (PrintStream ps = new PrintStream("before.dot")) {
    DOTExporter.writeTo(analyzer.ir(), ps);
}

myPass(analyzer.ir());

try (PrintStream ps = new PrintStream("after.dot")) {
    DOTExporter.writeTo(analyzer.ir(), ps);
}
```

Render both with `dot -Tsvg` and compare them side by side. See
[VISUALIZATION.md](VISUALIZATION.md) for rendering instructions.

### Step 2 — Check invariants

After a pass, call `new DFS2(root).getTopologicalOrder()` and confirm it
returns without throwing `IllegalStateException`. If the traversal aborts with
"Unschedulable IR detected!" a cycle was introduced in the forward edges —
a forward edge was left pointing backward or a back-edge was left unmarked.

### Step 3 — Re-sequence

Run the Sequencer after your pass. A graph that sequences correctly without
`SequencerException` is likely structurally sound:

```java
new Sequencer(analyzer.ir(), new DebugStructuredControlflowCodeGenerator(writer))
        .sequence();
```

The `DebugStructuredControlflowCodeGenerator` (used in the test suite) prints
human-readable pseudo-code. If it produces sensible output, the graph is
ready for the real backend.

---

*See also: [IR.md](IR.md) · [SEQUENCER.md](SEQUENCER.md) · [API.md](API.md) · [VISUALIZATION.md](VISUALIZATION.md) · [TESTING.md](TESTING.md)*
