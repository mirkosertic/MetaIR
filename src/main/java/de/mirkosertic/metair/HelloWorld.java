package de.mirkosertic.metair;

import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class HelloWorld {

    public static void main(final String[] args) throws IOException {
        final Type classToAnalyze = Type.getType(Debug2.class);
        final ClassReader reader = new ClassReader(classToAnalyze.getClassName());

        final ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        System.out.println("Analyse der Klasse: " + classNode.name);
        System.out.println("Superklasse: " + classNode.superName);

        // Analysiere alle Methoden
        for (final MethodNode method : classNode.methods) {
            System.out.println("\nMethode: " + method.name);
            System.out.println("Descriptor: " + method.desc);
            System.out.println("Zugriff: " + method.access);

            // Analysiere die Instruktionen
            System.out.println("Instruktionen:");

            final MethodAnalyzer analyzer = new MethodAnalyzer(Type.getObjectType(classNode.name), method);

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(new FileOutputStream(method.name + "_" + method.desc + ".dot")));


            System.out.println();
        }
    }
}
