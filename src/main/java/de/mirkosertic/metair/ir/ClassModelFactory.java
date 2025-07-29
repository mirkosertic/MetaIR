package de.mirkosertic.metair.ir;

import java.io.File;
import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.util.function.Consumer;

public final class ClassModelFactory {

    private ClassModelFactory() {
    }

    public static ClassModel createModelFrom(final Consumer<ClassBuilder> consumer) throws IOException {
        final File temp = File.createTempFile("code", ".class");
        ClassFile.of().buildTo(temp.toPath(), ClassDesc.of("de.mirkosertic.test", "Test"), consumer);
        return ClassFile.of().parse(temp.toPath());
    }
}
