package net.liamoneill.trinolsp;

import name.fraser.neil.plaintext.diff_match_patch;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {
    }

    public static Position positionAt(String text, int offset) {
        // Adapted from https://github.com/microsoft/vscode/blob/main/extensions/markdown-language-features/src/test/inMemoryDocument.ts#L48

        String before = text.substring(0, offset);
        Matcher newLines = Pattern.compile("\r\n|\n").matcher(before);

        int line = 0;
        if (newLines.find()) {
            line++;
        }

        Matcher preCharacters = Pattern.compile("(\r\n|\n|^).*$").matcher(before);

        int character = preCharacters.find() ? preCharacters.group(0).length() : 0;
        return new Position(line, character);
    }

    public static List<TextEdit> editsForDiff(String originalText, String output) {
        // Adapted from https://github.com/rubyide/vscode-ruby/blob/master/packages/language-server-ruby/src/formatters/BaseFormatter.ts#L91

        diff_match_patch differ = new diff_match_patch();
        List<diff_match_patch.Diff> diffs = differ.diff_main(originalText, output);

        List<TextEdit> edits = new ArrayList<>();
        int position = 0;
        for (diff_match_patch.Diff diff : diffs) {
            String str = diff.text;

            Position startPos = Utils.positionAt(originalText, position);

            Range range;
            switch (diff.operation) {
                case DELETE -> {
                    int endPosition = position + str.length();
                    range = new Range(startPos, Utils.positionAt(originalText, endPosition));
                    edits.add(new TextEdit(range, ""));
                    position = endPosition;
                }
                case INSERT -> {
                    range = new Range(startPos, startPos);
                    edits.add(new TextEdit(range, str));
                }
                case EQUAL -> position += str.length();
            }
        }

        return edits;
    }
}
