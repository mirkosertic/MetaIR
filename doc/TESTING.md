# MetaIR — Testing Infrastructure

This document explains how to write tests using the MetaIR JUnit Platform integration,
what happens under the hood when a test runs, and how to inspect the artifacts the
engine produces. It is a companion to [IR.md](IR.md) and [API.md](API.md).

---

## Table of Contents

1. [Overview](#1-overview)
2. [The `@MetaIRTest` Annotation](#2-the-metairtest-annotation)
3. [How the Test Engine Works](#3-how-the-test-engine-works)
   - [Discovery](#discovery)
   - [Execution](#execution)
4. [Output Artifacts](#4-output-artifacts)
5. [Writing Your First Test](#5-writing-your-first-test)
6. [Including Constructors](#6-including-constructors)
7. [Using `MetaIRTestHelper` Directly](#7-using-metairtesthelper-directly)
8. [Using `MetaIRTestTools` with JUnit 5](#8-using-metairtesttools-with-junit-5)
9. [Asserting Against the Graph](#9-asserting-against-the-graph)
10. [The Online Test Suite](#10-the-online-test-suite)

---

## 1. Overview

MetaIR ships a custom **JUnit Platform** test engine (engine ID `metair-test`). Any
class annotated with `@MetaIRTest` is treated as a test container: each of its declared
methods becomes an individual test case. When a test runs, MetaIR:

1. Loads the class's bytecode via the Java Class-File API.
2. Runs the full six-step parsing pipeline (see [IR.md](IR.md)) for each method.
3. Writes a set of diagnostic artifacts (DOT graphs, YAML bytecode dump, sequenced
   pseudo-code) to a per-test output directory.
4. Reports the test as passed if parsing succeeded, or failed with the
   `IllegalParsingStateException` if it did not.

This approach means you do not need to write any assertion code to verify that parsing
works — the act of parsing is itself the test.

---

## 2. The `@MetaIRTest` Annotation

```java
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Testable
public @interface MetaIRTest {
    boolean includeConstructors() default false;
}
```

Place `@MetaIRTest` on any top-level class whose methods you want to parse and inspect:

```java
import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class MyTest {

    public int add(int a, int b) {
        return a + b;
    }

    public String greet(String name) {
        return "Hello, " + name;
    }
}
```

Both `add` and `greet` become individual test cases. The class itself does not need to
extend anything or implement any interface.

**Attribute:**

| Attribute | Default | Effect |
|---|---|---|
| `includeConstructors` | `false` | When `true`, constructors (`<init>`) are parsed in addition to regular methods |

---

## 3. How the Test Engine Works

### Discovery

`MetaIRTestEngine` implements `org.junit.platform.engine.TestEngine` and registers
itself via the standard Java `ServiceLoader` mechanism
(`META-INF/services/org.junit.platform.engine.TestEngine`).

When Maven Surefire or any other JUnit Platform launcher runs, the engine is
automatically discovered. It responds to three selector types:

| Selector | What is discovered |
|---|---|
| `ClasspathRootSelector` | All classes in a classpath root that carry `@MetaIRTest` |
| `PackageSelector` | All `@MetaIRTest` classes in a package |
| `ClassSelector` | A single named class, if it carries `@MetaIRTest` |
| `MethodSelector` | A single named method on a `@MetaIRTest` class |

Each discovered class becomes a `ClassTestDescriptor` (a container). Each declared
method on that class becomes a `MethodTestDescriptor` (a leaf test). If
`includeConstructors = true`, constructors are added as additional leaf tests.

### Execution

`MetaIRTestExecutor.executeMethod` drives the actual test run for a single method:

```
1. Create a fresh ResolverContext
2. resolveClass(origin.getName())          → loads the .class file via ClassLoader
3. find the MethodModel matching the test method name
4. wrap it in a ResolvedMethod
5. create an output directory for this test
6. MetaIRTestHelper.analyzeAndReport(resolvedMethod)
   ├─ write bytecode.yaml
   ├─ write report.html
   ├─ MethodAnalyzer.analyze()             → six-step pipeline
   ├─ write ir.dot
   ├─ write ir_dominatortree.dot
   ├─ write bytecodecfg.dot
   ├─ write ir_cfg_dominatortree.dot
   └─ write sequenced.txt
7. Report SUCCESSFUL or FAILED
```

Each test gets its own `ResolverContext`, so class resolution is fully isolated between
tests. The number of transitively resolved classes is logged at INFO level.

---

## 4. Output Artifacts

After a test run, each method test writes the following files into its output directory
(under `target/surefire-reports/<ClassName>/<methodName>/` or the JUnit Platform output
directory):

| File | Contents |
|---|---|
| `bytecode.yaml` | The Class-File API debug representation of the method's bytecode — opcodes, local variable table, stack map frames |
| `report.html` | An HTML page embedding the source file (fetched from GitHub) alongside the generated graphs |
| `ir.dot` | The full IR graph in Graphviz DOT notation |
| `ir_dominatortree.dot` | The full IR dominator tree (all edge types) in DOT notation |
| `bytecodecfg.dot` | The bytecode-level CFG (the `Frame` graph from step 2) in DOT notation |
| `ir_cfg_dominatortree.dot` | The IR-level CFG dominator tree (control edges only) in DOT notation |
| `sequenced.txt` | The output of `DebugStructuredControlflowCodeGenerator` — human-readable pseudo-code after sequencing |

See [VISUALIZATION.md](VISUALIZATION.md) for how to render the DOT files and what the
visual conventions mean.

The full public test suite output is browsable at the
[MetaIR Test Suite](https://mirkosertic.github.io/MetaIR/) website.

---

## 5. Writing Your First Test

Add your class to `src/test/java`, annotate it, and run `mvn test`:

```java
package com.example;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class ArithmeticTest {

    public int multiply(int x, int y) {
        return x * y;
    }

    public double hypotenuse(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }
}
```

Maven will pick up the class automatically because `MetaIRTestEngine` scans the entire
test classpath. No additional configuration is needed.

---

## 6. Including Constructors

By default, constructors are skipped. Set `includeConstructors = true` to parse them:

```java
@MetaIRTest(includeConstructors = true)
public class MyClass {

    private final int value;

    public MyClass(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

Both the constructor and `getValue` will be parsed and reported.

---

## 7. Using `MetaIRTestHelper` Directly

`MetaIRTestHelper` can be used outside the JUnit engine when you want programmatic
control over where output is written or when you are building tooling on top of MetaIR:

```java
import de.mirkosertic.metair.ir.*;
import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import java.nio.file.Path;
import java.lang.classfile.*;

// 1. Set up the resolver
ResolverContext ctx = new ResolverContext();

// 2. Resolve the class
ResolvedClass resolvedClass = ctx.resolveClass("com.example.MyClass");

// 3. Find the method
ClassModel classModel = resolvedClass.classModel();
for (MethodModel method : classModel.methods()) {
    if ("myMethod".equals(method.methodName().stringValue())) {

        // 4. Wrap and analyse
        ResolvedMethod rm = resolvedClass.resolveMethod(method);
        Path outputDir = Path.of("build/metair-output");
        MethodAnalyzer analyzer = new MetaIRTestHelper(outputDir, ctx)
                .analyzeAndReport(rm);

        // 5. Work with the graph
        Method irMethod = analyzer.ir();
        // ... inspect nodes, run Sequencer, etc.
    }
}
```

`analyzeAndReport` returns the `MethodAnalyzer` so you can access `analyzer.ir()` for
the completed graph.

For a quick expression-to-string conversion (useful in assertions), use the static
helper:

```java
String expr = MetaIRTestHelper.toDebugExpression(someNode);
// e.g. "(arg0 + 1)"
```

---

## 8. Using `MetaIRTestTools` with JUnit 5

`MetaIRTestTools` is a JUnit 5 `ParameterResolver` extension. When used with
`@ExtendWith`, it injects a pre-configured `MetaIRTestHelper` into a JUnit 5 test
method. This is useful when you want to combine MetaIR analysis with regular JUnit 5
assertions in the same test class:

```java
import de.mirkosertic.metair.ir.test.MetaIRTestHelper;
import de.mirkosertic.metair.ir.test.MetaIRTestTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MetaIRTestTools.class)
class MyJUnit5Test {

    @Test
    void analyzeAdd(MetaIRTestHelper helper) throws Exception {
        var analyzer = helper.analyzeAndReport(
                helper.resolverContext().resolveClass("com.example.Calc")
                      .classModel()
                      .methods()
                      .stream()
                      .filter(m -> "add".equals(m.methodName().stringValue()))
                      .map(m -> helper.resolverContext()
                                      .resolveClass("com.example.Calc")
                                      .resolveMethod(m))
                      .findFirst()
                      .orElseThrow());

        // regular JUnit 5 assertion against the graph
        assertNotNull(analyzer.ir());
    }
}
```

> **Note:** `MetaIRTestTools` currently has a known limitation — the
> `OutputDirectoryProvider` is not always available when running under Maven Surefire.
> The output directory falls back to `target/metair-test/<className>/<methodName>`,
> controlled by the system property `test.output.dir`.

---

## 9. Asserting Against the Graph

The test engine itself only asserts that parsing does not throw. For richer assertions,
access `analyzer.ir()` after `analyzeAndReport` and traverse the graph directly:

```java
import de.mirkosertic.metair.ir.*;

MethodAnalyzer analyzer = helper.analyzeAndReport(resolvedMethod);
Method root = analyzer.ir();

// Check that the method has exactly one ReturnValue node
long returnCount = new DFS2(root).getTopologicalOrder()
        .stream()
        .filter(n -> n instanceof ReturnValue)
        .count();
assertEquals(1, returnCount);

// Check that a specific node's debug expression matches
Node phi = new DFS2(root).getTopologicalOrder()
        .stream()
        .filter(n -> n instanceof PHI)
        .findFirst()
        .orElseThrow();
assertEquals("Φ int", phi.debugDescription());
```

For expression-level assertions, use `MetaIRTestHelper.toDebugExpression(node)` which
runs the `DebugStructuredControlflowCodeGenerator` on a single node and returns the
result as a string.

---

## 10. The Online Test Suite

Every method in the `src/test/java/.../examples/` package is part of the public test
suite, which is built and published on each push to `main`. The output is available at:

**https://mirkosertic.github.io/MetaIR/**

Each directory corresponds to a single test class, and within it each method has its
own subdirectory containing all the artifacts listed in [Section 4](#4-output-artifacts).
This is the fastest way to understand what the IR looks like for a given Java construct
without running anything locally.
