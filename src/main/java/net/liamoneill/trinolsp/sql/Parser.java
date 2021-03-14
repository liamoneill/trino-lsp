package net.liamoneill.trinolsp.sql;

import io.trino.sql.parser.ParsingException;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.Statement;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class Parser {
    private static final ParsingOptions PARSING_OPTIONS = new ParsingOptions();
    private static final SqlParser SQL_PARSER = new SqlParser();

    private Parser() {
    }

    public static Either<Statement, ParsingException> parse(String sql) {
        try {
            Statement statement = SQL_PARSER.createStatement(sql, PARSING_OPTIONS);
            return Either.forLeft(statement);
        }
        catch (ParsingException e) {
            return Either.forRight(e);
        }
    }
}
