# MetaIR — Intermediate Representation Architecture

This document explains how MetaIR represents JVM bytecode internally, how a method is
parsed into that representation, and how the result can be consumed. It is written for
readers who are new to the topic and goes from high-level concepts down to the specific
data structures and algorithms implemented in the code.

---

## Table of Contents

1. [Why a Graph-Based IR?](#1-why-a-graph-based-ir)
2. [Sea-of-Nodes at a Glance](#2-sea-of-nodes-at-a-glance)
3. [The Node Hierarchy](#3-the-node-hierarchy)
4. [The Edge / Use System](#4-the-edge--use-system)
5. [Types — `IRType`](#5-types--irtype)
6. [The Parsing Pipeline — Six Steps](#6-the-parsing-pipeline--six-steps)
   - [Step 1 — Prepare try-catch blocks](#step-1--prepare-try-catch-blocks)
   - [Step 2 — Analyse the Control-Flow Graph](#step-2--analyse-the-control-flow-graph)
   - [Step 3 — Compute Topological Order](#step-3--compute-topological-order)
   - [Step 4 — Compute Frame Dominators](#step-4--compute-frame-dominators)
   - [Step 5 — Abstract Interpretation and IR Emission](#step-5--abstract-interpretation-and-ir-emission)
   - [Step 6 — Remove Singular PHIs](#step-6--remove-singular-phi-nodes)
7. [The Abstract Interpreter in Depth](#7-the-abstract-interpreter-in-depth)
   - [The `Status` Object](#the-status-object)
   - [The `Frame` Object](#the-frame-object)
   - [Processing a Single Frame](#processing-a-single-frame)
   - [Merge Points and PHI Creation](#merge-points-and-phi-creation)
   - [Back Edges and Loop Headers](#back-edges-and-loop-headers)
8. [Exception Handling](#8-exception-handling)
9. [Instruction-Level Parsing Details](#9-instruction-level-parsing-details)
   - [Constants](#constants)
   - [Local Variables](#local-variables)
   - [Stack Manipulation (DUP, POP, SWAP)](#stack-manipulation-dup-pop-swap)
   - [Arithmetic and Bitwise Operations](#arithmetic-and-bitwise-operations)
   - [Conversions and Widening](#conversions-and-widening)
   - [Field Access](#field-access)
   - [Method Invocations](#method-invocations)
   - [Object and Array Creation](#object-and-array-creation)
   - [Branches and Conditional Jumps](#branches-and-conditional-jumps)
   - [Switch Instructions](#switch-instructions)
   - [Return and Throw](#return-and-throw)
   - [invokedynamic](#invokedynamic)
10. [The Memory Chain](#10-the-memory-chain)
11. [Consuming the IR — The `Sequencer`](#11-consuming-the-ir--the-sequencer)
12. [Graph Traversal Utilities](#12-graph-traversal-utilities)
13. [Worked Example — Simple Diamond Shape](#13-worked-example--simple-diamond-shape)

---

## 1. Why a Graph-Based IR?

Traditional compiler intermediate representations store a method as a list of basic
blocks, each containing a sequence of instructions. Control flow between blocks is
expressed with explicit jump instructions. This is convenient for simple transformations
but has two drawbacks:

- **Order matters too much.** Instructions must appear in a fixed sequence even when
  their relative order does not matter. This rigidity complicates scheduling and
  optimization.
- **Data flow is implicit.** Which instruction produces the value consumed by another
  instruction is encoded in named variables (in SSA form) or in operand stacks — not
  directly in the graph topology.

A **graph-based IR** removes the artificial ordering constraint. Every value and every
computation is a node. Edges encode the actual dependencies: "this node needs the result
produced by that node." The nodes may be laid out in any order that respects those
dependencies. This is the foundation of Cliff Click's **Sea-of-Nodes** design, which
MetaIR follows.

---

## 2. Sea-of-Nodes at a Glance

In the Sea-of-Nodes representation there is only one concept: a **node**. Nodes are
connected by typed **edges** (called *uses* in MetaIR). There are three families of
edges:

| Edge family | What it expresses |
|---|---|
| **Data flow** | Node A produces a value that node B consumes. |
| **Control flow** | Node B may only execute after node A has executed. |
| **Memory flow** | Node B may only read/write memory after node A has done so. |

Because all three kinds of dependency are explicit edges, the graph by itself contains
everything needed to determine valid execution orderings. There is no separate concept
of "basic block" or "instruction list" — the graph *is* the program.

A very important special node is the **PHI (Φ) node**. In SSA form, whenever the same
variable can hold different values depending on the path taken through the program (e.g.
after an `if–else`), a PHI node is inserted to merge the incoming values into a single
new value. In the Sea-of-Nodes representation PHI nodes are data-flow nodes owned by
the control-flow node at the join point.

---

## 3. The Node Hierarchy

All nodes extend the abstract base class `Node` (`ir/Node.java`).

```
Node                            — base; stores uses and usedBy sets
 ├─ Value                       — a node that produces a typed result (has IRType)
 │   ├─ PHI                     — SSA merge node; one per merged value at join points
 │   ├─ PrimitiveInt/Long/Float/Double  — compile-time constant values
 │   ├─ StringConstant          — string literal
 │   ├─ Null                    — null reference constant
 │   ├─ RuntimeclassReference   — reference to a Class<?> at run time
 │   ├─ ExtractThisRefProjection / ExtractMethodArgProjection
 │   │                          — projections extracting the receiver / arguments
 │   │                            from the method entry node
 │   ├─ Add, Sub, Mul, Div, Rem — arithmetic (typed by the operand kind)
 │   ├─ Negate                  — unary negation
 │   ├─ BitOperation            — AND, OR, XOR, SHL, SHR, USHR
 │   ├─ NumericCompare          — LCMP / FCMPG / FCMPL / DCMPG / DCMPL
 │   ├─ Convert                 — numeric type conversion (I2F, L2D, …)
 │   ├─ Truncate                — narrow a value to a smaller bit width
 │   ├─ Extend                  — sign- or zero-extend a narrowed value back
 │   ├─ GetField / PutField     — instance field read/write
 │   ├─ GetStatic / PutStatic   — static field read/write
 │   ├─ InvokeVirtual / InvokeSpecial / InvokeStatic / InvokeInterface
 │   │                          — method calls (Value because they can return a value)
 │   ├─ InvokeDynamic           — invokedynamic call site
 │   ├─ New                     — object allocation
 │   ├─ NewArray / NewMultiArray — array allocation
 │   ├─ ArrayLoad / ArrayStore  — array element access
 │   ├─ ArrayLength             — array.length
 │   ├─ InstanceOf / CheckCast  — type tests
 │   ├─ Catch                   — the caught exception value at a handler entry
 │   ├─ ClassInitialization     — implicit <clinit> trigger before static access
 │   └─ VarArgsArray            — synthetic array wrapping vararg bootstrap args
 │
 └─ TupleNode                   — a node with multiple named outputs (projections)
     ├─ Method                  — the graph root; represents the method entry point
     ├─ ExceptionGuard          — marks the start of a try region; projects the
     │                            guarded body, each catch arm, and the exit path
     ├─ If                      — conditional branch; projects "true" and "false"
     ├─ LookupSwitch / TableSwitch — switch; projects each case and the default
     └─ (other control nodes)
```

Pure control nodes that carry no data value and are not tuple nodes include:
`MergeNode`, `LoopHeaderNode`, `LabelNode`, `Goto`, `Return`, `ReturnValue`, `Throw`,
`MonitorEnter`, `MonitorExit`, `CatchProjection`, `ExtractControlFlowProjection`.

### The graph root: `Method`

Every method graph starts at a `Method` node. It extends `TupleNode` and registers its
named projections (`"default"` for the control-flow entry, `"this"` for the receiver,
`"arg0"` … `"argN"` for parameters). All nodes that are *defined by* the method — like
argument projections and compile-time constants scoped to the method — record a
`DefinedByUse` edge back to the `Method` node. This keeps every reachable node anchored
to the graph even if it is not used as an operand anywhere.

### Merge and loop nodes

`MergeNode` marks a control-flow join point where two or more forward paths converge.
`LoopHeaderNode` is the specialised version for back-edge targets (loop entries). Both
can own PHI nodes that merge values coming from the different predecessors.

### `ExceptionGuard` — the try-region sentinel

`ExceptionGuard` (a `TupleNode`) is placed at the beginning of a try-block. It
pre-registers the following named projections:

- `"default"` — the continuation for the guarded body (an
  `ExtractControlFlowProjection`).
- `"exit"` — the normal exit path after the guard ends (another
  `ExtractControlFlowProjection`).
- `"catch_N"` — for each catch arm, a chain that goes through a `CatchProjection` into
  a `Catch` node (which is the value carrying the caught exception).

---

## 4. The Edge / Use System

Every edge in the graph is represented as a `UseEdge` stored in the *consuming* node's
`uses` list. A `UseEdge` holds:

- **`node`** — the node being depended upon (the producer / predecessor).
- **`use`** — the semantic type of the dependency (a `Use` subclass).

In addition each node has a `usedBy` set that is the reverse index: all nodes that
directly reference this node. This allows efficient traversal in both directions.

The `Use` class hierarchy:

```
Use
 ├─ ControlFlowUse(FlowType)   — sequencing edge; FlowType is FORWARD or BACKWARD
 ├─ MemoryUse                  — memory ordering edge
 ├─ DefinedByUse               — "I am lexically defined inside this scope node"
 ├─ ArgumentUse                — positional function argument
 └─ DataFlowUse                — a value dependency
     └─ PHIUse(origin, type)   — PHI input; records which control node the value
                                  comes from and whether the edge is FORWARD/BACKWARD
```

### Creating edges

`Node` provides fluent helper methods so edges are always consistent (both `uses` and
`usedBy` are updated atomically):

```java
// "this node's control flows forward into target"
source.controlFlowsTo(target, FlowType.FORWARD);

// "this node's memory state flows into target"
source.memoryFlowsTo(target);

// "PHI phi takes value v when coming from block origin"
phi.use(v, new PHIUse(FlowType.FORWARD, origin));
```

Constants and projections are created through `Node.define*` factory methods, which
automatically add the `DefinedByUse` back-edge:

```java
Node scope = ...;
PrimitiveInt c  = scope.definePrimitiveInt(42);     // DefinedByUse to scope
StringConstant s = scope.defineStringConstant("hi"); // DefinedByUse to scope
PHI phi          = scope.definePHI(IRType.CD_int);  // DefinedByUse to scope
```

---

## 5. Types — `IRType`

`IRType<T>` wraps the Java Class-File API's `ConstantDesc` hierarchy and adds helpers
useful for IR construction:

| Subclass | Wraps | Purpose |
|---|---|---|
| `IRType.MetaClass` | `ClassDesc` | Any class, interface, array or primitive type |
| `IRType.MethodType` | `MethodTypeDesc` | A method signature with resolved parameter / return `MetaClass` |
| `IRType.MethodHandle` | `MethodHandleDesc` | A method handle constant (for invokedynamic) |

Frequently used singleton types are pre-built as constants:
`IRType.CD_int`, `IRType.CD_long`, `IRType.CD_float`, `IRType.CD_double`,
`IRType.CD_boolean`, `IRType.CD_byte`, `IRType.CD_char`, `IRType.CD_short`,
`IRType.CD_void`, `IRType.CD_String`, `IRType.CD_Object`.

The JVM distinguishes *category-1* types (int, float, reference — occupy one local
variable slot) from *category-2* types (long, double — occupy two slots).
`TypeUtils.isCategory2(IRType)` encodes this distinction and is used throughout parsing
to advance the local-variable index by two for wide values.

---

## 6. The Parsing Pipeline — Six Steps

`MethodAnalyzer` (`ir/MethodAnalyzer.java`) contains the complete transformation logic.
Its public constructor drives the six-step pipeline:

```java
// Step 0 — read the code attribute
final CodeModel code = method.code().get();
stackMapTableAttribute = code.findAttribute(Attributes.stackMapTable()).orElse(null);

step1PrepareTryCatchBlocks(code);   // Step 1
step2AnalyzeCFG(code);              // Step 2
step3ComputeTopologicalOrder();     // Step 3
step4ComputeFrameDominators();      // Step 4
step5FollowCFGAndInterpret(code);   // Step 5
step6RemoveSingularPHIs(code);      // Step 6
```

Each step is explained below.

---

### Step 1 — Prepare try-catch blocks

**Goal:** Build a structured description of the try-catch table so that later steps can
easily look up which exception handlers are active at any bytecode position.

The Class-File API provides a flat `List<ExceptionCatch>`, where each entry holds:

| Field | Meaning |
|---|---|
| `tryStart` | Label at the beginning of the guarded region |
| `tryEnd` | Label just past the end of the guarded region |
| `handler` | Label of the exception handler entry point |
| `catchType` | The caught exception class, or empty for finally blocks |

`step1PrepareTryCatchBlocks` groups these flat entries into `TryCatchBlock` records,
keyed by `tryStart` label. A single try block may have multiple `CatchHandler` entries
(one per `catch` clause, plus optionally a `finally`). A `CatchHandler` records the
handler label and the set of exception types it covers. An absent `catchType` (the
finally case) is represented by `handler.markAsFinally()`.

After this step `tryCatchBlocks` is a `Map<Label, List<TryCatchBlock>>` that answers the
question: *"What try-catch blocks begin at this label?"*

---

### Step 2 — Analyse the Control-Flow Graph

**Goal:** Discover every bytecode position that is a reachable control-flow node, and
record for each one the set of predecessor positions together with the named projection
and flow type of each incoming edge.

The step works in two passes.

**Pass A — label mapping.**
The Class-File API model is a flat `List<CodeElement>`. A `LabelTarget` element does not
generate code; it just marks a position. The first pass maps every label to its index in
the element list so that branch targets can later be resolved to element indices.

**Pass B — BFS over the CFG.**
A work queue of `CFGAnalysisJob` items is processed. Each job starts from a given
element index. Processing walks forward through elements, tracking the current path (for
back-edge detection), until it reaches a control-flow terminator.

At each step:

| Element type | Action |
|---|---|
| `LabelTarget` | If the label starts a try block, enqueue jobs for all handler entry points and record them as FORWARD predecessors of those handler frames. |
| `GOTO` / `GOTO_W` | Unconditional branch: record the target frame as a FORWARD (or BACKWARD, if the target is on the current path) predecessor; stop processing this job. |
| Conditional branch (`IF_*`) | Record the *true* branch target with projection `"true"`. Fall through continues to the next element with projection `"false"`. |
| `LOOKUPSWITCH` / `TABLESWITCH` | Record each `"case_N"` target and the `"default"` target; stop processing this job. |
| `IRETURN`, `ARETURN`, `RETURN`, `ATHROW`, etc. | Stop processing this job. |
| Any other instruction | Record the next element as a FORWARD predecessor (`"default"` projection) and continue. |

A `Frame` object is created on first encounter of any reachable element index and stored
in the `frames[]` array indexed by element position.

**Flow types.** An edge is `FlowType.BACKWARD` when its target index is already on the
current DFS path (i.e. the branch is a loop back-edge). All other edges are
`FlowType.FORWARD`.

**Named projections.** Branch instructions produce multiple outgoing paths. These are
named so that when the interpreter later propagates the abstract state across the edge,
it can look up the right projection in a `TupleNode`. The convention is:

| Source | Projection name |
|---|---|
| Fall-through after conditional branch | `"false"` |
| Taken branch of conditional | `"true"` |
| Unconditional / normal sequential flow | `"default"` |
| Switch case N | `"case_N"` |
| Switch default | `"default"` |
| Exception handler N | `CatchProjection.nameFor(N, types)` |

---

### Step 3 — Compute Topological Order

**Goal:** Produce a list of reachable frames in **reverse-post-order** (RPO) of the
forward-edge CFG. This ordering guarantees that when the interpreter visits a frame in
step 5, all frames that can reach it via forward edges have already been processed.
Backward (loop) edges are deliberately excluded because their source frame has not yet
been processed when the target loop-header frame is visited.

The algorithm is an iterative DFS. It maintains a stack (`currentPath`) and a `marked`
set. It follows only FORWARD edges. When a node has no unvisited forward successors it
is appended to `reversePostOrder`. Reversing that list at the end yields the desired RPO.
Each frame records its position in the order via `frame.indexInTopologicalOrder`, which
is used in steps 4 and 5.

---

### Step 4 — Compute Frame Dominators

**Goal:** For every frame F, find its **immediate dominator** (idom): the unique frame
that dominates F and is dominated by all other dominators of F. The idom relationship
forms a tree rooted at the entry frame. It is used in step 5 to decide which control-
flow node should own PHI nodes created at a merge point.

MetaIR uses the classic **Cooper, Harvey & Kennedy** iterative algorithm (often called
the "Simple, Fast Dominators" algorithm). Because the frames are already in RPO, the
algorithm converges quickly — usually in one or two passes.

```
Entry.idom = Entry  (the entry frame dominates itself)

repeat until no change:
  for each frame V in RPO order (skip entry):
    newIdom = first predecessor of V whose idom is already computed
    for each other predecessor P of V with idom computed:
      newIdom = intersect(P, newIdom)
    V.idom = newIdom
```

The `intersect` operation walks up the idom chain of two frames simultaneously until
they meet, using the topological-order index to decide which one to advance:

```java
private Frame intersectIDoms(Frame v1, Frame v2) {
    while (v1 != v2) {
        // advance whichever is deeper in the RPO
        if (topologicalOrder.indexOf(v1) < topologicalOrder.indexOf(v2))
            v2 = v2.immediateDominator;
        else
            v1 = v1.immediateDominator;
    }
    return v1;
}
```

After this step every frame has its `immediateDominator` set.

---

### Step 5 — Abstract Interpretation and IR Emission

This is the core of the parser. It iterates frames in topological order and for each
frame either:

- **emits IR nodes** for the bytecode instruction at that position, or
- **creates structural control nodes** (MergeNode, LoopHeaderNode, ExceptionGuard) when
  the frame is a CFG join point.

See [Section 7](#7-the-abstract-interpreter-in-depth) for a detailed walkthrough of the
abstract interpreter state machine.

---

### Step 6 — Remove Singular PHI Nodes

**Goal:** Clean up PHI nodes that ended up with only a single data input. Such a PHI is
logically unnecessary — it just passes its one input through — and can be bypassed.

The step performs a full DFS traversal of the graph (via `DFS2`) and for each node
checks if any of its users is a PHI with exactly one `PHIUse` edge. If so:

1. The single input value's `usedBy` set no longer includes the PHI.
2. Every user of the PHI has its `UseEdge.node` pointer redirected from the PHI to the
   single input value.
3. The single input value's `usedBy` set gains all former users of the PHI.
4. The PHI's `DefinedByUse` back-edge is severed from its definer node.

After this step the PHI is no longer reachable from any live node and will be garbage
collected.

---

## 7. The Abstract Interpreter in Depth

### The `Status` Object

`Status` (`ir/Status.java`) is the mutable abstract interpreter state for a single
bytecode position. It contains:

| Field | Type | Meaning |
|---|---|---|
| `locals` | `Value[]` | Abstract values held in each JVM local variable slot |
| `stack` | `Stack<Value>` | Abstract values on the JVM operand stack |
| `control` | `Node` | The current control-flow node (the "program counter" in the IR) |
| `memory` | `Node` | The current memory node (head of the memory ordering chain) |
| `lineNumber` | `int` | The source line number active at this position |

Each frame has two `Status` instances:

- `frame.in` — the state arriving at the beginning of the frame.
- `frame.out` — the state produced after processing the frame.

`frame.copyIncomingToOutgoing()` initialises `frame.out` as a shallow copy of `frame.in`.
Every `parse_*` method calls this first and then mutates `frame.out`.

### The `Frame` Object

`Frame` (`ir/Frame.java`) is a build-time object associated with one position in the
`CodeModel` element list. It is discarded after construction. Its key fields:

| Field | Purpose |
|---|---|
| `elementIndex` | Index into the flat `CodeElement` list |
| `predecessors` | List of `FrameCFGEdge` recording where control flows from |
| `immediateDominator` | Set by step 4 |
| `indexInTopologicalOrder` | Set by step 3; -1 if unreachable |
| `entryPoint` | The first IR node emitted when processing this frame |
| `in` / `out` | Abstract interpreter status objects |
| `verificationInfos` | StackMapFrame entries from the class file (for debugging) |

### Processing a Single Frame

Step 5 visits each frame in topological order. The logic at the start of each iteration
determines `frame.in`:

**Case: first frame.**
`frame.in` is pre-initialised directly. The method arguments are placed in the local
variable slots according to the signature. `control` and `memory` both start at the
`Method` node.

**Case: exactly one forward predecessor.**
`frame.in` is a copy of the predecessor's `frame.out`. If the predecessor ended at a
`TupleNode` (e.g. an `If` or a `LookupSwitch`), the status's `control` is replaced with
the named projection matching the edge's projection name:

```java
if (incomingStatus.control instanceof TupleNode) {
    incomingStatus.control =
        ((TupleNode) incomingStatus.control).getNamedNode(edge.projection().name());
}
```

For exception-handler entries the stack is cleared (the JVM spec says only the thrown
exception is on the stack at a handler entry) and the caught exception value is pushed.

**Case: multiple forward predecessors (merge point).**
A `MergeNode` (or `LoopHeaderNode` if any predecessor is a back-edge) is created. All
incoming control edges are wired to it. Then:

1. **Stack merge.** For each stack depth position, if all incoming values are the same
   object, that value is propagated; otherwise a new PHI node is created.
2. **Local variable merge.** Same logic via `mergeFrames()`.
3. **Memory merge.** If all incoming memory nodes are the same, that node is kept;
   otherwise all memories flow into the new merge/loop node.

After building `frame.in`, the frame's bytecode instruction is dispatched to the
appropriate `parse_*` or `visit*` method via `visitNode()`.

### Merge Points and PHI Creation

`mergeFrames()` is called when two or more forward paths converge. For each local
variable slot it inspects all incoming `frame.out.locals[i]` values:

- If the slot is undefined in any incoming frame, skip it.
- If all incoming values are identical (same node identity) **and** there are no back
  edges, propagate the single value unchanged.
- Otherwise, create a PHI node owned by the immediate dominator's entry-point node and
  wire an input from each incoming frame:

```java
final PHI phi = phiOwner.definePHI(type);
for (Frame srcFrame : sourceFrames) {
    Value v = srcFrame.out.locals[i];
    if (v != null) {
        phi.use(v, new PHIUse(FlowType.FORWARD, srcFrame.out.control));
    }
}
targetStatus.setLocal(i, phi);
```

The PHI owner is found by walking up the idom chain from the target frame to the root
dominator. This is the node in the IR graph that best represents the scope in which the
merge occurs.

### Back Edges and Loop Headers

When step 2 detects that a branch target is already on the current DFS path, it records
the edge as `FlowType.BACKWARD`. When step 5 encounters a frame with at least one
backward predecessor, it creates a `LoopHeaderNode` instead of a `MergeNode`.

Forward predecessors of the loop header are wired immediately. Backward predecessors
(the loop's closing edge) cannot be wired yet because their source frame has not been
processed. Instead, `handlePotentialBackedgeFor()` is called after the *source*
instruction of the back-edge is parsed. At that point:

1. It wires the control-flow back-edge:
   ```java
   outgoing.control.controlFlowsTo(targetFrame.entryPoint, FlowType.BACKWARD);
   ```
2. For every local variable slot that already holds a PHI at the loop header, it adds
   the current loop-iteration value as a `PHIUse(FlowType.BACKWARD, jumpSource)` input.

This deferred wiring means loop PHIs get their initial values from the pre-loop forward
path and their loop-carried values from the back-edge path — exactly the SSA invariant.

---

## 8. Exception Handling

Exception handling is the most structurally complex part of the IR. It involves four
node types working together: `ExceptionGuard`, `CatchProjection`, `Catch`, and the
`MergeNode` that joins the guarded body's normal exit with the guard's exit projection.

### Construction

When step 5 reaches a `LabelTarget` that starts a try region, it creates an
`ExceptionGuard` node and connects it to the current control node. `ExceptionGuard`'s
constructor immediately creates and wires all its sub-nodes:

```
control ──► ExceptionGuard
               ├─ "default" ──► ExtractControlFlowProjection  (guarded body entry)
               ├─ "exit"    ──► ExtractControlFlowProjection  (normal post-guard exit)
               └─ "catch_N" ──► CatchProjection ──► Catch     (exception value node)
```

The guarded body executes under the `"default"` projection. All code inside the try
block uses this projection as its active control node. The `"exit"` projection is the
path taken when control reaches the `LabelTarget` marking the end of the try region
normally.

When step 5 encounters the `LabelTarget` that *ends* a try region, it:
1. Locates the active `ExceptionGuard` by walking forward control edges from the current
   control node looking for an `ExceptionGuard` whose `startLabel` matches.
2. Creates a `MergeNode` named `"EndOfGuardedBlock_N"`.
3. Wires both the current control (end of the guarded body) and the guard's `"exit"`
   projection into that merge node.

### Exception handler entry

When step 5 encounters a frame that is an exception handler entry (its label is in the
`exceptionHandlers` set), it clears the abstract stack and pushes the `Catch` node's
value (the caught exception). For a frame with a single predecessor, the `Catch` node is
already the active control node at that point because the `ExceptionGuard` projection
chain ends there.

---

## 9. Instruction-Level Parsing Details

Each bytecode category is handled by one or more `parse_*` methods. They all follow the
same pattern:

```java
final Status outgoing = frame.copyIncomingToOutgoing();  // clone in → out
// ... pop operands from outgoing.stack ...
// ... create IR node(s) ...
// ... push result(s) onto outgoing.stack if needed ...
// ... update outgoing.control and/or outgoing.memory ...
// frame.entryPoint = <first IR node created>
```

### Constants

`ICONST_*`, `BIPUSH`, `SIPUSH`, `LDC`, `LCONST_*`, `FCONST_*`, `DCONST_*`,
`ACONST_NULL` all call `control.define*()` factory methods and push the result:

```java
// ICONST_0 .. ICONST_5, ICONST_M1:
outgoing.push(outgoing.control.definePrimitiveInt((Integer) constantValue));

// ACONST_NULL:
outgoing.push(outgoing.control.defineNullReference());
```

`LDC` / `LDC_W` / `LDC2_W` go through `constantToValue()` which dispatches on the
`ConstantDesc` runtime type and can produce `PrimitiveInt`, `PrimitiveLong`,
`PrimitiveFloat`, `PrimitiveDouble`, `StringConstant`, `RuntimeclassReference`,
`MethodType`, or `MethodHandle` nodes.

### Local Variables

`LOAD` instructions (`ALOAD`, `ILOAD`, `LLOAD`, `FLOAD`, `DLOAD` in all their
slot-encoded forms) read a value from `frame.in.getLocal(slot)` and push it onto the
stack. **No new IR node is created.** The same `Value` object that was stored in the
local slot is reused on the stack — this is the key benefit of SSA form: a variable use
is just a reference to the node that defined it.

`STORE` instructions (`ASTORE`, `ISTORE`, etc.) pop a value from the stack and write it
into `frame.out.setLocal(slot, v)`. Again, **no new IR node is created** for the
assignment itself. The value's definition node is simply associated with that local slot
in the outgoing abstract state.

`IINC` is special: it synthesises an `Add` node inline:
```java
outgoing.setLocal(slot,
    new Add(IRType.CD_int, frame.in.getLocal(slot),
            outgoing.control.definePrimitiveInt(constant)));
```

### Stack Manipulation (DUP, POP, SWAP)

`DUP`, `DUP_X1`, `DUP_X2`, `DUP2`, `DUP2_X1`, `DUP2_X2`, `POP`, `POP2`, `SWAP` are
pure abstract-stack manipulations. They rearrange references in `Status.stack` but
create **no IR nodes**. After construction these opcodes are completely invisible in the
graph — they exist only to satisfy the JVM's stack discipline during parsing.

### Arithmetic and Bitwise Operations

Binary operations pop two values and push one result node:

```java
// IADD:
Value value2 = outgoing.pop();
Value value1 = outgoing.pop();
outgoing.push(new Add(IRType.CD_int, value1, value2));
```

The full set of binary IR nodes: `Add`, `Sub`, `Mul`, `Div`, `Rem`, `BitOperation`
(AND/OR/XOR/SHL/SHR/USHR), `NumericCompare` (LCMP, FCMPG/L, DCMPG/L).

Unary operations pop one value: `Negate` (INEG/LNEG/FNEG/DNEG), `ArrayLength`
(ARRAYLENGTH).

### Conversions and Widening

JVM conversion instructions map to two IR node types:

- **`Convert`** — for conversions where the bit representation changes
  (I2F, L2D, F2I, etc.).
- **`Truncate` + `Extend`** — for sub-int narrowing/widening (I2B, I2C, I2S):
  the value is first truncated to the target bit width, then sign- or zero-extended
  back to `int`.
- **`Extend` alone** — for I2L (sign-extend `int` to `long`).

### Field Access

`GETFIELD` pops the receiver, creates a `GetField` node, and pushes it. A memory edge
is also added: `outgoing.memory = outgoing.memory.memoryFlowsTo(get)`.

`PUTFIELD` pops value and receiver, creates a `PutField` node. Both a memory edge and a
control edge are added, because `PutField` has an observable side effect.

`GETSTATIC` additionally materialises a `RuntimeclassReference` for the declaring class
and passes it through a `ClassInitialization` node to model the implicit `<clinit>`
trigger. The memory chain passes through `ClassInitialization` first, then through
`GetStatic`.

`PUTSTATIC` follows the same `ClassInitialization` pattern and also adds a control edge.

### Method Invocations

All four invoke variants follow the same structure:

1. Pop `N` arguments from the stack (in reverse: the last argument was pushed last).
2. For non-static invocations, pop the receiver.
3. Resolve the method via `resolverContext`.
4. Create the appropriate invoke node (`InvokeSpecial`, `InvokeVirtual`,
   `InvokeInterface`, `InvokeStatic`).
5. Add a control edge and a memory edge (all calls are considered to have observable
   side effects).
6. If the return type is non-void, push the invoke node itself as the result value.

`INVOKESTATIC` additionally creates a `ClassInitialization` node before the call, to
model the required class initialization trigger.

### Object and Array Creation

`NEW` creates a `New` node and pushes it. No memory edge is added here — memory is
consumed by the subsequent `INVOKESPECIAL` (`<init>`) call.

`NEWARRAY` / `ANEWARRAY` pop the length, create a `NewArray` node, push it, and add a
memory edge.

`MULTIANEWARRAY` pops N dimension lengths, creates a `NewMultiArray` node, and adds a
memory edge.

### Branches and Conditional Jumps

All conditional branches follow this pattern:

1. Pop one or two values from the stack.
2. Wrap them in a condition node (`NumericCondition`, `ReferenceCondition`, or
   `ReferenceTest`).
3. Create an `If` node wrapping the condition.
4. Wire the current control into the `If`: `outgoing.control = outgoing.control.controlFlowsTo(If, FORWARD)`.
5. Set `frame.entryPoint = If`.
6. Call `handlePotentialBackedgeFor()` to handle the case where the taken branch is a
   loop back-edge.

The fall-through path (projection `"false"`) and the taken path (projection `"true"`)
are resolved by step 2 which already recorded them as named projections on the
successor frames. When step 5 later processes those successor frames it extracts the
right projection from the `If` TupleNode.

`GOTO` / `GOTO_W` create a `Goto` node, wire it into the control chain, then call
`handlePotentialBackedgeFor()` to wire the back-edge if the jump is a loop back-edge.

### Switch Instructions

`LOOKUPSWITCH` and `TABLESWITCH` pop the key value and create a `LookupSwitch` or
`TableSwitch` node. These are `TupleNode`s whose projections are named `"case_N"` and
`"default"`. The successor frames receive those projections via step 2's CFG analysis.

### Return and Throw

`RETURN` creates a `Return` node. `IRETURN`, `ARETURN`, etc. pop the return value and
create a `ReturnValue` node. Both add a control edge and a memory edge.

`ATHROW` pops the exception value, creates a `Throw` node with a control and a memory
edge, and sets it as the frame entry point.

### invokedynamic

`INVOKEDYNAMIC` is the most complex single instruction. It is lowered completely at
parse time into a visible bootstrap invocation:

1. A `RuntimeclassReference` for the caller class is created.
2. A `MethodHandles.Lookup.in(caller)` call is emitted as an `InvokeStatic`.
3. The target method name is emitted as a `StringConstant`.
4. The call-site `MethodType` is emitted via `constantToValue`.
5. Each additional bootstrap argument is emitted via `constantToValue`.
6. If the bootstrap method has a vararg parameter, remaining bootstrap args are wrapped
   in a `VarArgsArray`.
7. The bootstrap method is called as an `InvokeStatic` (`bootstrapInvocation`).
8. Finally, an `InvokeDynamic` node is created, taking the bootstrap result and the
   dynamic arguments from the stack.

Memory and control edges are added to the `InvokeDynamic` node.

---

## 10. The Memory Chain

The JVM's memory model requires that memory operations (field reads/writes, array
accesses, allocations, calls) are not reordered past each other in ways that would
change observable behaviour. In the Sea-of-Nodes representation this ordering is
captured by a **memory chain** — a sequence of `MemoryUse` edges threading through all
memory-affecting nodes.

The `Status.memory` field always points to the last node that touched memory. When a new
memory-affecting node is created, it is inserted at the end of the chain:

```java
outgoing.memory = outgoing.memory.memoryFlowsTo(newNode);
```

`memoryFlowsTo(target)` adds a `MemoryUse` edge from `this` to `target` and returns
`target`, so the assignment advances the chain head.

At merge points, if multiple incoming paths have different memory heads, all of them are
wired into the merge/loop node:

```java
for (Node mem : incomingMemories) {
    mem.memoryFlowsTo(target);  // target is the MergeNode or LoopHeaderNode
}
incomingStatus.memory = target;
```

This ensures that any node scheduled after the merge is guaranteed to see the effects of
all paths that fed into it.

---

## 11. Consuming the IR — The `Sequencer`

> A comprehensive description of the sequencer and the code-generation backend
> interface is in [Sequencer.md](SEQUEMCER.md). This section provides a brief overview.


After construction, the graph is a pure dependency graph with no inherent textual order.
To emit code for a target (OpenCL C, WebAssembly, etc.) it must be linearised back into
a structured sequence.

`Sequencer` (`ir/Sequencer.java`) does this by walking the **IR-level dominator tree**
(computed by `CFGDominatorTree` on the `Node` graph, not the `Frame` graph). The core
insight is: if node B is immediately dominated by node A in the IR graph, then B is
always executed directly after A, so B can be emitted immediately after A without any
jump.

When a node has a successor that it does not immediately dominate (e.g. a branch to a
loop header or to a merge node that is dominated by an ancestor), a `GOTO` is emitted
instead.

`Sequencer` delegates all concrete output to a `StructuredControlflowCodeGenerator`
interface, which receives events such as:

- `begin(resolvedMethod, method)` — method entry
- `write(node)` — emit a specific IR node
- `writePHINodesFor(node)` — emit all PHI nodes owned by this node
- `writePreJump(node)` — called before a GOTO is generated
- `finished()` — signal that sequencing is complete

This separation allows the same `Sequencer` to drive generation of OpenCL C, WASM, or
any other target by providing a different `StructuredControlflowCodeGenerator`
implementation.

---

## 12. Graph Traversal Utilities

Two traversal helpers are provided:

**`DFS2`** (`ir/DFS2.java`) performs a topological sort of IR nodes, respecting all
dependency edges (control, data, memory, defined-by). It can optionally be restricted to
control-flow edges only. It has a safety counter (limit 5000 iterations) to detect
unschedulable cycles. Used by step 6 and by `DominatorTree`.

**`DominatorTree`** (`ir/DominatorTree.java`) computes the immediate-dominator tree of
the IR node graph (as opposed to the `Frame` CFG dominator tree used during parsing).
It exposes:
- `getIDom(node)` — immediate dominator of a node
- `dominates(a, b)` — does node `a` dominate node `b`?
- `getStrictDominators(node)` — all nodes that strictly dominate a node
- `immediatelyDominatedNodesOf(node)` — direct children in the dominator tree
- `domSetOf(node)` — the complete domination subtree

Used by `CFGDominatorTree` and `Sequencer`.

---

## 13. Worked Example — Simple Diamond Shape

Consider the following Java method:

```java
public int diamond(int x) {
    int r;
    if (x > 0) {
        r = 1;
    } else {
        r = -1;
    }
    return r;
}
```

The bytecode roughly looks like:

```
  0: iload_1          // push x
  1: ifle ELSE        // if x <= 0 jump to ELSE
  4: iconst_1
  5: istore_2         // r = 1
  6: goto END
ELSE:
  9: iconst_m1
 10: istore_2         // r = -1
END:
 11: iload_2          // push r
 12: ireturn
```

**After the pipeline:**

```
Method
  │ (DefinedByUse)
  ├─ ExtractMethodArgProjection[x : int]   ← local slot 1
  │
  └─► If(NumericCondition[GT, x, 0])
        │ "true"
        │   LabelNode[true-branch]
        │     (r_true = PrimitiveInt[1])     ← no IR node; stored in local slot 2
        │
        │ "false"
        │   LabelNode[false-branch]
        │     (r_false = PrimitiveInt[-1])   ← no IR node; stored in local slot 2
        │
        └─► MergeNode[Merge11]
              │ (DefinedByUse)
              └─ PHI[int] ← PHIUse(r_true, true-branch-control)
                          ← PHIUse(r_false, false-branch-control)
              │
              └─► ReturnValue(PHI[r])
```

Key observations:
- `x` is represented by a single `ExtractMethodArgProjection` node throughout; no
  copies.
- The constants `1` and `-1` are `PrimitiveInt` nodes owned (via `DefinedByUse`) by the
  control node active when they were created.
- `istore_2` / `iload_2` do not generate any nodes — they only update and read the
  abstract local slot.
- After step 6, if the code were restructured so only one path exists (e.g. after
  constant folding of `x > 0`), the PHI would be singular and removed.