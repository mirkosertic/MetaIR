package de.mirkosertic.metair.ir.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class MetaIRTestTools implements ParameterResolver {

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext,
                                     final ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == MetaIRTestHelper.class;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext,
                                   final ExtensionContext extensionContext) {

        // TODO: Are we running in Maven, and how do we get the directory provider?

        // Try to get OutputDirectoryProvider from the store
        final ExtensionContext.Store store = extensionContext.getStore(
                ExtensionContext.Namespace.GLOBAL);

        // TODO: This is not working, why?
        final OutputDirectoryProvider provider = store.get(
                OutputDirectoryProvider.class, OutputDirectoryProvider.class);

        final String baseOutputDir = System.getProperty("test.output.dir", "target/metair-test");

        // Create test-specific subdirectory
        final String testClass = extensionContext.getRequiredTestClass().getName();
        final String testMethod = extensionContext.getRequiredTestMethod().getName();

        final Path outputPath = Paths.get(baseOutputDir, testClass, testMethod);

        try {
            Files.createDirectories(outputPath);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to create output directory: " + outputPath, e);
        }

        return new MetaIRTestHelper(outputPath);
    }
}