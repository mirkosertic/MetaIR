package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.ir.CFGDominatorTree;
import de.mirkosertic.metair.ir.DOTExporter;
import de.mirkosertic.metair.ir.DominatorTree;
import de.mirkosertic.metair.ir.MethodAnalyzer;
import de.mirkosertic.metair.ir.ResolvedClass;
import de.mirkosertic.metair.ir.ResolvedField;
import de.mirkosertic.metair.ir.ResolvedMethod;
import de.mirkosertic.metair.ir.ResolverContext;
import de.mirkosertic.metair.ir.Sequencer;
import de.mirkosertic.metair.opencl.api.Kernel;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HWACompiledKernel {

    private final ResolverContext resolverContext;
    private final ResolvedClass resolvedKernelClass;
    private ResolvedMethod resolvedKernelMethod;
    private MethodAnalyzer analyzer;
    private final List<HWAKernelArgument> arguments;

    public HWACompiledKernel(final Kernel kernel) {
        final Class<?> origin = kernel.getClass();

        resolverContext = new ResolverContext();
        resolvedKernelClass = resolverContext.resolveClass(origin.getName());

        final List<ResolvedMethod> kernelMethods = new ArrayList<>();

        final ClassModel model = resolvedKernelClass.classModel();
        // We resolve all methods of the kernel claas, but keep also
        // track of the "processWorkItem" main method"
        for (final MethodModel method : model.methods()) {

            final ResolvedMethod m = resolvedKernelClass.resolveMethod(method);
            final MethodAnalyzer ma = m.analyze();

            kernelMethods.add(m);

            if ("processWorkItem".equals(method.methodName().stringValue())) {
                resolvedKernelMethod = m;
                analyzer = ma;
            }
        }

        if (resolvedKernelMethod == null) {
            throw new IllegalArgumentException("The kernel class " + origin.getName() + " does not contain a method processWorkItem");
        }

        final List<HWAKernelArgument> arguments = new ArrayList<>();
        for (final ResolvedField field : resolvedKernelClass.resolvedFields()) {
            arguments.add(new HWAKernelArgument(field.fieldName(), field.type()));
        }

        this.arguments = arguments;

        // Generate the code for the kernel
        final HWAStructuredControlflowCodeGenerator controlFlowGenerator = new HWAStructuredControlflowCodeGenerator(this.arguments);
        for (final ResolvedMethod m : kernelMethods) {
            if (!m.isConstructor()) {
                final MethodAnalyzer analyzer = m.analyze();
                new Sequencer<>(analyzer.ir(), controlFlowGenerator);
            }
        }

        new Sequencer<>(analyzer.ir(), controlFlowGenerator);

        try {
            final Path outputDirectory = Path.of("target", "openclkernels");
            outputDirectory.toFile().mkdirs();

            DOTExporter.writeTo(analyzer.ir(), new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir.dot"))));

            final DominatorTree dominatorTree = new DominatorTree(analyzer.ir());

            DOTExporter.writeTo(dominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_dominatortree.dot"))));

            DOTExporter.writeBytecodeCFGTo(analyzer, new PrintStream(Files.newOutputStream(outputDirectory.resolve("bytecodecfg.dot"))));

            final CFGDominatorTree cfgDominatorTree = new CFGDominatorTree(analyzer.ir());
            DOTExporter.writeTo(cfgDominatorTree, new PrintStream(Files.newOutputStream(outputDirectory.resolve("ir_cfg_dominatortree.dot"))));

            final PrintStream sequenced = new PrintStream(Files.newOutputStream(outputDirectory.resolve("sequenced.txt")));
            sequenced.print(controlFlowGenerator);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
    }
}
