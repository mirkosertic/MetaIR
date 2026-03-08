# MetaIR — API Reference

This document is the programmatic API guide for MetaIR. It covers how to load
classes, resolve methods and fields, run the analysis pipeline, and traverse the
resulting IR graph without going through the JUnit test engine.

Companion documents:

- [IR Architecture](IR.md) — node types, edge semantics, the full parsing pipeline
- [Sequencer & Code Generation](SEQUEMCER.md) — linearising the graph into output code
- [Testing Infrastructure](TESTING.md) — the `@MetaIRTest` JUnit integration
- [Visualization](VISUALIZATION.md) — rendering the DOT graphs produced by the API

---

## Table of Contents

1. [Overview](#1-overview)
2. [IRType — the Type System](#2-irtype--the-type-system)
3. [ResolverContext](#3-resolvercontext)
   - [Construction](#construction)
   - [Resolving Classes](#resolving-classes)
   - [Resolving Types and Method Types](#resolving-types-and-method-types)
   - [Resolving Fields](#resolving-fields)
   - [Method Resolution (current state)](#method-resolution-current-state)
4. [ResolvedClass](#4-resolvedclass)
5. [ResolvedMethod](#5-resolvedmethod)
6. [ResolvedField](#6-resolvedfield)
7. [Running the Analysis Pipeline](#7-running-the-analysis-pipeline)
8. [Traversing the IR Graph](#8-traversing-the-ir-graph)
9. [Common Patterns](#9-common-patterns)
10. [Error Handling](#10-error-handling)

---

## 1. Overview

The MetaIR API has three layers:

```
┌───────────────────────────────────────────────────────────────┐
│  ResolverContext                                               │
│  (class loading, caching, type resolution)                     │
├───────────────────────────────────────────────────────────────┤
│  ResolvedClass / ResolvedMethod / ResolvedField               │
│  (thin wrappers around the Java Class-File API models)         │
├───────────────────────────────────────────────────────────────┤
│  MethodAnalyzer → Method (IR graph)                           │
│  (the six-step parsing and construction pipeline)              │
└───────────────────────────────────────────────────────────────┘
```

Everything flows through a single `ResolverContext`. Create one per
compilation unit (one method, one class, or one whole-program analysis), then
ask it to resolve classes. Resolved classes expose their methods and fields.
Call `ResolvedMethod.analyze()` to run the pipeline and get back the IR.

---

## 2. IRType — the Type System

`IRType<T>` is the MetaIR representation of JVM types, wrapping the
corresponding `java.lang.constant` descriptor.

### Subclasses

| Subclass | Wraps | Purpose |
|---|---|---|
| `IRType.MetaClass` | `ClassDesc` | Any class, interface, primitive, or array type |
| `IRType.MethodType` | `MethodTypeDesc` | A method signature (return + parameters) |
| `IRType.MethodHandle` | `MethodHandleDesc` | A method handle constant |

### Pre-defined MetaClass constants

`IRType` exposes static constants for all primitive types and a few common
reference types:

```java
IRType.CD_int      // int
IRType.CD_long     // long
IRType.CD_float    // float
IRType.CD_double   // double
IRType.CD_byte     // byte
IRType.CD_char     // char
IRType.CD_short    // short
IRType.CD_boolean  // boolean
IRType.CD_void     // void
IRType.CD_String   // java.lang.String
IRType.CD_Object   // java.lang.Object
```

### MetaClass operations

```java
IRType.MetaClass t = IRType.MetaClass.of(ClassDesc.of("com.example.Foo"));

t.isPrimitive()          // false — reference type
t.isArray()              // false
t.arrayType()            // IRType.MetaClass for Foo[]
t.type()                 // the underlying ClassDesc
```

### MethodType operations

```java
IRType.MethodType mt = ctx.resolveMethodType(someMethodTypeDesc);

mt.returnType()          // IRType.MetaClass
mt.parameterCount()      // number of formal parameters
mt.parameterType(0)      // IRType.MetaClass for parameter 0
mt.type()                // the underlying MethodTypeDesc
```

---

## 3. ResolverContext

`ResolverContext` is the entry point for all class loading. It maintains a
cache of every class it has loaded — loading each `.class` file at most once
per context instance.

### Construction

```java
// Uses Thread.currentThread().getContextClassLoader() — fine for most uses
ResolverContext ctx = new ResolverContext();

// Pass a specific ClassLoader when running in a container or OSGi environment
ResolverContext ctx = new ResolverContext(myClassLoader);
```

### Resolving Classes

There are four overloads:

```java
// By binary name — most common
ResolvedClass rc = ctx.resolveClass("com.example.MyClass");

// By ClassDesc (from the Class-File API constant pool)
ResolvedClass rc = ctx.resolveClass(someClassDesc);

// From an already-parsed ClassModel (no I/O needed)
ResolvedClass rc = ctx.resolveClass(someClassModel);
```

`resolveClass(String)` performs the following steps:

1. Check the internal cache; return immediately if already resolved.
2. Locate the `.class` file via `ClassLoader.getResource()`, trying both
   OS-specific and UNIX-style separators.
3. Parse the bytes with `ClassFile.of().parse(data)` (Java Class-File API).
4. Recursively resolve the superclass and all implemented interfaces.
5. Store the `ResolvedClass` in the cache and return it.

Array types are mapped to `java.lang.reflect.Array` automatically.

```java
// Diagnostic: how many classes have been loaded transitively
int count = ctx.numberOrResolvedClasses();
```

### Resolving Types and Method Types

```java
// Resolve a type descriptor to an IRType.MetaClass
IRType.MetaClass t = ctx.resolveType(ClassDesc.of("com.example.Foo"));

// Resolve a method descriptor — also resolves all parameter and return types
IRType.MethodType mt = ctx.resolveMethodType(
        MethodTypeDesc.of(ClassDesc.of("int"), ClassDesc.of("com.example.Foo")));
```

Both methods trigger class loading for any non-primitive types they encounter,
so the resolver's cache grows as a side effect.

### Resolving Fields

```java
// Instance field
ResolvedField f = ctx.resolveMemberField(ownerClassDesc, "fieldName", fieldTypeDesc);

// Static field
ResolvedField f = ctx.resolveStaticField(ownerClassDesc, "fieldName", fieldTypeDesc);
```

Both methods delegate to the `ResolvedClass` for the owner type. Field
resolution walks the class hierarchy when the field is not found on the
declaring class (superclass chain traversal).

### Method Resolution (current state)

Four methods exist for resolving invoke targets:

```java
ctx.resolveInvokeSpecial(owner, name, typeDesc)
ctx.resolveInvokeStatic(owner, name, typeDesc)
ctx.resolveInvokeInterface(owner, name, typeDesc)
ctx.resolveInvokeVirtual(owner, name, typeDesc)
```

> **Note:** These methods currently return `null` — the implementations are
> stubs with TODO comments. Method resolution (interprocedural analysis) is a
> planned future feature. When it lands, callers will receive a `ResolvedMethod`
> that can be analyzed inline.

---

## 4. ResolvedClass

`ResolvedClass` is a thin, lazy wrapper around a `ClassModel` (the Java
Class-File API's representation of a parsed `.class` file).

### Key methods

```java
ResolvedClass rc = ctx.resolveClass("com.example.MyClass");

// The underlying Class-File API model
ClassModel model = rc.classModel();

// The MetaIR type for this class
IRType.MetaClass t = rc.thisType();

// All resolved fields (cached on first access)
List<ResolvedField> fields = rc.resolvedFields();

// Check if a specific interface is implemented
boolean impl = rc.hasInterface(ClassDesc.of("java.lang.Runnable"));

// Wrap a MethodModel in a ResolvedMethod (does not start analysis)
ResolvedMethod rm = rc.resolveMethod(someMethodModel);

// Find and wrap by name and descriptor
ResolvedMethod rm = rc.resolveMethodForSpecialInvocation("myMethod", typeDesc);
ResolvedMethod rm = rc.resolveMethodForStaticInvocation("staticHelper", typeDesc);
```

### Iterating methods

`ClassModel.methods()` (the Class-File API list) is the canonical source:

```java
for (MethodModel mm : rc.classModel().methods()) {
    String name = mm.methodName().stringValue();
    // Skip constructors, synthetics, native methods, etc. as needed
    ResolvedMethod rm = rc.resolveMethod(mm);
    // ...
}
```

`ResolvedClass` automatically resolves and analyzes `<clinit>` (static
initializers) during loading, so static field initializers are always
available in the IR.

---

## 5. ResolvedMethod

`ResolvedMethod` pairs a `MethodModel` with its owning `ResolvedClass` and the
`ResolverContext`, giving it everything it needs to drive the analysis.

```java
ResolvedMethod rm = rc.resolveMethod(someMethodModel);

// The raw Class-File API model
MethodModel mm = rm.methodModel();

// The class that owns this method
ResolvedClass owner = rm.thisClass();

// True if this method is a constructor (<init>)
boolean isCtor = rm.isConstructor();

// Run the six-step analysis pipeline (idempotent — cached after first call)
MethodAnalyzer analyzer = rm.analyze();
```

`analyze()` is idempotent: calling it multiple times returns the same
`MethodAnalyzer` instance. The analysis runs synchronously on the first call
and is thereafter a cache lookup.

---

## 6. ResolvedField

`ResolvedField` wraps a `FieldModel` with its owner class and resolved type.

```java
ResolvedField rf = ctx.resolveMemberField(ownerDesc, "value", fieldTypeDesc);

rf.fieldName()   // "value"
rf.type()        // IRType.MetaClass — the resolved field type
rf.owner()       // ResolvedClass that declares this field
rf.fieldModel()  // FieldModel — raw Class-File API model
```

---

## 7. Running the Analysis Pipeline

The full pipeline runs inside `MethodAnalyzer`. You can drive it either
through `ResolvedMethod.analyze()` or through `MetaIRTestHelper` (which also
writes diagnostic artifacts).

### Direct approach

```java
ResolverContext ctx = new ResolverContext();
ResolvedClass rc = ctx.resolveClass("com.example.MyClass");

for (MethodModel mm : rc.classModel().methods()) {
    if ("compute".equals(mm.methodName().stringValue())) {
        ResolvedMethod rm = rc.resolveMethod(mm);
        MethodAnalyzer analyzer = rm.analyze();

        // The root Method node of the completed IR graph
        Method irMethod = analyzer.ir();
        break;
    }
}
```

### With artifact output (MetaIRTestHelper)

```java
import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import java.nio.file.Path;

ResolverContext ctx = new ResolverContext();
ResolvedClass rc = ctx.resolveClass("com.example.MyClass");
ResolvedMethod rm = rc.resolveMethod(/* ... */);

Path outputDir = Path.of("build/metair-output");
MethodAnalyzer analyzer = new MetaIRTestHelper(outputDir, ctx)
        .analyzeAndReport(rm);

// All six artifact files are now written to outputDir:
//   bytecode.yaml, report.html, ir.dot, ir_dominatortree.dot,
//   bytecodecfg.dot, ir_cfg_dominatortree.dot, sequenced.txt

Method ir = analyzer.ir();
```

See [TESTING.md § 7](TESTING.md#7-using-metairtesthelper-directly) for the
full `MetaIRTestHelper` reference and [VISUALIZATION.md](VISUALIZATION.md) for
how to render the DOT output.

---

## 8. Traversing the IR Graph

After analysis, `analyzer.ir()` returns the `Method` root node. Traverse the
graph with `DFS2`:

```java
Method root = analyzer.ir();
List<Node> order = new DFS2(root).getTopologicalOrder();

for (Node n : order) {
    System.out.println(n.debugDescription());
}
```

### Filtering by node type

```java
// All PHI nodes
List<PHI> phis = order.stream()
        .filter(n -> n instanceof PHI)
        .map(n -> (PHI) n)
        .toList();

// All return points
long returns = order.stream()
        .filter(n -> n instanceof ReturnValue || n instanceof Return)
        .count();
```

### Navigating edges

Every `Node` has two edge lists:

```java
// Outgoing (what this node uses / depends on)
for (Node.UseEdge e : node.uses) {
    Node dependency = e.node();
    Use edgeType = e.use();    // DataFlowUse, ControlFlowUse, MemoryUse, etc.
}

// Incoming (who depends on this node)
for (Node consumer : node.usedBy) {
    // ...
}
```

Edge type reference:

| `Use` subtype | Meaning |
|---|---|
| `DataFlowUse` | Value dependency (black arrow in DOT) |
| `ControlFlowUse` | Control-flow successor (red arrow) |
| `MemoryUse` | Memory-chain dependency (green arrow) |
| `PHIUse` | PHI incoming value (blue arrow) |
| `DefinedByUse` | Variable definition scope (dotted arrow) |

### Debug expressions

For a quick human-readable string representation of any node:

```java
String expr = MetaIRTestHelper.toDebugExpression(someNode);
// e.g., "(arg0 + 1)", "Φ int", "return (arg0 * 2)"
```

---

## 9. Common Patterns

### Check how many times a value is used

```java
int consumers = node.usedBy.size();
```

### Find the immediate dominator of a node in the CFG

```java
CFGDominatorTree cfgDom = new CFGDominatorTree(root);
Node idom = cfgDom.idom.get(node);
```

### Run the Sequencer to produce pseudo-code

```java
MyCodeGenerator backend = new MyCodeGenerator();    // implements StructuredControlflowCodeGenerator
new Sequencer(analyzer.ir(), backend).sequence();
```

See [SEQUEMCER.md](SEQUEMCER.md) for the full Sequencer API and how to
implement a backend.

### Export graphs to DOT

```java
DOTExporter.writeTo(analyzer.ir(), System.out);                        // full IR
DOTExporter.writeTo(new DominatorTree(analyzer.ir()), System.out);     // full dominator tree
DOTExporter.writeTo(new CFGDominatorTree(analyzer.ir()), System.out);  // CFG dominator tree
DOTExporter.writeBytecodeCFGTo(analyzer, System.out);                  // bytecode CFG
```

See [VISUALIZATION.md](VISUALIZATION.md) for rendering instructions and a
guide to the visual conventions.

---

## 10. Error Handling

### Class not found

```java
// Throws IllegalArgumentException if the .class resource cannot be located
ResolvedClass rc = ctx.resolveClass("com.example.Missing");
```

The message includes the resource path that was searched, which helps diagnose
classpath issues.

### Analysis failure

```java
// Throws IllegalParsingStateException if the pipeline encounters
// a bytecode pattern it cannot represent
MethodAnalyzer analyzer = rm.analyze();
```

`IllegalParsingStateException` extends `RuntimeException` and carries a
reference to the `MethodAnalyzer` instance at the point of failure. The
trimmed stack trace points directly to the internal method that detected the
inconsistency, making it easier to diagnose which step failed.

### Field not found

```java
// Throws IllegalArgumentException if the field is not found
// in the class or any superclass
ResolvedField f = ctx.resolveMemberField(ownerDesc, "missing", typeDesc);
```

---

*See also: [IR.md](IR.md) · [SEQUEMCER.md](SEQUEMCER.md) · [TESTING.md](TESTING.md) · [VISUALIZATION.md](VISUALIZATION.md) · [OPTIMIZATION.md](OPTIMIZATION.md)*
