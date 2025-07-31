# MetaIR

MetaIR is a graph-based intermediate representation (IR) for JVM bytecode, built on Cliff Click's Sea-of-Nodes concept. 
The framework leverages the Java Class-File API introduced in Java 24 (JEP 484).

## Key Features

- **Analysis**: Transform and inspect existing JVM bytecode
- **Visualization**: Debug functionality for graph representation
- **Optimization**: Built-in peephole optimizations for graph reduction
- **Cross-Compilation**: Foundation framework for cross-compiler development

## Technical Details
- Built on [Java Class-File API (JEP 484)](https://openjdk.org/jeps/484)
- Implements Sea-of-Nodes IR design
- Further examples and documentation: [SeaOfNodes/Simple](https://github.com/SeaOfNodes/Simple)

> 🚧 Scheduling logic for machine code transformation is under development.

## Example new instance creation

A simple Java example with constructor invocation:
```
public class Test {

    public static Test newInstance() {
        return new Test();
    }
}
```

Class-File API Debug YAML:
```
  - method name: newInstance
    flags: [PUBLIC, STATIC]
    method type: ()Lde/mirkosertic/metair/Test;
    attributes: [Code]
    code: 
        max stack: 2
        max locals: 0
        attributes: [LineNumberTable]
        line numbers: 
          - {start: 0, line number: 32}
        //stack map frame @0: {locals: [], stack: []}
        0: {opcode: NEW, type: de/mirkosertic/metair/Test}
        3: {opcode: DUP}
        4: {opcode: INVOKESPECIAL, owner: de/mirkosertic/metair/Test, method name: <init>, method type: ()V}
        7: {opcode: ARETURN}
```

Generated IR (raw and unoptimized):

![IRExample New Instance Creation](./docs/newinstance_1.png)

## Example constructor invocation

A simple Java example with constructor invocation:
```
public class Test {

    public Test() {
    }
}
```

Class-File API Debug YAML:
```
  - method name: <init>
    flags: [PUBLIC]
    method type: ()V
    attributes: [Code]
    code: 
        max stack: 1
        max locals: 1
        attributes: [LineNumberTable, LocalVariableTable]
        line numbers: 
          - {start: 0, line number: 5}
          - {start: 4, line number: 6}
        local variables: 
          - {start: 0, end: 5, slot: 0, name: this, type: Lde/mirkosertic/metair/Test;}
        //stack map frame @0: {locals: [THIS], stack: []}
        0: {opcode: ALOAD_0, slot: 0, type: Lde/mirkosertic/metair/Test;, variable name: this}
        1: {opcode: INVOKESPECIAL, owner: java/lang/Object, method name: <init>, method type: ()V}
        4: {opcode: RETURN}
```

Generated IR (raw and unoptimized):

![IRExample Constructor Invocation](./docs/constructor_1.png)

## Example if / conditional logic

A simple Java example with an if-statement returning different values based on a condition:
```
public class Test {

    public static int simpleIf(int a, int b, int c, int d) {
        if (a > b) {
            return c;
        } else {
            return d;
        }
    }
}
```

Class-File API Debug YAML:
```
  - method name: simpleIf
    flags: [PUBLIC, STATIC]
    method type: (IIII)I
    attributes: [Code]
    code: 
        max stack: 2
        max locals: 4
        attributes: [LineNumberTable, LocalVariableTable, StackMapTable]
        line numbers: 
          - {start: 0, line number: 9}
          - {start: 5, line number: 10}
          - {start: 7, line number: 12}
        local variables: 
          - {start: 0, end: 9, slot: 0, name: a, type: I}
          - {start: 0, end: 9, slot: 1, name: b, type: I}
          - {start: 0, end: 9, slot: 2, name: c, type: I}
          - {start: 0, end: 9, slot: 3, name: d, type: I}
        stack map frames: 
            7: {locals: [int, int, int, int], stack: []}
        //stack map frame @0: {locals: [int, int, int, int], stack: []}
        0: {opcode: ILOAD_0, slot: 0, type: I, variable name: a}
        1: {opcode: ILOAD_1, slot: 1, type: I, variable name: b}
        2: {opcode: IF_ICMPLE, target: 7}
        5: {opcode: ILOAD_2, slot: 2, type: I, variable name: c}
        6: {opcode: IRETURN}
        //stack map frame @7: {locals: [int, int, int, int], stack: []}
        7: {opcode: ILOAD_3, slot: 3, type: I, variable name: d}
        8: {opcode: IRETURN}
```

Generated IR (raw and unoptimized):

![IRExample Conditional](./docs/if_1.png)

## Example loop

A simple Java example with a loop:
```
public class Test {

    public static int forLoop(int a) {
        for (int i = 0; i < a; i++) {
            compute(i);
        }
        return a;
    }

    private static int compute(int a) {
        return a + 1;
    }
}
```

Class-File API Debug YAML:
```
  - method name: forLoop
    flags: [PUBLIC, STATIC]
    method type: (I)I
    attributes: [Code]
    code: 
        max stack: 2
        max locals: 2
        attributes: [LineNumberTable, LocalVariableTable, StackMapTable]
        line numbers: 
          - {start: 0, line number: 17}
          - {start: 7, line number: 18}
          - {start: 12, line number: 17}
          - {start: 18, line number: 20}
        local variables: 
          - {start: 2, end: 18, slot: 1, name: i, type: I}
          - {start: 0, end: 20, slot: 0, name: a, type: I}
        stack map frames: 
            2: {locals: [int, int], stack: []}
            18: {locals: [int], stack: []}
        //stack map frame @0: {locals: [int], stack: []}
        0: {opcode: ICONST_0, constant value: 0}
        1: {opcode: ISTORE_1, slot: 1}
        //stack map frame @2: {locals: [int, int], stack: []}
        2: {opcode: ILOAD_1, slot: 1, type: I, variable name: i}
        3: {opcode: ILOAD_0, slot: 0, type: I, variable name: a}
        4: {opcode: IF_ICMPGE, target: 18}
        7: {opcode: ILOAD_1, slot: 1, type: I, variable name: i}
        8: {opcode: INVOKESTATIC, owner: de/mirkosertic/metair/Test, method name: compute, method type: (I)I}
        11: {opcode: POP}
        12: {opcode: IINC, slot: 1, const: 1, type: I, variable name: i}
        15: {opcode: GOTO, target: 2}
        //stack map frame @18: {locals: [int], stack: []}
        18: {opcode: ILOAD_0, slot: 0, type: I, variable name: a}
        19: {opcode: IRETURN}
```

Generated IR (raw and unoptimized):

![IRExample Loop](./docs/loop_1.png)

## Static method invocation

A simple static method invocation:
```
public class Test {

    private static int compute(int a) {
        return a + 1;
    }

    public int staticInvocationInMember() {
        return compute(10);
    }
}
```

Class-File API Debug YAML:
```
  - method name: staticInvocationInMember
    flags: [PUBLIC]
    method type: ()I
    attributes: [Code]
    code: 
        max stack: 1
        max locals: 1
        attributes: [LineNumberTable, LocalVariableTable]
        line numbers: 
          - {start: 0, line number: 28}
        local variables: 
          - {start: 0, end: 6, slot: 0, name: this, type: Lde/mirkosertic/metair/Test;}
        //stack map frame @0: {locals: [de/mirkosertic/metair/Test], stack: []}
        0: {opcode: BIPUSH, constant value: 10}
        2: {opcode: INVOKESTATIC, owner: de/mirkosertic/metair/Test, method name: compute, method type: (I)I}
        5: {opcode: IRETURN}
```

Generated IR (raw and unoptimized):

![IRExample StaticMethodInvocation](docs/staticinvoke_1.png)

## JVM instructions not yet implemented

> 🚧 Under development.

    /**
     * Compare {@link TypeKind#LONG long}.
     *
     * @see Long#numericCondition(long, long)
     * @jvms 6.5.lcmp <em>lcmp</em>
     * @see Kind#OPERATOR
     */
    LCMP(RawBytecodeHelper.LCMP, 1, Kind.OPERATOR),

    /**
     * Compare {@link TypeKind#FLOAT float}.
     * Produces {@code -1} if any operand is {@link Float#isNaN(float) NaN}.
     *
     * @see Double##equivalenceRelation Floating-point Equality, Equivalence, and Comparison
     * @jvms 6.5.fcmp_op <em>fcmp&lt;op&gt;</em>
     * @see Kind#OPERATOR
     */
    FCMPL(RawBytecodeHelper.FCMPL, 1, Kind.OPERATOR),

    /**
     * Compare {@link TypeKind#FLOAT float}.
     * Produces {@code 1} if any operand is {@link Float#isNaN(float) NaN}.
     *
     * @see Double##equivalenceRelation Floating-point Equality, Equivalence, and Comparison
     * @jvms 6.5.fcmp_op <em>fcmp&lt;op&gt;</em>
     * @see Kind#OPERATOR
     */
    FCMPG(RawBytecodeHelper.FCMPG, 1, Kind.OPERATOR),

    /**
     * Compare {@link TypeKind#DOUBLE double}.
     * Produces {@code -1} if any operand is {@link Double#isNaN(double) NaN}.
     *
     * @see Double##equivalenceRelation Floating-point Equality, Equivalence, and Comparison
     * @jvms 6.5.dcmp_op <em>dcmp&lt;op&gt;</em>
     * @see Kind#OPERATOR
     */
    DCMPL(RawBytecodeHelper.DCMPL, 1, Kind.OPERATOR),

    /**
     * Compare {@link TypeKind#DOUBLE double}.
     * Produces {@code 1} if any operand is {@link Double#isNaN(double) NaN}.
     *
     * @see Double##equivalenceRelation Floating-point Equality, Equivalence, and Comparison
     * @jvms 6.5.dcmp_op <em>dcmp&lt;op&gt;</em>
     * @see Kind#OPERATOR
     */
    DCMPG(RawBytecodeHelper.DCMPG, 1, Kind.OPERATOR),

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
     * Access jump table by index and jump.
     *
     * @jvms 6.5.tableswitch <em>tableswitch</em>
     * @see Kind#TABLE_SWITCH
     */
    TABLESWITCH(RawBytecodeHelper.TABLESWITCH, -1, Kind.TABLE_SWITCH),

    /**
     * Access jump table by key match and jump.
     *
     * @jvms 6.5.lookupswitch <em>lookupswitch</em>
     * @see Kind#LOOKUP_SWITCH
     */
    LOOKUPSWITCH(RawBytecodeHelper.LOOKUPSWITCH, -1, Kind.LOOKUP_SWITCH),

    /**
     * Invoke a dynamically-computed call site.
     *
     * @jvms 6.5.invokedynamic <em>invokedynamic</em>
     * @see Kind#INVOKE_DYNAMIC
     */
    INVOKEDYNAMIC(RawBytecodeHelper.INVOKEDYNAMIC, 5, Kind.INVOKE_DYNAMIC),

    /**
     * Create new multidimensional array.
     *
     * @jvms 6.5.multianewarray <em>multianewarray</em>
     * @see Kind#NEW_MULTI_ARRAY
     */
    MULTIANEWARRAY(RawBytecodeHelper.MULTIANEWARRAY, 4, Kind.NEW_MULTI_ARRAY),

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
