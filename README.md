# MetaIR

[![Java CI with Maven](https://github.com/mirkosertic/MetaIR/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/mirkosertic/MetaIR/actions/workflows/maven.yml)

MetaIR is a graph-based intermediate representation (IR) for JVM bytecode, built on Cliff Click's Sea-of-Nodes concept. 
The framework leverages the Java Class-File API introduced in Java 24 (JEP 484). 

Most parts of the IR are taken from [Bytecoder - Framework to interpret and transpile JVM bytecode to JavaScript, OpenCL or WebAssembly. ](https://github.com/mirkosertic/Bytecoder),
but were rewritten to be more flexible and extensible.

## Key Features

- **Analysis**: Transform and inspect existing JVM bytecode
- **Visualization**: Debug functionality for graph representation
- **Optimization**: Built-in peephole optimizations for graph reduction
- **Integration**: Built-in integration with JUnit Platform
- **Cross-Compilation**: Foundation framework for cross-compiler development
- **MetaIR** covers all aspects of JVM bytecode, including:
  - **Control Flow**: Branching, loops, exception handling
  - **Data Flow**: Local variables, stack, memory
  - **Memory aliasing**: Memory allocation, memory access

## Technical Details
- Built on [Java Class-File API (JEP 484)](https://openjdk.org/jeps/484)
- Implements Sea-of-Nodes IR design
- Further examples and documentation: [SeaOfNodes/Simple](https://github.com/SeaOfNodes/Simple)

> * 🚧 Scheduling logic for machine code transformation is under development.
> * 🚧 Exception handling is under development.

## Credits
- An excellent tutorial about the Class-File API: [Build A Compiler With The Java Class-File API by Dr. James Hamilton](https://jameshamilton.eu/programming/build-compiler-java-class-file-api)
- IR Sequencing: [Beyond Relooper: recursive translation of unstructured control flow to structured control flow (functional pearl)](https://dl.acm.org/doi/10.1145/3547621)
- Compilers - Nuts and bolts of Programming Languages: [Compilers - Nuts and bolts of Programming Languages](https://pgrandinetti.github.io/compilers/)

## Example new instance creation

A simple Java example with constructor invoke, demonstrating the use of
the JUnit Platform integration:

```
import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class NewInstanceTest {

    public void newInstance() {
        new NewInstanceTest();
    }
}
```

Class-File API Debug YAML:
```

- method name: newInstance
  flags: [PUBLIC]
  method type: ()V
  attributes: [Code]
  code:
  max stack: 2
  max locals: 1
  attributes: [LineNumberTable, LocalVariableTable]
  line numbers:
  - {start: 0, line number: 12}
  - {start: 8, line number: 13}
  local variables:
  - {start: 0, end: 9, slot: 0, name: this, type: Lde/mirkosertic/metair/ir/examples/NewInstanceTest;}
  //stack map frame @0: {locals: [de/mirkosertic/metair/ir/examples/NewInstanceTest], stack: []}
  0: {opcode: NEW, type: de/mirkosertic/metair/ir/examples/NewInstanceTest}
  3: {opcode: DUP}
  4: {opcode: INVOKESPECIAL, owner: de/mirkosertic/metair/ir/examples/NewInstanceTest, method name: <init>, method type: ()V}
  7: {opcode: POP}
  8: {opcode: RETURN}
```

Generated IR (raw and unoptimized):

![IRExample New Instance Creation](https://mirkosertic.github.io/MetaIR/de.mirkosertic.metair.ir.examples.NewInstanceTest/newInstance/ir.dot.svg)

A full set of examples can be found in the [MetaIR Test Suite](https://mirkosertic.github.io/MetaIR/). Each directory corresponds
to a single testcase, and you will find the Class-File API Debug YAML and the
generated IR (raw and unoptimized) as well in dot(graphviz) notation and as SVG images.

## JVM instructions not implemented

> 🚧 These instructions are discontinued and will be removed in the future, so MetaIR will not support them for now.

    /**
     * (Discontinued) Jump subroutine; last used in major version {@value
     * ClassFile#JAVA_6_VERSION}.
     *
     * @jvms 4.9.1 Static Constraints
     * @jvms 6.5.jsr <em>jsr</em>
     * @see Kind#DISCONTINUED_JSR
     */
    JSR(RawBytecodeHelper.JSR, 3, Kind.DISCONTINUED_JSR),

    /**
     * (Discontinued) Return from subroutine; last used in major version
     * {@value ClassFile#JAVA_6_VERSION}.
     *
     * @jvms 4.9.1 Static Constraints
     * @jvms 6.5.ret <em>ret</em>
     * @see Kind#DISCONTINUED_RET
     */
    RET(RawBytecodeHelper.RET, 2, Kind.DISCONTINUED_RET),

    /**
     * (Discontinued) Jump subroutine (wide index); last used in major
     * version {@value ClassFile#JAVA_6_VERSION}.
     *
     * @jvms 4.9.1 Static Constraints
     * @jvms 6.5.jsr_w <em>jsr_w</em>
     * @see Kind#DISCONTINUED_JSR
     */
    JSR_W(RawBytecodeHelper.JSR_W, 5, Kind.DISCONTINUED_JSR),

    /**
     * (Discontinued) Return from subroutine (wide index); last used in major
     * version {@value ClassFile#JAVA_6_VERSION}.
     * This is a {@linkplain #isWide() wide}-modified pseudo-opcode.
     *
     * @jvms 4.9.1 Static Constraints
     * @jvms 6.5.wide <em>wide</em>
     * @jvms 6.5.ret <em>ret</em>
     * @see Kind#DISCONTINUED_RET
     */
    RET_W((RawBytecodeHelper.WIDE << 8) | RawBytecodeHelper.RET, 4, Kind.DISCONTINUED_RET),
