package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.CFGDominatorTree;
import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.IllegalParsingStateException;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import de.mirkosertic.metair.ir.Node;
import de.mirkosertic.metair.ir.ResolvedClass;
import de.mirkosertic.metair.ir.ResolvedMethod;
import de.mirkosertic.metair.ir.ResolverContext;
import de.mirkosertic.metair.ir.Sequencer;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetaIRTestHelper {

    private final ResolverContext resolverContext;
    private final Path outputDirectory;

    public MetaIRTestHelper(final Path outputDirectory, final ResolverContext resolverContext) {
        this.outputDirectory = outputDirectory;
        this.resolverContext = resolverContext;
    }

    public MethodAnalyzer analyzeAndReport(final ClassModel model, final MethodModel method) throws IOException {
        final ResolvedClass resolvedClass = resolverContext.resolveClass(model);
        return analyzeAndReport(new ResolvedMethod(resolverContext, resolvedClass, method));
    }

    public MethodAnalyzer analyzeAndReport(final ResolvedMethod resolvedMethod) throws IOException {
        try (final var ps = new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecode.yaml")))) {
            ps.print(resolvedMethod.methodModel().toDebugString());
        }

        final ClassDesc thisType = resolvedMethod.thisClass().thisType().type();
        final String fullQualifiedClassNameOfMethod = thisType.packageName() + "." + thisType.displayName();
        final String sourceFileNameURL = "https://raw.githubusercontent.com/mirkosertic/MetaIR/refs/heads/main/src/test/java/" + fullQualifiedClassNameOfMethod.replace(".", "/") + ".java";

        try (final PrintStream ps = new PrintStream(Files.newOutputStream(outputDirectory.resolve("report.html")))) {
            try (final var is = MetaIRTestHelper.class.getResourceAsStream("/report-template.html")) {
                final String content = new String(is.readAllBytes());
                ps.print(content.replace("${sourcefileurl}", sourceFileNameURL));
            }
        }

        try {
            final MethodAnalyzer analyzer = resolvedMethod.analyze();

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
