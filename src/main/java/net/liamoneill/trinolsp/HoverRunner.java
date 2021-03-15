package net.liamoneill.trinolsp;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.*;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.concurrent.CompletableFuture;

public class HoverRunner {

    private final TrinoLanguageServer trinoLanguageServer;

    public HoverRunner(TrinoLanguageServer trinoLanguageServer) {
        this.trinoLanguageServer = trinoLanguageServer;
    }

    public CompletableFuture<Hover> compute(HoverParams params) {
        String documentUri = params.getTextDocument().getUri();
        String text = trinoLanguageServer.getTextDocumentService()
                .getOpenedDocument(documentUri)
                .getText();

        Either<Statement, ParsingException> parseResult = Parser.parse(text);

        if (parseResult.isRight()) {
            return CompletableFuture.completedFuture(null);
        }

        HoverVisitor hoverVisitor = new HoverVisitor(params.getPosition());
        hoverVisitor.process(parseResult.getLeft());

        if (hoverVisitor.getHoveredFunctionCall() != null) {
            FunctionCall hoveredFunctionCall = hoverVisitor.getHoveredFunctionCall();
            MarkupContent hoverContent = FunctionDocumentationRegistry.getDocumentation(hoveredFunctionCall.getName());
            Hover hover = hoverContent != null ? new Hover(hoverContent) : null;
            return CompletableFuture.completedFuture(hover);
        }

        return CompletableFuture.completedFuture(null);
    }

    public static class HoverVisitor extends DefaultTraversalVisitor<Void>
    {
        private final Position hoverPosition;
        private Node hoveredNode = null;

        public HoverVisitor(Position hoverPosition) {
            this.hoverPosition = hoverPosition;
        }

        @Override
        protected Void visitFunctionCall(FunctionCall node, Void context) {
            if (node.getLocation().isPresent()
                && Utils.inRange(hoverPosition, functionCallRange(node))) {

                hoveredNode = node;
            }

            return super.visitFunctionCall(node, context);
        }

        private Range functionCallRange(FunctionCall node) {
            if (node.getLocation().isEmpty()) {
                throw new IllegalArgumentException("FunctionCall does not have a location");
            }

            NodeLocation location = node.getLocation().get();
            String name = String.join(".", node.getName().getParts());

            Position start = new Position(location.getLineNumber(), location.getColumnNumber());
            Position end = new Position(location.getLineNumber(), location.getColumnNumber() + name.length());

            return new Range(start, end);
        }

        public FunctionCall getHoveredFunctionCall() {
            return hoveredNode instanceof FunctionCall
                    ? (FunctionCall) hoveredNode
                    : null;
        }
    }

    private static class FunctionDocumentationRegistry {
        private FunctionDocumentationRegistry() {
        }

        public static MarkupContent getDocumentation(QualifiedName function) {
            // TODO programmatically build function documentation
            //  - https://github.com/trinodb/trino/tree/353/docs#tools
            //  - https://mvnrepository.com/artifact/io.trino/trino-docs/353
            //  - https://raw.githubusercontent.com/trinodb/trino/353/docs/src/main/sphinx/functions/json.rst

            String name = function.getSuffix();
            return switch (name) {
                case "json_extract" -> new MarkupContent("markdown", """
                        `json_extract(json, json_path)` -> `json`

        
                        Evaluates the [JSONPath](http://goessner.net/articles/JsonPath/)-like expression `json_path` on `json`
                        (a string containing JSON) and returns the result as a JSON string
                                    
                        ```sql
                        SELECT json_extract(json, '$.store.book');
                        SELECT json_extract(json, '$.store[book]');
                        SELECT json_extract(json, '$.store["book name"]');
                        ```
                        """.trim());
                case "json_extract_scalar" -> new MarkupContent("markdown", """
                        `json_extract_scalar(json, json_path)` -> `varchar`
                        
                        
                        Like `json_extract`, but returns the result value as a string (as opposed
                        to being encoded as JSON). The value referenced by `json_path` must be a
                        scalar (boolean, number or string).
                             
                        ```sql
                        SELECT json_extract_scalar('[1, 2, 3]', '$[2]');
                        SELECT json_extract_scalar(json, '$.store.book[0].author');
                        ```
                         """.trim());
                default -> null;
            };
        }


    }
}
