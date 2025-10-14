# A list of things not implemented yet in MetaIR

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
