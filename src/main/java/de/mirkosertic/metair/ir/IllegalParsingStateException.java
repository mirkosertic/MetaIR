package de.mirkosertic.metair.ir;

public class IllegalParsingStateException extends IllegalStateException {

    private final MethodAnalyzer analyzer;

    public IllegalParsingStateException(final MethodAnalyzer analyzer, final String s, final Throwable t) {
        super(s, t);
        this.analyzer = analyzer;
    }

    public IllegalParsingStateException(final MethodAnalyzer analyzer, final String s) {
        super(s);
        this.analyzer = analyzer;
    }

    public MethodAnalyzer getAnalyzer() {
        return analyzer;
    }
}
