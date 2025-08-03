package de.mirkosertic.metair;

import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.MethodAnalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

public final class HelloWorld {

    public static void main(final String[] args) throws IOException {

        final ClassFile cf = ClassFile.of();
        final ClassModel model = cf.parse(new File("target/test-classes/de/mirkosertic/metair/Test.class").toPath());

        System.out.println("Analyse der Klasse: " + model.thisClass().name());
        System.out.println("Superklasse: " + model.superclass().get().name());

        // Analysiere alle Methoden
        for (final MethodModel method : model.methods()) {

            System.out.println(method.toDebugString());

            System.out.println("\nMethode: " + method.methodName().stringValue());
            System.out.println("Descriptor: " + method.methodTypeSymbol().displayDescriptor());
            System.out.println("Zugriff: " + method.flags().flags());

            // Analysiere die Instruktionen
            System.out.println("Instruktionen:");

            final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method);

            final String prefix = (method.methodName().stringValue() + "_" + method.methodTypeSymbol().descriptorString()).replace("<","").replace(">", "").replace("(", "").replace(")", "").replace("/", "").replace(";", "");

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(new FileOutputStream(prefix + ".dot")));

            try (final PrintStream ps = new PrintStream(new FileOutputStream(prefix + ".yaml"))) {
                ps.print(method.toDebugString());
            }

            final DominatorTree dominatorTree = new DominatorTree(analyzer.ir());

            DOTExporter.writeTo(dominatorTree, new PrintStream(new FileOutputStream(prefix+ "_dominatortree.dot")));

            System.out.println();
        }
    }
}
