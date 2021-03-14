package net.liamoneill.trinolsp;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.Statement;
import net.liamoneill.trinolsp.sql.Formatter;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FormatterRunner {
    private final TrinoLanguageServer trinoLanguageServer;

    public FormatterRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public CompletableFuture<List<? extends TextEdit>> compute(DocumentFormattingParams params) {
        String text = trinoLanguageServer.getTextDocumentService()
                .getOpenedDocument(params.getTextDocument().getUri())
                .getText();

        Either<Statement, ParsingException> parseResult = Parser.parse(text);
        if (parseResult.isRight()) {
            // Cannot format a document that does not parse.
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Statement statement = parseResult.getLeft();
        String formattedSql = Formatter.format(statement);

        List<TextEdit> edits = Utils.editsForDiff(text, formattedSql);
        return CompletableFuture.completedFuture(edits);
    }


}
