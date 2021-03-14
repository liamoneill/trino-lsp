package net.liamoneill.trinolsp;

import com.google.common.base.CharMatcher;
import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.Statement;
import net.liamoneill.trinolsp.sql.Parser;
import org.checkerframework.checker.units.qual.C;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompletionRunner {

    private static final Collection<String> KNOWN_TABLES = Collections.unmodifiableList(Arrays.asList(
            "profiles",
            "events",
            "orders",
            "carts",
            "returns"));

    private final TrinoLanguageServer trinoLanguageServer;

    public CompletionRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> compute(CompletionParams params) {
        String text = trinoLanguageServer.getTextDocumentService()
                .getOpenedDocument(params.getTextDocument().getUri())
                .getText();

        Either<Statement, ParsingException> parseResult = Parser.parse(text);

        List<CompletionItem> completionItems = new ArrayList<>();
        if (parseResult.isRight()) {
            for (String suggestion : suggestionsFromParsingException(parseResult.getRight(), text)) {
                completionItems.add(new CompletionItem(suggestion));
            }
        }

        return CompletableFuture.completedFuture(Either.forLeft(completionItems));
    }

    private static Collection<String> suggestionsFromParsingException(ParsingException e, String sql)
    {
        String errorMessage = e.getErrorMessage();

        Collection<String> suggestions = Collections.emptyList();

        if (errorMessage.startsWith("mismatched input '<EOF>'. Expecting: ")) {
            suggestions = suggestionsFromEofError(errorMessage, sql);
        } else if (errorMessage.matches("^mismatched input '.*'\\. Expecting: .+")) {
            suggestions = suggestionsFromMismatchedInputError(errorMessage);
        }

        return suggestions;
    }

    private static Collection<String> suggestionsFromMismatchedInputError(String errorMessage)
    {
        String[] expectingTokens = errorMessage.replaceFirst("^mismatched input '.*'\\. Expecting: ", "")
                .split(", ");

        return Arrays.stream(expectingTokens)
                .sorted((t1, t2) -> {
                    if (t1.startsWith("<") && !t2.startsWith("<")) {
                        return -1;
                    }
                    if (!t1.startsWith("<") && t2.startsWith("<")) {
                        return 1;
                    }
                    return t1.compareTo(t2);
                })
                .flatMap(token -> token.equals("<query>")
                        ? Stream.of("SELECT", "WITH")
                        : Stream.of(token))
                .map(token -> {
                    token = CharMatcher.is('\'').trimLeadingFrom(token);
                    token = CharMatcher.is('\'').trimTrailingFrom(token);
                    return token;
                })
                .collect(Collectors.toList());
    }

    private static Collection<String> suggestionsFromEofError(String errorMessage, String sql)
    {
        String[] expectingTokens = errorMessage
                .replace("mismatched input '<EOF>'. Expecting: ", "")
                .split(", ");

        return Arrays.stream(expectingTokens)
                .sorted((t1, t2) -> {
                    if (t1.startsWith("<") && !t2.startsWith("<")) {
                        return -1;
                    }
                    if (!t1.startsWith("<") && t2.startsWith("<")) {
                        return 1;
                    }
                    return t1.compareTo(t2);
                })
                .map(token -> {
                    token = CharMatcher.is('\'').trimLeadingFrom(token);
                    token = CharMatcher.is('\'').trimTrailingFrom(token);
                    return token;
                })
                .flatMap(token -> token.equals("<identifier>") && (
                        sql.trim().toUpperCase().endsWith("FROM") || sql.trim().toUpperCase().endsWith("JOIN"))
                        ? KNOWN_TABLES.stream()
                        : Stream.of(token))
                .collect(Collectors.toList());
    }
}
