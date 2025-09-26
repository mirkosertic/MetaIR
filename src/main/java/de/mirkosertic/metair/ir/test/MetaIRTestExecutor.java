package de.mirkosertic.metair.ir.test;

import de.mirkosertic.metair.ir.ResolvedClass;
import de.mirkosertic.metair.ir.ResolvedMethod;
import de.mirkosertic.metair.ir.ResolverContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Path;

public class MetaIRTestExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaIRTestExecutor.class);

    public void execute(final ExecutionRequest request, final TestDescriptor descriptor) {
        if (descriptor instanceof EngineDescriptor) {
            executeContainer(request, descriptor);
        }

        if (descriptor instanceof ClassTestDescriptor) {
            executeContainer(request, descriptor);
        }

        if (descriptor instanceof final MethodTestDescriptor desc) {
            executeMethod(request, desc);
        }
    }

    private void executeContainer(final ExecutionRequest request, final TestDescriptor containerDescriptor) {
        request.getEngineExecutionListener().executionStarted(containerDescriptor);
        containerDescriptor.getChildren()
                .forEach(descriptor -> execute(request, descriptor));
        request.getEngineExecutionListener().executionFinished(containerDescriptor, TestExecutionResult.successful());
    }

    private void executeMethod(final ExecutionRequest request, final MethodTestDescriptor descriptor) {
        request.getEngineExecutionListener().executionStarted(descriptor);
        try {
            final Class<?> origin = descriptor.getTestClass();

            final ResolverContext ctx = new ResolverContext();
            final ResolvedClass resolvedClass = ctx.resolveClass(origin.getName());

            final ClassModel model = resolvedClass.classModel();

            for (final MethodModel method : model.methods()) {

                if (method.methodName().stringValue().equals(descriptor.getMethodName())) {

                    final ResolvedMethod resolvedMethod = resolvedClass.resolveMethod(method);

                    final Path targetDir = request.getOutputDirectoryProvider().createOutputDirectory(descriptor);

                    new MetaIRTestHelper(targetDir, ctx).analyzeAndReport(resolvedMethod);
                }
            }

            LOGGER.info(() -> "Resolved " + ctx.numberOrResolvedClasses() + " classes during test run");

            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.successful());

        } catch (final Exception e) {
            request.getEngineExecutionListener().executionFinished(descriptor, TestExecutionResult.failed(e));
        }
    }
}
