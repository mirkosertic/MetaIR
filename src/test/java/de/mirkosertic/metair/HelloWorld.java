package de.mirkosertic.metair;

import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

public class HelloWorld {

    public static void main(final String[] args) throws IOException {

        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(new File("target/test-classes/de/mirkosertic/metair/Debug2.class").toPath());

        System.out.println("Analyse der Klasse: " + model.thisClass().name());
        System.out.println("Superklasse: " + model.superclass().get().name());

        // Analysiere alle Methoden
        for (final MethodModel method : model.methods()) {
            System.out.println("\nMethode: " + method.methodName().stringValue());
            System.out.println("Descriptor: " + method.methodTypeSymbol().displayDescriptor());
            System.out.println("Zugriff: " + method.flags().flags());

            // Analysiere die Instruktionen
            System.out.println("Instruktionen:");

            final MethodAnalyzer analyzer = new MethodAnalyzer(model.thisClass().asSymbol(), method);

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(new FileOutputStream((method.methodName().stringValue() + "_" + method.methodTypeSymbol().descriptorString() + ".dot").replace("(", "").replace(")", ""))));


            System.out.println();
        }
    }
}
