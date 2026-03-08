# MetaIR — Visualization Guide

MetaIR produces four distinct Graphviz DOT files for every analyzed method.
This document explains what each file shows, the visual conventions used in
every graph, and how to render them locally or interpret them in the browser.

Companion documents:

- [IR Architecture](IR.md) — node types and edge semantics
- [Sequencer & Code Generation](SEQUENCER.md) — how the graph is linearised
- [Testing Infrastructure](TESTING.md) — how artifact files are written during test runs
- [API Reference](API.md) — how to trigger export programmatically

---

## Table of Contents

1. [The Four Graph Types](#1-the-four-graph-types)
2. [Visual Conventions](#2-visual-conventions)
   - [Node shapes and fill colors](#node-shapes-and-fill-colors)
   - [Edge colors and styles](#edge-colors-and-styles)
   - [Projections](#projections)
   - [Clusters](#clusters)
   - [Legend](#legend)
3. [Rendering Locally](#3-rendering-locally)
4. [Graph-by-Graph Walkthrough](#4-graph-by-graph-walkthrough)
   - [ir.dot — the full IR graph](#irdot--the-full-ir-graph)
   - [ir_dominatortree.dot — full dominator tree](#ir_dominatortreecot--full-dominator-tree)
   - [bytecodecfg.dot — bytecode-level CFG](#bytecodecfgdot--bytecode-level-cfg)
   - [ir_cfg_dominatortree.dot — IR CFG dominator tree](#ir_cfg_dominatortreecot--ir-cfg-dominator-tree)
5. [Online Test Suite](#5-online-test-suite)
6. [Exporting Programmatically](#6-exporting-programmatically)

---

## 1. The Four Graph Types

| File | What it shows |
|---|---|
| `ir.dot` | The full Sea-of-Nodes IR graph — all node and edge types together |
| `ir_dominatortree.dot` | The full IR graph with **dominance edges** (fuchsia) overlaid |
| `bytecodecfg.dot` | The low-level **bytecode CFG** from step 2 of the pipeline (the `Frame` graph) |
| `ir_cfg_dominatortree.dot` | The IR graph with **only control-flow and dominance edges** |

Start with `ir.dot` to understand what the IR looks like. Use
`ir_cfg_dominatortree.dot` to study control-flow structure. Use
`bytecodecfg.dot` to compare with the original bytecode layout.

---

## 2. Visual Conventions

The same visual language is used across all four graphs wherever applicable.

### Node shapes and fill colors

| Shape | Fill color | Meaning |
|---|---|---|
| Box (rectangle) | Light grey | **Control node** — `Method`, `LabelNode`, `Return`, `ReturnValue`, `Goto`, `If`, `Invoke`, `ClassInitialization`, `MonitorEnter`, `MonitorExit`, `Throw`, `ArrayStore`, `ArrayLoad`, `CheckCast`, `PutField`, `PutStatic`, `MergeNode`, `Projection`, `LoopHeaderNode`, `TableSwitch`, `LookupSwitch`, `ExceptionGuard`, `Catch` |
| Octagon | Light green | **Constant node** — any node where `node.isConstant()` is true |
| Octagon | Orange | **Value node** — any other `Value` subclass (arithmetic results, field reads, comparisons, etc.) |

The shape alone tells you the rough role of a node:
- **Box = has control-flow significance** (can appear in a sequential program listing)
- **Octagon = pure value** (only appears as an operand)

### Edge colors and styles

| Color | Style | Meaning |
|---|---|---|
| **Red**, solid | — | Control-flow edge (forward) |
| **Red**, dashed | — | Control-flow back edge (loop) |
| **Black**, solid | — | Data-flow edge (value dependency) |
| **Black**, dotted | — | Declaration edge (`DefinedByUse`) — links a value to its defining scope |
| **Blue**, solid | — | PHI data-flow edge (forward) |
| **Blue**, dashed | — | PHI data-flow back edge (loop-carried dependency) |
| **Green**, solid | — | Memory-chain edge (`MemoryUse`) |
| **Fuchsia**, solid | — | Immediate-dominator edge (only in dominator-tree graphs) |

### Projections

Nodes that have multiple successors (like `If`, `LookupSwitch`, or
`ExceptionGuard`) emit their control flow through **Projection** sub-nodes.
Each projection represents a single outgoing path (e.g., *true branch*,
*false branch*, *exception handler*).

In the DOT output, a node that has projections is rendered as an HTML table:
the top row shows the node's label and the bottom row shows one cell per
projection, color-coded yellow. An edge from an upstream node arrives at the
specific projection port rather than at the parent node.

This makes it visually obvious which branch a particular control path takes.

### Clusters

Grey-bordered rectangular clusters group a defining node together with the
nodes it "declares" (connected via `DefinedByUse` edges). In practice this
means:

- A `LabelNode` (the CFG block entry) is clustered with the IR nodes that
  belong to that block.
- PHI nodes are clustered with their merge block.

Clusters make it easier to see which variables are in scope in each block.

### Legend

Every generated DOT file includes a `cluster_000` subgraph that renders a
legend in the bottom-right corner of the image. The legend shows one example
arrow for each edge type so you do not have to memorise the colour scheme.

---

## 3. Rendering Locally

You need [Graphviz](https://graphviz.org/) installed. On macOS:

```bash
brew install graphviz
```

On Debian/Ubuntu:

```bash
sudo apt-get install graphviz
```

### PNG (quick overview)

```bash
dot -Tpng ir.dot -o ir.png
open ir.png
```

### SVG (recommended — scales without blurring, clickable in browsers)

```bash
dot -Tsvg ir.dot -o ir.svg
open ir.svg
```

### All four files in one shot

```bash
for f in ir.dot ir_dominatortree.dot bytecodecfg.dot ir_cfg_dominatortree.dot; do
    dot -Tsvg "$f" -o "${f%.dot}.svg"
done
```

### Tips for large graphs

Very complex methods produce dense graphs. Two options:

1. **`-Kneato` or `-Ksfdp`** layout engines sometimes produce cleaner layouts
   for large graphs than the default hierarchical `dot` engine:

   ```bash
   dot -Kneato -Tsvg ir.dot -o ir.svg
   ```

2. **Interactive viewers** like [xdot](https://github.com/jrfonseca/xdot.py)
   or [Gephi](https://gephi.org/) let you pan and zoom:

   ```bash
   pip install xdot
   xdot ir.dot
   ```

---

## 4. Graph-by-Graph Walkthrough

### ir.dot — the full IR graph

This is the primary diagnostic graph. It shows every node and every edge in
the IR at the point analysis completed (before any optimization passes).

**Reading it top-down:**

- The `Method` root node (grey box) appears near the top. It is the only node
  with no incoming control-flow edge.
- Red solid edges trace the control-flow path through the method.
- Red dashed back edges indicate loop edges — the target is a loop header that
  was already visited.
- Black edges carry data values from producers to consumers.
- Green edges trace the memory chain — the single linear sequence of
  memory-affecting operations that preserves ordering.
- Blue edges connect incoming values to PHI nodes at merge points.

**Worked example — a simple `if` statement:**

```
Method → If (control, red solid)
If:true  → MergeNode (control, red solid)
If:false → MergeNode (control, red solid)
MergeNode → ReturnValue (control, red solid)
PHI ← value-in-true-branch (blue solid)
PHI ← value-in-false-branch (blue solid)
```

Both branches converge at the `MergeNode`, and a `PHI` node selects between
the two branch values.

### ir_dominatortree.dot — full dominator tree

This graph overlays the full IR (all edge types) with additional **fuchsia**
arrows showing the immediate-dominator relationship.

For any node `n`, the fuchsia arrow points to `n`'s immediate dominator —
the closest ancestor that lies on every possible path from the `Method` root
to `n`.

Use this graph when:

- Debugging a failing dominator computation.
- Understanding why the Sequencer emits code in a particular order.
- Verifying that a loop header dominates all nodes inside the loop body.

### bytecodecfg.dot — bytecode-level CFG

This graph shows the **Frame graph** built in step 2 of the parsing pipeline,
before any IR nodes are created. Each node in this graph represents one
element in the `CodeModel`'s element list (a bytecode instruction or a label
target).

- Red solid edges are forward control-flow edges between bytecode positions.
- Red dotted edges (in this graph) are back edges from a backward jump to a
  loop header.
- Fuchsia edges show the immediate-dominator relationship in the bytecode CFG.
- Each node label shows: `#<index> <element> Order: <topological position>`.

Use this graph to compare the raw bytecode structure with the higher-level IR.
It is particularly useful when the IR looks unexpected, because it lets you
verify whether the problem is in the CFG-building step (step 2) or the
IR-construction step (steps 3–6).

### ir_cfg_dominatortree.dot — IR CFG dominator tree

This is a stripped-down version of the full dominator-tree graph. It shows
**only** control-flow edges (red) and dominance edges (fuchsia) — data-flow,
memory, and PHI edges are omitted.

Use this graph for:

- A clean view of the program's control structure without the visual noise of
  data dependencies.
- Verifying loop nesting: a `LoopHeaderNode` should dominate every node in
  its loop body.
- Checking that exception handlers are correctly placed relative to the guarded
  region.

---

## 5. Online Test Suite

Every method in `src/test/java/.../examples/` is published at:

**https://mirkosertic.github.io/MetaIR/**

Each directory corresponds to one test class. Within it, each method has a
subdirectory containing all four `.dot` files rendered as SVGs, plus the
`bytecode.yaml` and `sequenced.txt` artifacts. This is the quickest way to
explore what the IR looks like for common Java constructs without running
anything locally.

---

## 6. Exporting Programmatically

All four export methods live in `DOTExporter` and write to any `PrintStream`:

```java
import de.mirkosertic.metair.ir.*;

// Full IR graph
try (PrintStream ps = new PrintStream("ir.dot")) {
    DOTExporter.writeTo(analyzer.ir(), ps);
}

// Full dominator tree (all edge types + dominance edges)
try (PrintStream ps = new PrintStream("ir_dominatortree.dot")) {
    DOTExporter.writeTo(new DominatorTree(analyzer.ir()), ps);
}

// IR CFG dominator tree (control edges + dominance edges only)
try (PrintStream ps = new PrintStream("ir_cfg_dominatortree.dot")) {
    DOTExporter.writeTo(new CFGDominatorTree(analyzer.ir()), ps);
}

// Bytecode-level CFG (Frame graph from step 2)
try (PrintStream ps = new PrintStream("bytecodecfg.dot")) {
    DOTExporter.writeBytecodeCFGTo(analyzer, ps);
}
```

`MetaIRTestHelper.analyzeAndReport()` calls all four methods automatically and
writes the files to the configured output directory — see
[TESTING.md § 4](TESTING.md#4-output-artifacts) for the full artifact list.

---

*See also: [IR.md](IR.md) · [API.md](API.md) · [TESTING.md](TESTING.md) · [SEQUENCER.md](SEQUENCER.md)*
