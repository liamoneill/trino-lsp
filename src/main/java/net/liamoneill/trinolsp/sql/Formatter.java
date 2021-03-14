package net.liamoneill.trinolsp.sql;

import com.google.common.base.CharMatcher;
import io.trino.sql.SqlFormatter;
import io.trino.sql.parser.ParsingException;
import io.trino.sql.tree.Statement;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import static com.google.common.base.Preconditions.checkState;

public class Formatter {

    private Formatter()
    {
    }

    public static String format(Statement sql)
    {
        String formattedSql = SqlFormatter.formatSql(sql);

        // Check that the original & formatted SQL statements are logically equivalent
        Either<Statement, ParsingException> parseResult = Parser.parse(formattedSql);
        checkState(parseResult.isLeft(), "Formatted SQL is syntactically invalid");
        checkState(sql.equals(parseResult.getLeft()), "Formatted SQL is different than original");

        formattedSql = CharMatcher.is('\n').trimTrailingFrom(formattedSql);
        formattedSql = formattedSql + '\n';

        return formattedSql;
    }
}
