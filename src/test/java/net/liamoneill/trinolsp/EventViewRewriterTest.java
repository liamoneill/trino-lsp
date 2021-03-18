package net.liamoneill.trinolsp;

import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.Statement;
import net.liamoneill.trinolsp.sql.EventViewRewriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventViewRewriterTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "select 1",
            "select * from not_events",
            "select * from events -- no where clause",
            "select * from events where type = 'pageview' -- no table"
    })
    public void testDoesNotChangeStatementsWhichDoNotMatch(String sql) {
        assertTreeEquals(rewrite(sql), sql);
    }

    private static Stream<Arguments> testCorrectlyRewritesArguments() {
        return Stream.of(
                Arguments.of(
                        "select * from events as e where e.id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d'",
                        "select * from events_by_id as e where e.id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d'"
                ),
                Arguments.of(
                        "select * from events as e where e.type = 'pageview'",
                        "select * from events_by_type as e where e.type = 'pageview'"
                ),
                Arguments.of(
                        "select * from events as e where e.session_id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d'",
                        "select * from events_by_session_id as e where e.session_id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d'"
                ),
                Arguments.of(
                        "select * from events as e where e.session_id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d' and e.type = 'pageview'",
                        "select * from events_by_session_id as e where e.session_id = 'c8b7acbc-2ba8-457e-a3bc-8b904893cc6d' and e.type = 'pageview'"
                ),
                Arguments.of(
                        "select * from events as e where 'pageview' = e.type",
                        "select * from events_by_type as e where 'pageview' = e.type"
                ),
                Arguments.of(
                        "select * from events as e where e.type = 'pageview' and e.created_at > date '2021-01-01'",
                        "select * from events_by_type as e where e.type = 'pageview' and e.created_at > date '2021-01-01'"
                ),
                Arguments.of(
                        "select * from events as e where e.type IN ('pageview')",
                        "select * from events_by_type as e where e.type IN ('pageview')"
                ),
                Arguments.of(
                        "with pagesviews as (select * from events as e where 'pageview' = e.type) select * from pageviews",
                        "with pagesviews as (select * from events_by_type as e where 'pageview' = e.type) select * from pageviews"
                ),
                Arguments.of(
                        "with pagesviews as (select * from events as e where 'pageview' = e.type), all_events as (select * from events as e) select * from pageviews",
                        "with pagesviews as (select * from events_by_type as e where 'pageview' = e.type), all_events as (select * from events as e) select * from pageviews"
                ),
                Arguments.of(
                        "select p.* from profiles as p join events as e on p.id = e.profile_id where e.type = 'pageview'",
                        "select p.* from profiles as p join events_by_type as e on p.id = e.profile_id where e.type = 'pageview'"
                ),
                Arguments.of(
                        "select p.* from profiles as p where p.id IN (select e.profile_id from events as e where e.type = 'pageview' and e.profile_id is not null)",
                        "select p.* from profiles as p where p.id IN (select e.profile_id from events_by_type as e where e.type = 'pageview' and e.profile_id is not null)"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testCorrectlyRewritesArguments")
    public void testCorrectlyRewrites(String sql, String expected) {
        assertTreeEquals(rewrite(sql), expected);
    }

    private static Node rewrite(String sql) {
        ParsingOptions parsingOptions = new ParsingOptions();
        Statement statement = new SqlParser().createStatement(sql, parsingOptions);
        return EventViewRewriter.rewrite(statement);
    }

    private static void assertTreeEquals(Node actual, String expected) {
        ParsingOptions parsingOptions = new ParsingOptions();
        Statement expectedStatement = new SqlParser().createStatement(expected, parsingOptions);
        assertEquals(expectedStatement, actual);
    }
}
