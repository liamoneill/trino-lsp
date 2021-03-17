package net.liamoneill.trinolsp.http;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.Statement;
import net.liamoneill.trinolsp.sql.EventViewRewriter;
import net.liamoneill.trinolsp.sql.Formatter;
import net.liamoneill.trinolsp.sql.Parser;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class RewriteEndpoint {

    public Response handle(Request request) {
        Either<Statement, ParsingException> parseResult = Parser.parse(request.getSql());
        if (parseResult.isLeft()) {
            Node rewrittenTree = EventViewRewriter.rewrite(parseResult.getLeft());
            String rewrittenSql = Formatter.format(rewrittenTree);
            return new Response(rewrittenSql, null);
        } else {
            return new Response(null, parseResult.getRight().getErrorMessage());
        }
    }

    public static class Request {
        private final String sql;

        public Request(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public static class Response {
        private final String sql;
        private final String errorMessage;

        public Response(String sql, String errorMessage) {
            this.sql = sql;
            this.errorMessage = errorMessage;
        }

        public String getSql() {
            return sql;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
