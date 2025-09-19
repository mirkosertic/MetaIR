package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.CFGDominatorTree;
import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.IRType;
import de.mirkosertic.metair.ir.IllegalParsingStateException;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import de.mirkosertic.metair.ir.Node;
import de.mirkosertic.metair.ir.Sequencer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetaIRTestHelper {

    private final Path outputDirectory;

    public MetaIRTestHelper(final Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public MethodAnalyzer analyzeAndReport(final ClassModel model, final MethodModel method) throws IOException {
        try (final PrintStream ps = new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecode.yaml")))) {
            ps.print(method.toDebugString());
        }

        try {
            final MethodAnalyzer analyzer = new MethodAnalyzer(IRType.MetaClass.of(model.thisClass().asSymbol()), method);

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir.dot"))));

            final DominatorTree dominatorTree = new DominatorTree(analyzer.ir());

            DOTExporter.writeTo(dominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_dominatortree.dot"))));

            DOTExporter.writeBytecodeCFGTo(analyzer, new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecodecfg.dot"))));

            final CFGDominatorTree cfgDominatorTree = new CFGDominatorTree(analyzer.ir());
            DOTExporter.writeTo(cfgDominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_cfg_dominatortree.dot"))));

            final DebugStructuredControlflowCodeGenerator debugStructuredControlflowCodeGenerator = new DebugStructuredControlflowCodeGenerator();
            new Sequencer<>(analyzer.ir(), debugStructuredControlflowCodeGenerator);
            final PrintStream sequenced = new PrintStream(Files.newOutputStream(outputDirectory.resolve("sequenced.txt")));
            sequenced.print(debugStructuredControlflowCodeGenerator);

            return analyzer;
        } catch (final IllegalParsingStateException ex) {

            DOTExporter.writeBytecodeCFGTo(ex.getAnalyzer(), new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecodecfg.dot"))));

            throw ex;
        }
    }

    public static String toDebugExpression(final Node node) {
        final DebugStructuredControlflowCodeGenerator generator = new DebugStructuredControlflowCodeGenerator();
        generator.emit(node);
        return generator.toString();
    }
}
