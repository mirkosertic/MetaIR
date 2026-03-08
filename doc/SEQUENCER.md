# MetaIR — Sequencer and Code Generation Architecture

This document explains how a MetaIR graph — which has no inherent linear order — is
turned into structured, executable code for a target language such as OpenCL C or
WebAssembly. It is a companion to [IR.md](IR.md) and assumes familiarity with the node
and edge concepts described there.

---

## Table of Contents

1. [The Problem: From Graph to Code](#1-the-problem-from-graph-to-code)
2. [The Core Insight: Dominator-Guided Linearisation](#2-the-core-insight-dominator-guided-linearisation)
3. [CFGDominatorTree](#3-cfgdominatortree)
4. [The Block Stack — Structured Jumps Without goto](#4-the-block-stack--structured-jumps-without-goto)
5. [Sequencer — The Walk](#5-sequencer--the-walk)
   - [Entry Point](#entry-point)
   - [`visitDominationTreeOf` — the main loop](#visitdominationtreeof--the-main-loop)
   - [`followUpProcessor` — inline or jump?](#followupprocessor--inline-or-jump)
   - [`generateGOTO` — resolving jumps to structured breaks/continues](#generategoto--resolving-jumps-to-structured-breakscontinues)
6. [Branching Nodes — the Template Pattern](#6-branching-nodes--the-template-pattern)
   - [`visitBranchingNodeTemplate`](#visitbranchingnodetemplate)
   - [If — conditional branches](#if--conditional-branches)
   - [LoopHeaderNode — loops](#loopheadernode--loops)
   - [LookupSwitch and TableSwitch — switch statements](#lookupswitch-and-tableswitch--switch-statements)
   - [ExceptionGuard — try-catch](#exceptionguard--try-catch)
   - [Catch — exception handler body](#catch--exception-handler-body)
7. [Terminator Nodes](#7-terminator-nodes)
8. [StructuredControlflowCodeGenerator — the Backend Interface](#8-structuredcontrolflowcodegenerator--the-backend-interface)
   - [Statement-level callbacks](#statement-level-callbacks)
   - [Expression emission — `emit`](#expression-emission--emit)
   - [Temporary variables](#temporary-variables)
   - [PHI node emission](#phi-node-emission)
   - [The `writePreJump` hook](#the-writeprejump-hook)
9. [Implementing a Backend](#9-implementing-a-backend)
10. [Worked Example — Loop with PHI](#10-worked-example--loop-with-phi)
11. [Error Handling](#11-error-handling)

---

## 1. The Problem: From Graph to Code

The MetaIR graph produced by `MethodAnalyzer` is a pure dependency structure. Nodes are
connected by edges that state *what depends on what*, but the graph imposes no particular
textual order. Every valid topological sort of the graph is a valid execution order.

A target language like OpenCL C or WebAssembly expects a *linear* sequence of
statements with *structured* control flow: `if/else`, `for`, `while`, `break`,
`continue`, `try/catch`. It cannot express arbitrary jumps (or, in the case of WASM,
can only express them through labelled blocks with `br` instructions that map to `break`
and `continue`).

The `Sequencer` solves this in two steps:

1. **Linearise** the control-flow graph into a valid statement order.
2. **Map** every edge that crosses a control structure boundary to a structured
   `break` or `continue`.

---

## 2. The Core Insight: Dominator-Guided Linearisation

A node A **dominates** a node B if *every* path from the method entry to B passes
through A. The **immediate dominator** (idom) of B is the closest such A. The idom
relationship forms a tree rooted at the method entry.

The key observation is:

> If A immediately dominates B in the control-flow graph, then on every execution path
> that reaches B, A was the last control-flow node executed before B. Therefore B can
> always be emitted *directly after* A in the output without any jump.

This means the sequencer can emit code simply by walking the **dominator tree**:

- At each node, emit the node's statement.
- Then recurse into the node's immediately dominated successor.
- If a control-flow edge points to a node that A does *not* immediately dominate (i.e.
  a join point further up the tree, or a loop back-edge), emit a structured jump instead
  of attempting to inline the target.

This approach is a simplified form of the algorithm described in
*"Beyond Relooper: recursive translation of unstructured control flow to structured
control flow"* and is well-suited to the structured-output languages MetaIR targets.

---

## 3. CFGDominatorTree

`CFGDominatorTree` (`ir/CFGDominatorTree.java`) computes the dominator tree of the IR
node graph, considering **only forward control-flow edges** (`ControlFlowUse` with
`FlowType.FORWARD`). Back-edges (loop closing edges) are ignored during dominator
computation because they do not constrain the domination structure of the forward graph.

It is distinct from the `DominatorTree` used in other parts of the framework:

| Class | Edge set considered | Used by |
|---|---|---|
| `CFGDominatorTree` | Control-flow edges only (forward) | `Sequencer` |
| `DominatorTree` | All edges (control, data, memory, defined-by) | Graph analysis utilities |

`CFGDominatorTree` uses `DFS2(start, true)` (control-flow-only DFS) to produce a
pre-order traversal, then runs the Cooper-Harvey-Kennedy iterative dominator algorithm
on that ordering. It also computes a **reverse-post-order (RPO)** of the forward
control-flow edges for use in sorting merge-node children.

Key methods used by `Sequencer`:

| Method | Purpose |
|---|---|
| `getIDom(node)` | Returns the immediate dominator of a node |
| `immediatelyDominatedNodesOf(node)` | Returns all nodes whose idom is the given node |
| `getRpo()` | Returns the full RPO list, used to sort dominated merge nodes |

---

## 4. The Block Stack — Structured Jumps Without goto

Many structured target languages do not support arbitrary `goto`. Instead, they allow
labelled blocks with `break LABEL` (jump to after the block) and `continue LABEL` (jump
to the top of a loop block). The `Sequencer` translates all control-flow edges that
cross a dominator boundary into one of these two forms.

To do this, the sequencer maintains a **block stack** (`Deque<Block>`). A `Block` is a
lightweight record:

```java
class Block {
    enum Type { LOOP, NORMAL }

    String label;          // unique string label for break/continue targeting
    Type   type;           // LOOP or NORMAL
    Node   continueLeadsTo; // which IR node a "continue" to this block reaches
    Node   breakLeadsTo;   // which IR node a "break" from this block reaches
}
```

Whenever a branching construct is entered:

- **Loop header** — a `LOOP` block is pushed. Its `continueLeadsTo` is the loop header
  node itself (a `continue` goes back to the loop top). Its `breakLeadsTo` is `null`
  (the loop exit is handled by the merge nodes below).
- **Each merge node** dominated by the branching node — a `NORMAL` block is pushed. Its
  `breakLeadsTo` is that merge node (a `break` jumps to the join point after the
  branch arm).

The blocks are pushed in reverse-RPO order so that the innermost (most specific) join
point is checked first when resolving a jump. When the arm is finished, the blocks are
popped and the merge node's subtree is visited.

When `generateGOTO(currentNode, target, activeStack)` is called, it scans the block
stack from top to bottom looking for the first block whose `breakLeadsTo == target` or
`continueLeadsTo == target`. It then calls the appropriate backend method
(`writeBreakTo` or `writeContinueTo`). If no matching block is found, a
`IllegalStateException` is thrown — indicating a sequencing bug.

---

## 5. Sequencer — The Walk

### Entry Point

```java
new Sequencer(resolvedMethod, method, codeGenerator);
```

The constructor:
1. Builds the `CFGDominatorTree` from the `Method` node.
2. Calls `visitDominationTreeOf(method, emptyStack)`.
3. Calls `codegenerator.finished()` when done.

The walk starts at the `Method` node, which is the root of both the IR graph and the
dominator tree.

### `visitDominationTreeOf` — the main loop

This is the central recursive procedure. It takes a starting node and the current block
stack, then iterates forward through directly-dominated control-flow successors:

```
current = startNode
while current != null:
    dispatch current to its handler (see node table below)
    current = followUpProcessor(current)  OR  null if branching/terminating
close any blocks pushed since we entered this call
```

After the loop, any blocks that were pushed during this call (i.e. blocks whose stack
depth exceeds the depth at entry) are closed via `codegenerator.finishBlock(...)`.

The per-node dispatch is a `switch` over the concrete node type:

| Node type | Action |
|---|---|
| `Method` | `begin()`, `writePHINodesFor()`, then follow up |
| `LabelNode`, `MergeNode` | `writePreJump()`, `writePHINodesFor()`, `write()`, then follow up |
| Side-effecting values: `ArrayStore`, `PutField`, `PutStatic`, `InvokeSpecial`, `InvokeVirtual`, `InvokeInterface`, `InvokeStatic`, `InvokeDynamic`, `ClassInitialization`, `Div`, `Rem`, `MonitorEnter`, `MonitorExit`, `CheckCast`, `ArrayLoad` | `writePHINodesFor()`, `write()`, then follow up |
| `ExtractControlFlowProjection` | transparent — follow up without emitting anything |
| `Return`, `ReturnValue`, `Throw` | `write()`, then `current = null` (terminates) |
| `Goto` | `writePreJump()`, then either follow up inline (if target is dominated by current) or `generateGOTO()` + `current = null` |
| `If`, `LookupSwitch`, `TableSwitch` | `writePHINodesFor()`, delegate to `visit(node, ...)`, then `current = null` |
| `LoopHeaderNode`, `ExceptionGuard`, `Catch` | delegate to `visit(node, ...)`, then `current = null` |

### `followUpProcessor` — inline or jump?

`followUpProcessor` is a lambda that finds the unique control-flow successor of the
current node and decides whether to continue inline or emit a jump:

```
for each node U in current.usedBy:
    for each edge E in U.uses where E.node == current and E is ControlFlowUse:
        if dominatorTree.getIDom(U) == current:
            return U          // U is immediately dominated → emit inline
        else:
            writePreJump(current)
            generateGOTO(current, U, activeStack)
            // fall through; return null at end
return null
```

**Inline case:** The successor U is the idom child of the current node. This means U is
always reached directly from current and from nowhere else in this subtree — it can be
emitted in-place without a label or a jump.

**Jump case:** U is reachable from current but U's idom is a ancestor higher in the
tree. This means U is a join point that other paths also flow into. A structured jump
(`break LABEL`) is emitted instead, and the current chain terminates (`null` returned).

### `generateGOTO` — resolving jumps to structured breaks/continues

```java
private void generateGOTO(Node current, Node target, Deque<Block> activeStack) {
    for (Block b : activeStack) {
        if (b.breakLeadsTo == target) {
            codegenerator.writeBreakTo(b.label, current, target);
            return;
        }
        if (b.continueLeadsTo == target) {
            codegenerator.writeContinueTo(b.label, current, target);
            return;
        }
    }
    throw new IllegalStateException("GOTO not properly handled");
}
```

The stack is searched from top (innermost scope) to bottom. The first matching block
wins. This correctly handles nested loops where a `break` or `continue` must target a
specific outer block.

---

## 6. Branching Nodes — the Template Pattern

All nodes that introduce multiple outgoing control-flow paths use a shared template
method before invoking their specific logic. This ensures consistent block-stack
management.

### `visitBranchingNodeTemplate`

```
orderedBlocks = immediatelyDominatedNodesOf(node)
                    .filter(MergeNode)
                    .sortedByRPO(descending)

if node is LoopHeaderNode:
    push LOOP block (continueLeadsTo = node)
    startBlock(loopBlock)

for each mergeNode M in orderedBlocks:
    push NORMAL block (breakLeadsTo = M)
    startBlock(normalBlock)

nodeCallback(activeStack)   // node-specific body

for each M in orderedBlocks (reverse order):
    pop block, finishBlock
    visitDominationTreeOf(M, activeStack)   // emit merge node and its subtree

if node is LoopHeaderNode:
    pop block, finishBlock(loopBlock)
```

**Why descending RPO for merge nodes?** The merge nodes are ordered from *last* to
*first* in RPO and pushed onto the stack in that order. The *first* merge node in RPO
ends up at the top of the stack after pushing. When an inner `break` resolves a jump
to that merge node, it matches the topmost (innermost) block first — exactly what
structured control flow requires.

After the callback, merge nodes are visited in *reverse* order of how they were pushed
(i.e. first-to-last in RPO), so the first join point in the output is emitted before
the later ones.

### If — conditional branches

```
visitBranchingNodeTemplate(iff, activeStack, blocks -> {
    startIfWithTrueBlock(iff)
    visitDominationTreeOf(iff.trueProjection(), blocks)   // true arm
    startIfElseBlock(iff)
    visitDominationTreeOf(iff.falseProjection(), blocks)  // false arm
    finishIfBlock()
})
```

`If` is a `TupleNode` with two named projections: `"true"` and `"false"`, each an
`ExtractControlFlowProjection`. The sequencer visits each projection's subtree in turn.
Any `break` that needs to jump to the `MergeNode` after the if/else is handled by the
`NORMAL` block that `visitBranchingNodeTemplate` pushed for that merge node.

### LoopHeaderNode — loops

```
visitBranchingNodeTemplate(loopHeader, activeStack, blocks -> {
    visitDominationTreeOf(followUpProcessor(loopHeader), blocks)
})
```

A `LoopHeaderNode` has exactly one forward control-flow successor (the loop body entry).
The template pushes a `LOOP` block so that any back-edge jump inside the body can emit
`continue LABEL` to target the loop header. The merge node immediately dominated by the
loop header (the loop exit join point) gets a `NORMAL` block so that any `break` inside
the body can exit the loop.

### LookupSwitch and TableSwitch — switch statements

```
visitBranchingNodeTemplate(switchNode, activeStack, blocks -> {
    startLookupSwitch(switchNode)                             // or startTableSwitch
    for each case i:
        writeSwitchCase(i)
        visitDominationTreeOf(switchNode.caseProjection(i), blocks)
        finishSwitchCase()
    writeSwitchDefaultCase()                                  // or startTableSwitchDefaultBlock
    visitDominationTreeOf(switchNode.defaultProjection(), blocks)
    finishSwitchDefault()                                     // or finishTableSwitchDefaultBlock
    finishLookupSwitch()                                      // or finishTableSwitch
})
```

Each switch arm is a separate sub-walk. If an arm falls through to a merge node (e.g.
no `break` at the end), `generateGOTO` will emit a `break LABEL` to the appropriate
`NORMAL` block that was pushed for that merge node.

### ExceptionGuard — try-catch

```
visitBranchingNodeTemplate(guard, activeStack, blocks -> {
    if guard has catch handlers:
        startTryCatch(guard)
    visitDominationTreeOf(guard.guardedBlock(), blocks)       // try body
    if guard has catch handlers:
        startCatchBlock()
        for each catch arm i:
            visitDominationTreeOf(guard.catchProjection(i), blocks)
        writeRethrowException()
        finishTryCatch()
})
```

`ExceptionGuard` projects three kinds of paths (see [IR.md](IR.md) Section 8):
- `"default"` → the guarded body (`guard.guardedBlock()`)
- `"catch_N"` → each `CatchProjection` → `Catch` node
- `"exit"` → the normal exit path

The template handles the merge node at the end of the guard (the `MergeNode` labelled
`"EndOfGuardedBlock_N"` created during parsing) by pushing a `NORMAL` block for it, so
that a `break` from within the guarded body or catch handlers can jump to the post-guard
code.

### Catch — exception handler body

```
startCatchHandler(node.exceptionTypes)
visitDominationTreeOf(followUpProcessor(node), activeStack)
finishCatchHandler()
```

A `Catch` node is the exception value node inside the handler. Its control-flow
successor is the first statement of the handler body. `followUpProcessor` finds that
successor, and the body is visited as a sub-walk.

---

## 7. Terminator Nodes

Three node types end the current walk branch unconditionally:

| Node | Backend call | Notes |
|---|---|---|
| `Return` | `write(ret)` | Void method return |
| `ReturnValue` | `write(returnValue)` | Non-void return; value is emitted via `emit()` |
| `Throw` | `write(th)` | Exception throw |

After these, `current` is set to `null` and the while-loop exits.

`Goto` is a semi-terminator: if its target is immediately dominated by the `Goto` node
itself, it continues inline; otherwise it calls `generateGOTO` and terminates.

---

## 8. StructuredControlflowCodeGenerator — the Backend Interface

`StructuredControlflowCodeGenerator<T>` (`ir/StructuredControlflowCodeGenerator.java`)
is the abstract class that all code generation backends must implement. It is
parameterised on `T extends GeneratedThing`, which is the type the backend uses to
represent a generated expression fragment (e.g. a string of source code, or a WASM
instruction reference).

The interface is split into two layers: **statement-level** and **expression-level**.

### Statement-level callbacks

These are called by the `Sequencer` in the order they should appear in the output.

**Method lifecycle:**

| Method | When called |
|---|---|
| `begin(resolvedMethod, method)` | Once, at the very start |
| `finished()` | Once, after the entire graph has been walked |

**Block management:**

| Method | When called |
|---|---|
| `startBlock(block)` | When a structured block scope is opened (for break/continue targeting) |
| `finishBlock(block)` | When that block scope is closed |
| `writeBreakTo(label, current, target)` | When a forward jump to a post-block merge is emitted |
| `writeContinueTo(label, current, target)` | When a backward jump to a loop header is emitted |

**Control flow:**

| Method | When called |
|---|---|
| `startIfWithTrueBlock(iff)` | Opens the `if` statement and its true branch |
| `startIfElseBlock(iff)` | Transitions from true branch to false (else) branch |
| `finishIfBlock()` | Closes the entire `if/else` |
| `startLookupSwitch(node)` | Opens a lookup-switch statement |
| `writeSwitchCase(key)` | Begins one case arm |
| `finishSwitchCase()` | Closes a case arm |
| `writeSwitchDefaultCase()` | Begins the default arm |
| `finishSwitchDefault()` | Closes the default arm |
| `finishLookupSwitch()` | Closes the lookup-switch |
| `startTableSwitch(node)` | Opens a table-switch statement |
| `startTableSwitchDefaultBlock()` | Begins the default arm of a table-switch |
| `finishTableSwitchDefaultBlock()` | Closes the default arm |
| `finishTableSwitch()` | Closes the table-switch |
| `startTryCatch(guard)` | Opens a try block |
| `startCatchBlock()` | Opens the combined catch block |
| `startCatchHandler(exceptionTypes)` | Begins one specific catch handler arm |
| `writeRethrowException()` | Emits a re-throw after all handled cases |
| `finishCatchHandler()` | Closes a catch handler arm |
| `finishTryCatch()` | Closes the entire try-catch |

**Statements and values:**

| Method | When called |
|---|---|
| `writePHINodesFor(node)` | Before the node's own statement; emits variable assignments for all PHI nodes owned by this node |
| `write(Return/ReturnValue/Throw/...)` | For each side-effecting statement node |
| `writePreJump(node)` | Immediately before any jump is about to be emitted; allows flushing pending expressions |

### Expression emission — `emit`

Pure data-flow nodes (those that compute a value without observable side effects) are
not emitted as statements — they are emitted **on demand** as sub-expressions whenever
they appear as operands of a statement node.

The backend base class provides `emit(node)` and `emit(node, expressionStack,
evaluationStack)` which drive a recursive bottom-up traversal:

```
emit(node):
    push node onto expressionStack      // cycle detection / context
    result = visit(node, ...)           // dispatch to visit_* method
    if node is not constant AND is used more than once:
        result = emitTemporaryVariable(result)   // extract to a named temporary
    push result onto evaluationStack
    pop expressionStack
```

The `expressionStack` provides context about the current expression being evaluated (it
can be used by backends for formatting). The `evaluationStack` accumulates generated
fragments bottom-up, so that when a binary operator's `visit_Add` is called, both
operands are already at the top of the `evaluationStack`.

The concrete `visit_*` methods are the only thing a backend must implement for
expressions. The full set:

```
visit_PrimitiveInt / PrimitiveLong / PrimitiveFloat / PrimitiveDouble
visit_Add / Sub / Mul / Div / Rem / Negate
visit_BitOperation / NumericCompare / NumericCondition
visit_Convert / Truncate / Extend
visit_ExtractThisRefProjection / ExtractMethodArgProjection
visit_StringConstant / Null / RuntimeClassReference
visit_GetField / GetStatic
visit_New / NewArray / NewMultiArray / VarArgsArray / ArrayLength / ArrayLoad
visit_InvokeSpecial / InvokeVirtual / InvokeInterface / InvokeStatic / InvokeDynamic
visit_ClassInitialization
visit_ReferenceCondition / ReferenceTest / InstanceOf
visit_PHI
visit_MethodType / MethodHandle
visit_CaughtExceptionProjection
```

### Temporary variables

When a data-flow value node is used by more than one consumer node and is not a compile-
time constant, emitting it inline would duplicate computation. The base class handles
this automatically: on the second emit of such a node, `emitTemporaryVariable` is called
to assign the result to a fresh named variable, and subsequent uses reference that
variable name instead.

The cache is `committedToTemporary: Map<Node, T>`. Once a node is committed to a
temporary, every subsequent `emit` for that node just pushes the cached `T` (the
variable reference) without re-evaluating the expression.

Backends must implement:
```java
T emitTemporaryVariable(String prefix, T value);
```
This should declare a new variable in the current output scope, assign `value` to it,
and return a `T` that represents a reference to that variable.

### PHI node emission

PHI nodes are *data-flow* nodes that merge values at control-flow join points. In the
output they must become variable assignments — one per PHI — executed at the join point
before the join-point node's own statement.

The `Sequencer` calls `writePHINodesFor(node)` before emitting any node. The backend
receives all PHI nodes that are owned (via `DefinedByUse`) by `node` and must emit
them as variable assignments. This typically looks like:

```c
// at the MergeNode:
int phi_r = (came_from_true_branch) ? 1 : phi_r_incoming_false;
```

The PHI's `uses` list (filtered to `PHIUse` edges) provides the source values and the
originating control nodes, so the backend knows which branch each incoming value comes
from.

### The `writePreJump` hook

`writePreJump(node)` is called immediately before any jump instruction is about to be
generated: before `generateGOTO` for out-of-line edges, and explicitly in the `Goto`,
`LabelNode`, and `MergeNode` handlers.

This is a low-level hook that gives the backend a chance to close any open expression
context before the flow transfer. For example, a backend that accumulates expressions
into a buffer might need to flush that buffer before emitting a jump, because the jump
may land at a label that also needs to set PHI variables.

---

## 9. Implementing a Backend

To add a new code generation target, extend `StructuredControlflowCodeGenerator<T>`
where `T` is the type you use to represent expression results (e.g. `String` for
text-based generators).

The minimal required steps:

1. **Choose `T`**. For a textual target, `T = String` works. For a binary format you
   might use a wrapper around a byte offset or an instruction handle.

2. **Implement all `abstract` methods.** There are two groups:
   - The ~30 statement-level `write*` / `start*` / `finish*` methods — these emit
     code to your output stream when called by the Sequencer.
   - The ~30 `visit_*` methods — these recursively build an expression of type `T` by
     consuming operands from the `evaluationStack`.

3. **Handle temporaries.** Implement `emitTemporaryVariable(prefix, value)` to declare
   a variable and return a reference to it.

4. **Handle PHI nodes.** In `writePHINodesFor(node)`, iterate over all `DefinedByUse`
   users of the node and emit a variable assignment for each PHI found.

5. **Instantiate `Sequencer`.** Pass your backend instance:
   ```java
   new Sequencer(resolvedMethod, methodAnalyzer.ir(), myBackend);
   ```

The `DebugStructuredControlflowCodeGenerator` in the test sources is a complete,
working reference implementation that emits human-readable pseudo-code and is an
excellent starting point for understanding the callback sequence.

---

## 10. Worked Example — Loop with PHI

Consider this Java method:

```java
public int sum(int n) {
    int s = 0;
    for (int i = 0; i < n; i++) {
        s += i;
    }
    return s;
}
```

After parsing, the IR contains (simplified):

```
Method
 └─► LoopHeaderNode
       ├─ PHI[s : int]  ← PHIUse(0, Method)          // initial value
       │                ← PHIUse(s+i, If)              // loop-carried value
       ├─ PHI[i : int]  ← PHIUse(0, Method)
       │                ← PHIUse(i+1, If)
       └─► If(NumericCondition[LT, i, n])
             │ "true"  (loop body)
             │   Add[s, i]  →  stored back into PHI[s]
             │   Add[i, 1]  →  stored back into PHI[i]
             │   └─► Goto ──► LoopHeaderNode  (BACKWARD)
             │
             └─► "false" (loop exit)
                   MergeNode
                   └─► ReturnValue(PHI[s])
```

The Sequencer produces the following callback sequence:

```
begin(resolvedMethod, method)
writePHINodesFor(method)           // nothing owned by Method here

  visit(LoopHeaderNode):
    visitBranchingNodeTemplate:
      push LOOP block  (continueLeadsTo = LoopHeaderNode)
      push NORMAL block (breakLeadsTo = MergeNode)
      startBlock(LOOP)
      startBlock(NORMAL)

      writePHINodesFor(LoopHeaderNode)
        → emit:  int phi_s = 0;   int phi_i = 0;

      followUpProcessor(LoopHeaderNode) → If

      visit(If):
        writePHINodesFor(If)       // nothing
        startIfWithTrueBlock(If)

          // true arm: loop body
          visit(Goto):
            writePreJump(Goto)
            // Goto's target = LoopHeaderNode; idom(LoopHeaderNode) ≠ Goto
            generateGOTO → continueLeadsTo matches LOOP block
            writeContinueTo("LoopHeaderNode_N", Goto, LoopHeaderNode)
            current = null

        startIfElseBlock(If)

          // false arm → ExtractControlFlowProjection (transparent)
          followUpProcessor → MergeNode
          // idom(MergeNode) = If? No, idom(MergeNode) = LoopHeaderNode
          → generateGOTO → breakLeadsTo matches NORMAL block
          writeBreakTo("LoopHeaderNode_N_0", ..., MergeNode)

        finishIfBlock()

      finishBlock(NORMAL)          // close NORMAL block for MergeNode
      visitDominationTreeOf(MergeNode):
        writePreJump(MergeNode)
        writePHINodesFor(MergeNode)  // nothing owned by MergeNode
        write(MergeNode)
        followUpProcessor → ReturnValue
        write(ReturnValue)           // emit: return phi_s;

      finishBlock(LOOP)             // close LOOP block

finished()
```

The generated OpenCL C (approximately):

```c
int phi_s = 0;
int phi_i = 0;
LoopHeaderNode_N: for (;;) {
  LoopHeaderNode_N_0: {
    if (phi_i < n) {
      phi_s = phi_s + phi_i;
      phi_i = phi_i + 1;
      continue LoopHeaderNode_N;
    } else {
      break LoopHeaderNode_N_0;
    }
  }
  // MergeNode (loop exit)
  return phi_s;
}
```

---

## 11. Error Handling

The `Sequencer` wraps all processing in a try-catch that converts unexpected
`RuntimeException`s into `SequencerException` with the offending node information.
`IllegalStateException` (which signals a sequencing logic error, such as an unresolved
GOTO) is re-thrown directly without wrapping.

The most common error is **"GOTO not properly handled"** from `generateGOTO`. This
means the sequencer encountered a control-flow edge whose target could not be mapped to
any active `break` or `continue` label. This indicates either a bug in the IR
construction (an edge crosses a scope boundary that was not modelled as a block), or a
node type that is not yet covered by `visitBranchingNodeTemplate`.
