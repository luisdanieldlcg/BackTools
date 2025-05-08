package com.daniking.backtools.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class StringJsonReader {
    public static final char ARRAY_OPEN_CHAR = '[';
    public static final char ARRAY_CLOSE_CHAR = ']';
    public static final char OPEN_OBJECT_CHAR = '{';
    public static final char CLOSE_OBJECT_CHAR = '}';
    public static final char KEY_VALUE_SEPARATOR = ':';
    public static final char SINGLE_QUOTE_CHAR = '\'';
    public static final char DOUBLE_QUOTE_CHAR = '"';
    public static final char COMMA_CHAR = ',';
    public static final char SEMICOLON_CHAR = ';';
    public static final char MINUS_SIGN = '-';
    public static final char PLUS_SIGN = '+';
    public static final char SPACE_CHAR = ' ';
    public static final char COMMENT_HASH_CHAR = '#';
    private static final char FROM_FEED_CHAR = '\f';
    public static final char CARRIAGE_RETURN = '\r';
    public static final char TAB_CHAR = '\t';
    public static final char LINE_BREAK_CHAR = '\n';
    private static final char EQUALS_SIGN = '=';
    private static final char ESCAPE_CHARACTER = '\\';
    private static final char SLASH_CHAR = '/';

    protected final @NotNull String source;
    protected final int sourceLength;
    protected int nextPos = 0;
    protected int previousPos = nextPos;

    protected int lineNumber = 0;

    protected @NotNull PeekStatus peekStatus = PeekStatus.NONE;

    /*
     * The nesting stack. Using a manual array rather than an ArrayList saves 20%.
     */
    protected @NotNull JsonScope[] stack = new JsonScope[32];
    protected int stackSize = 0;

    {
        stack[stackSize++] = JsonScope.EMPTY_DOCUMENT;
    }

    /*
     * The path members. It corresponds directly to stack: At indices where the
     * stack contains an object (EMPTY_OBJECT, DANGLING_NAME or NONEMPTY_OBJECT),
     * pathNames contains the name at this scope. Where it contains an array
     * (EMPTY_ARRAY, NONEMPTY_ARRAY) pathIndices contains the current index in
     * that array. Otherwise the value is undefined, and we take advantage of that
     * by incrementing pathIndices when doing so isn't useful.
     */
    protected String[] pathNames = new String[32];
    protected int[] pathIndices = new int[32];

    protected @NotNull Pattern truePattern = Pattern.compile("^true");
    protected @NotNull Pattern falsePattern = Pattern.compile("^false");
    protected @NotNull Pattern nullPattern = Pattern.compile("^null");

    /** Creates a new instance that reads a JSON-encoded stream from {@code in}. */
    public StringJsonReader(final @NotNull String in) {
        this.source = in;
        this.sourceLength = in.length();
    }

    /**
     * @return the position of the last chat that got read. Will start with 0,
     * and end with String length -1.
     */
    public int getNextPosition() {
        return nextPos;
    }

    public int getPreviousPos() {
        return previousPos;
    }

    public @NotNull String getRemaining() {
        return source.substring(nextPos);
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
     * object.
     *
     * @return the success status of this action
     */
    public boolean tryBeginObject() {
        if (doPeek(false) == PeekStatus.BEGIN_OBJECT) {
            push(JsonScope.EMPTY_OBJECT);
            this.peekStatus = PeekStatus.NONE;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the end of the current
     * object.
     *
     * @return the success status of this action
     */
    public boolean tryEndObject() {
        if (doPeek(false) == PeekStatus.END_OBJECT) {
            stackSize--;
            pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
            pathIndices[stackSize - 1]++;
            this.peekStatus = PeekStatus.NONE;

            return true;
        } else {
            return false;
        }
    }

    public @NotNull PeekStatus doPeek(final boolean forceNext) {
        if (!forceNext && peekStatus != PeekStatus.NONE) {
            return peekStatus;
        }

        final int posBeforePeek = nextPos;
        final @NotNull JsonScope peekStackNow = stack[stackSize - 1];

        switch (peekStackNow) {
            case EMPTY_ARRAY -> stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
            case NONEMPTY_ARRAY -> {
                // Look for a comma before the next element.
                final @Nullable Character character = nextNonWhitespace();

                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }

                switch (character) {
                    case ARRAY_CLOSE_CHAR -> {
                        return peekStatus = PeekStatus.END_ARRAY;
                    }
                    case SEMICOLON_CHAR, COMMA_CHAR -> {
                    }
                    case null -> {
                        return peekStatus = PeekStatus.END_DOCUMENT;
                    }
                    default -> {
                        return peekStatus = PeekStatus.INVALID_UNTERMINATED_ARRAY;
                    }
                }
            }
            case EMPTY_OBJECT, NONEMPTY_OBJECT -> {
                // Look for a comma before the next element.
                if (peekStackNow == JsonScope.NONEMPTY_OBJECT) {
                    final @Nullable Character character = nextNonWhitespace();

                    if (posBeforePeek != nextPos) {
                        previousPos = posBeforePeek;
                    }

                    switch (character) {
                        case CLOSE_OBJECT_CHAR -> {
                            return peekStatus = PeekStatus.END_OBJECT;
                        }
                        case SEMICOLON_CHAR,
                             COMMA_CHAR -> {
                        }
                        case null -> {
                            return peekStatus = PeekStatus.END_DOCUMENT;
                        }
                        default -> {
                            return peekStatus = PeekStatus.INVALID_UNTERMINATED_OBJECT;
                        }
                    }
                }

                final @Nullable Character character = nextNonWhitespace();
                switch (character) {
                    case DOUBLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;

                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        return peekStatus = PeekStatus.DOUBLE_QUOTED_NAME;
                    }
                    case SINGLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;

                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }
                        return peekStatus = PeekStatus.SINGLE_QUOTED_NAME;
                    }
                    case CLOSE_OBJECT_CHAR -> {
                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        if (peekStackNow != JsonScope.NONEMPTY_OBJECT) {
                            return peekStatus = PeekStatus.END_OBJECT;
                        } else {
                            return peekStatus = PeekStatus.INVALID_MISSING_NAME;
                        }
                    }
                    case null -> {
                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        return peekStatus = PeekStatus.END_DOCUMENT;
                    }
                    default -> {
                        nextPos--; // Don't consume the first character in an unquoted string.

                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        if (isLiteral(character)) {
                            stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                            return peekStatus = PeekStatus.UNQUOTED_NAME;
                        } else {
                            return peekStatus = PeekStatus.INVALID_MISSING_NAME;
                        }
                    }
                }
            }
            case DANGLING_NAME -> {
                stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;

                // Look for a colon before the value.
                switch (nextNonWhitespace()) {
                    case EQUALS_SIGN:
                        if (canRead(1) && source.charAt(nextPos) == '>') {
                            nextPos++;
                        }
                    case KEY_VALUE_SEPARATOR:
                        stack[stackSize - 1] = JsonScope.NAME_VALUE_SEPARATOR;

                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        return peekStatus = PeekStatus.NAME_VALUE_SEPARATOR;
                    case null, default:
                        if (posBeforePeek != nextPos) {
                            previousPos = posBeforePeek;
                        }

                        return peekStatus = PeekStatus.DANGLING_NAME;
                }
            }
            case NAME_VALUE_SEPARATOR -> stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
            case EMPTY_DOCUMENT -> stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
            case NONEMPTY_DOCUMENT -> {
                final @Nullable Character character = nextNonWhitespace();
                if (character == null) {
                    if (posBeforePeek != nextPos) {
                        previousPos = posBeforePeek;
                    }

                    return peekStatus = PeekStatus.END_DOCUMENT;
                } else {
                    nextPos--;
                }
            }
        }

        final @Nullable Character character = nextNonWhitespace();
        switch (character) {
            case ARRAY_CLOSE_CHAR:
                if (peekStackNow == JsonScope.EMPTY_ARRAY) {
                    if (posBeforePeek != nextPos) {
                        previousPos = posBeforePeek;
                    }
                    return peekStatus = PeekStatus.END_ARRAY;
                }
            case SEMICOLON_CHAR, COMMA_CHAR:
                // a 0-length literal in an array means 'null'.
                if (peekStackNow == JsonScope.EMPTY_ARRAY || peekStackNow == JsonScope.NONEMPTY_ARRAY) {
                    nextPos--;

                    if (posBeforePeek != nextPos) {
                        previousPos = posBeforePeek;
                    }

                    return peekStatus = PeekStatus.NULL;
                } else {
                    if (posBeforePeek != nextPos) {
                        previousPos = posBeforePeek;
                    }
                    return peekStatus = PeekStatus.INVALID_UNKNOWN;
                }
            case SINGLE_QUOTE_CHAR:
                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }
                return peekStatus = PeekStatus.SINGLE_QUOTED_VALUE;
            case DOUBLE_QUOTE_CHAR:
                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }
                return peekStatus = PeekStatus.DOUBLE_QUOTED_VALUE;
            case ARRAY_OPEN_CHAR:
                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }
                return peekStatus = PeekStatus.BEGIN_ARRAY;
            case OPEN_OBJECT_CHAR:
                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }
                return peekStatus = PeekStatus.BEGIN_OBJECT;
            case null:
                if (posBeforePeek != nextPos) {
                    previousPos = posBeforePeek;
                }
                return PeekStatus.END_DOCUMENT;
            default:
                nextPos--; // Don't consume the first character in a literal value.
        }

        if (readNull(true) ||
            readBoolean(true) != null ||
            readNumber(true) != null) {

            if (posBeforePeek != nextPos) {
                previousPos = posBeforePeek;
            }

            return peekStatus;
        }

        if (posBeforePeek != nextPos) {
            previousPos = posBeforePeek;
        }

        if (!isLiteral(source.charAt(nextPos))) {
            return peekStatus = PeekStatus.INVALID_UNKNOWN;
        }

        return peekStatus = PeekStatus.UNQUOTED_VALUE;
    }

    /// returns true if the next value is null
    public boolean readNull(final boolean peek) {
        final @NotNull String remaining = getRemaining();

        if (nullPattern.matcher(remaining).find()) {
            final int expectedLength = truePattern.pattern().length() - 1;

            if ((canRead(expectedLength + 1)) && isLiteral(source.charAt(nextPos + expectedLength))) {
                return false; // Don't match nullsoft!
            }

            // We've found the keyword followed either by EOF or by a non-literal character.
            if (!peek) {
                previousPos = nextPos;
                nextPos += expectedLength;
                peekStatus = PeekStatus.NONE;
            } else {
                peekStatus = PeekStatus.NULL;
            }

            return true;
        } else {
            return false;
        }
    }

    public @Nullable Boolean readBoolean(final boolean peek) {
        final @NotNull String remaining = getRemaining();
        final int expectedLength;
        final boolean result;

        if (truePattern.matcher(remaining).find()) {
            expectedLength = truePattern.pattern().length() - 1;
            result = true;
        } else if (falsePattern.matcher(remaining).find()) {
            expectedLength = falsePattern.pattern().length() - 1;
            result = false;
        } else {
            return null;
        }

        if ((canRead(expectedLength + 1)) && isLiteral(source.charAt(nextPos + expectedLength))) {
            return null; // Don't match trues, falsey!
        }

        // We've found the keyword followed either by EOF or by a non-literal character.
        if (!peek) {
            previousPos = nextPos;
            nextPos += expectedLength;
            peekStatus = PeekStatus.NONE;
        } else {
            peekStatus = PeekStatus.BOOLEAN;
        }

        return result;
    }

    public @Nullable Number readNumber(final boolean peek) {
        boolean leadingZero = false; // default value never gets used
        @NotNull NumberParsingState last = NumberParsingState.NUMBER_CHAR_NONE;
        int numberLengthLookahead = 0;

        charactersOfNumber:
        while (true) {
            if (numberLengthLookahead >= 1024) {
                // Though this looks like a well-formed number, it's too long to continue reading. Give up
                // and let the application handle this as an unquoted literal.
                return null;
            }

            final char charAt = source.charAt(nextPos + numberLengthLookahead);
            switch (charAt) {
                case MINUS_SIGN -> {
                    if (last == NumberParsingState.NUMBER_CHAR_NONE) {
                        last = NumberParsingState.NUMBER_CHAR_SIGN;
                        numberLengthLookahead++;
                        continue;
                    } else if (last == NumberParsingState.NUMBER_CHAR_EXP_E) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_SIGN;
                        numberLengthLookahead++;
                        continue;
                    }
                    return null;
                }
                case PLUS_SIGN -> {
                    if (last == NumberParsingState.NUMBER_CHAR_EXP_E) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_SIGN;
                        numberLengthLookahead++;
                        continue;
                    }
                    return null;
                }
                case 'e', 'E' -> {
                    if (last == NumberParsingState.NUMBER_CHAR_DIGIT || last == NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_E;
                        numberLengthLookahead++;
                        continue;
                    }
                    return null;
                }
                case '.' -> {
                    if (last == NumberParsingState.NUMBER_CHAR_DIGIT) {
                        last = NumberParsingState.NUMBER_CHAR_DECIMAL;
                        numberLengthLookahead++;
                        continue;
                    }
                    return null;
                }
                default -> {
                    if (charAt < '0' || charAt > '9') {
                        if (!isLiteral(charAt)) {
                            break charactersOfNumber;
                        }
                        return null;
                    }
                    if (last == NumberParsingState.NUMBER_CHAR_SIGN ||
                        last == NumberParsingState.NUMBER_CHAR_NONE) {
                        leadingZero = charAt == '0';

                        last = NumberParsingState.NUMBER_CHAR_DIGIT;
                    } else if (last == NumberParsingState.NUMBER_CHAR_DIGIT) {
                        if (leadingZero) {
                            return null; // Leading '0' prefix is not allowed (since it could be octal). - Just an oddity from javascript
                        }

                    } else if (last == NumberParsingState.NUMBER_CHAR_DECIMAL) {
                        last = NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT;
                    } else if (last == NumberParsingState.NUMBER_CHAR_EXP_E ||
                        last == NumberParsingState.NUMBER_CHAR_EXP_SIGN) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_DIGIT;
                    }
                }
            }
            numberLengthLookahead++;
        }

        if (last == NumberParsingState.NUMBER_CHAR_DIGIT ||
            last == NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT ||
            last == NumberParsingState.NUMBER_CHAR_EXP_DIGIT) {

            if (!peek) {
                previousPos = nextPos;
                nextPos += numberLengthLookahead;

                peekStatus = PeekStatus.NONE;
            } else {
                peekStatus = PeekStatus.NUMBER;
            }

            return NumberUtils.createNumber(source.substring(nextPos, nextPos + numberLengthLookahead));

        } else {
            return null;
        }
    }

    public @Nullable JsonElement readJsonObject() throws JsonIOException, JsonSyntaxException {
        while ( nextPos < sourceLength) {
            final PeekStatus peekStatus1 = doPeek(false);

            switch (peekStatus1) {
                case NONE -> { // should never happen
                    return null;
                }
                case SINGLE_QUOTED_NAME -> {
                    skipQuoted(SINGLE_QUOTE_CHAR);
                    pathNames[stackSize - 1] = "<skipped>";
                    peekStatus = PeekStatus.NONE;
                }
                case DOUBLE_QUOTED_NAME -> {
                    nextPos--;
                    skipQuoted(DOUBLE_QUOTE_CHAR);
                    pathNames[stackSize - 1] = "<skipped>";
                    peekStatus = PeekStatus.NONE;
                }
                case UNQUOTED_NAME -> {
                    skipUnquotedValue();
                    pathNames[stackSize - 1] = "<skipped>";
                    peekStatus = PeekStatus.NONE;
                }
                case DANGLING_NAME,
                     INVALID_UNTERMINATED_ARRAY,
                     INVALID_UNTERMINATED_OBJECT,
                     INVALID_MISSING_NAME,
                     INVALID_UNKNOWN,
                     END_DOCUMENT,
                     END_OBJECT,
                     END_ARRAY -> {
                    return null;
                }
                case NAME_VALUE_SEPARATOR -> peekStatus = PeekStatus.NONE; // just skip the separator.
                case SINGLE_QUOTED_VALUE -> {
                     return tryNextQuoted(SINGLE_QUOTE_CHAR).
                         mapRight(JsonPrimitive::new).
                         getRightOrElse(null);
                }
                case DOUBLE_QUOTED_VALUE -> {
                    return tryNextQuoted(DOUBLE_QUOTE_CHAR).
                        mapRight(JsonPrimitive::new).
                        getRightOrElse(null);
                }
                case UNQUOTED_VALUE -> {
                    return new JsonPrimitive(nextUnquoted());
                }
                case BEGIN_OBJECT,
                     BEGIN_ARRAY -> {
                    nextPos = previousPos; // go back before the value started
                    peekStatus = PeekStatus.NONE;

                    // Use jsonReader directly instead of passing the PartReader to the JsonParser,
                    // and letting the JsonParser construct the JsonReader.
                    // Elsewise the JsonParser tries to parse the whole document instead of just the value.
                    final @NotNull JsonReader jsonReader = new JsonReader(new PartReader());
                    jsonReader.setStrictness(Strictness.LENIENT);

                    return JsonParser.parseReader(jsonReader);
                }
                case BOOLEAN -> {
                    //noinspection DataFlowIssue
                    return new JsonPrimitive (readBoolean(false));
                }
                case NULL -> {
                    return JsonNull.INSTANCE;
                }
                case NUMBER -> {
                    //noinspection DataFlowIssue
                    return new JsonPrimitive (readNumber(false));
                }
            }
        }

        peekStatus = PeekStatus.END_DOCUMENT;
        return null;
    }

    protected boolean isLiteral(final char char_) {
        return switch (char_) {
            case SLASH_CHAR,
                 ESCAPE_CHARACTER,
                 SEMICOLON_CHAR,
                 COMMENT_HASH_CHAR,
                 EQUALS_SIGN,
                 OPEN_OBJECT_CHAR,
                 CLOSE_OBJECT_CHAR,
                 ARRAY_OPEN_CHAR,
                 ARRAY_CLOSE_CHAR,
                 KEY_VALUE_SEPARATOR,
                 COMMA_CHAR,
                 SPACE_CHAR,
                 TAB_CHAR,
                 FROM_FEED_CHAR,
                 CARRIAGE_RETURN,
                 LINE_BREAK_CHAR -> false;
            default -> true;
        };
    }

    public @NotNull Either<@NotNull String, @NotNull String> tryNextName() {
        final @NotNull Either<@Nullable String, @NotNull String> result;
        result = switch (doPeek(false)) {
            case UNQUOTED_NAME -> Either.right(nextUnquoted());
            case SINGLE_QUOTED_NAME -> tryNextQuoted(SINGLE_QUOTE_CHAR);
            case DOUBLE_QUOTED_NAME -> tryNextQuoted(DOUBLE_QUOTE_CHAR);
            default -> Either.left(null);
        };

        if (result.isRight()) {
            peekStatus = PeekStatus.NONE;
            pathNames[stackSize - 1] = result.getRight();
        }
        return result;
    }

    protected @NotNull Either<@NotNull String, @NotNull String> tryNextQuoted(char quote) {
        // Like nextNonWhitespace, this uses locals 'posNow' to save inner-loop field access.
        StringBuilder builder = null;
        int posBefore = nextPos;
        while (true) {
            int posNow = nextPos;
            /* the index of the first character not yet appended to the builder. */
            int start = posNow;
            while (posNow < sourceLength) {
                final char charAt = source.charAt(posNow++);

                if (charAt == quote) {
                    nextPos = posNow;
                    int len = posNow - start - 1;

                    if (posBefore != nextPos) {
                        previousPos = posBefore;
                    }

                    if (builder == null) {
                        return Either.right(source.substring(start, start + len));
                    } else {
                        builder.append(source, start, start + len);
                        return Either.right(builder.toString());
                    }
                } else if (charAt == ESCAPE_CHARACTER) {
                    nextPos = posNow;
                    int len = posNow - start - 1;
                    if (builder == null) {
                        int estimatedLength = (len + 1) * 2;
                        builder = new StringBuilder(Math.max(estimatedLength, 16));
                    }
                    builder.append(source, start, start + len);
                    final @Nullable Character escapedChar = readEscapeCharacter();
                    if (escapedChar != null) {
                        builder.append(escapedChar);
                    }
                    posNow = nextPos;
                    start = posNow;
                } else if (charAt == LINE_BREAK_CHAR) {
                    lineNumber++;
                }
            }

            if (builder == null) {
                int estimatedLength = (posNow - start) * 2;
                builder = new StringBuilder(Math.max(estimatedLength, 16));
            }

            builder.append(source, start, posNow);
            nextPos = posNow;
            if (!canRead(1)) {
                if (posBefore != nextPos) {
                    previousPos = posBefore;
                }

                return Either.left(builder.toString());
            }
        }
    }

    /**
     * Returns an unquoted value as a string.
     */
    protected @NotNull String nextUnquoted() {
        final @NotNull StringBuilder builder = new StringBuilder(16);
        final int posBefore = nextPos;

        for (int posNow = nextPos; posNow < sourceLength; posNow++) {
            final char nextChar = source.charAt(posNow);

            if (!isLiteral(nextChar)) {
                break;
            }

            builder.append(nextChar);
            nextPos++;
        }

        if (posBefore != nextPos) {
            previousPos = posBefore;
        }

        return builder.toString();
    }

    protected void push(final @NotNull JsonScope newTop) {
        if (stackSize == stack.length) {
            int newLength = stackSize * 2;
            stack = Arrays.copyOf(stack, newLength);
            pathIndices = Arrays.copyOf(pathIndices, newLength);
            pathNames = Arrays.copyOf(pathNames, newLength);
        }
        stack[stackSize++] = newTop;
    }

    /**
     * Returns true once {@code limit - pos >= minimum}. If the data is exhausted before that many
     * characters are available, this returns false.
     */
    public boolean canRead(int minimum) {
        // if this is the first time, consume an optional byte order mark (BOM) if it exists
        if (nextPos == 0 && source.charAt(0) == '\ufeff') {
            nextPos++;
            minimum++;
        }

        return sourceLength > nextPos + minimum;
    }

    /**
     * Returns the next character in the stream that is neither whitespace nor a part of a comment.
     * When this returns, the returned character is always at {@code pos-1}; this means the
     * caller can always push back the returned character by decrementing {@code pos}.
     */
    protected @Nullable Character nextNonWhitespace() {
        /*
         * This code uses ugly local variable 'localPos' representing the 'pos'.
         * Using locals rather than field saves a few field reads for each
         * whitespace character in a pretty-printed document, resulting in
         * a 5% speedup. We need to flush 'localPos' to its field before any
         * (potentially indirect) call to canRead() and reread
         * 'localPos' after any (potentially indirect) call to the same method.
         */
        int localPos = nextPos;
        while (true) {
            if (localPos == sourceLength) {
                nextPos = localPos;
                break;
            }

            final char charAt = source.charAt(localPos++);
            if (charAt == LINE_BREAK_CHAR) {
                lineNumber++;
                continue;
            } else if (charAt == SPACE_CHAR || charAt == CARRIAGE_RETURN || charAt == TAB_CHAR) {
                continue;
            }

            if (charAt == '/') {
                nextPos = localPos;
                if (localPos == sourceLength) {
                    if (!canRead(1)) {
                        return charAt;
                    }
                }

                char peek = source.charAt(nextPos);
                switch (peek) {
                    case '*' -> {
                        // skip a /* charAt-style comment */
                        nextPos++;
                        if (!skipComment()) {
                            return null; // Unterminated comment
                        }
                        localPos = nextPos + 2;
                    }
                    case '/' -> {
                        // skip a // end-of-line comment
                        nextPos++;
                        skipToEndOfLine();
                        localPos = nextPos;
                    }
                    default -> {
                        return charAt;
                    }
                }
            } else if (charAt == COMMENT_HASH_CHAR) {
                nextPos = localPos;
                /*
                 * Skip a # hash end-of-line comment. The JSON RFC doesn't
                 * specify this behaviour, but it's required to parse
                 * existing documents. See http://b/2571423.
                 */
                skipToEndOfLine();
                localPos = nextPos;
            } else {
                nextPos = localPos;
                return charAt;
            }
        }

        return null;
    }

    public void skipValue() {
        int count = 0;
        do {
            switch (doPeek(false)) {
                case PeekStatus.BEGIN_ARRAY -> {
                    push(JsonScope.EMPTY_ARRAY);
                    count++;
                }
                case PeekStatus.BEGIN_OBJECT -> {
                    push(JsonScope.EMPTY_OBJECT);
                    count++;
                }
                case PeekStatus.END_ARRAY -> {
                    stackSize--;
                    count--;
                }
                case PeekStatus.END_OBJECT -> {
                    // Only update when object end is explicitly skipped, otherwise stack is not updated
                    // anyways
                    if (count == 0) {
                        // Free the last path name so that it can be garbage collected
                        pathNames[stackSize - 1] = null;
                    }
                    stackSize--;
                    count--;
                }
                case PeekStatus.UNQUOTED_VALUE -> skipUnquotedValue();
                case PeekStatus.SINGLE_QUOTED_VALUE -> skipQuoted(SINGLE_QUOTE_CHAR);
                case PeekStatus.DOUBLE_QUOTED_VALUE -> skipQuoted(DOUBLE_QUOTE_CHAR);
                case PeekStatus.UNQUOTED_NAME -> {
                    skipUnquotedValue();
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case NONE -> { // never happens
                }
                case PeekStatus.SINGLE_QUOTED_NAME -> {
                    skipQuoted(SINGLE_QUOTE_CHAR);
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case PeekStatus.DOUBLE_QUOTED_NAME -> {
                    skipQuoted(DOUBLE_QUOTE_CHAR);
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case PeekStatus.NUMBER -> readNumber(false);
                case PeekStatus.END_DOCUMENT,
                     INVALID_UNTERMINATED_ARRAY,
                     INVALID_UNTERMINATED_OBJECT,
                     INVALID_MISSING_NAME,
                     INVALID_UNKNOWN,
                     DANGLING_NAME -> {
                    // can't do anything here
                    return;
                }
                case NAME_VALUE_SEPARATOR -> {
                    // just skip to value
                }
                case BOOLEAN -> readBoolean(false);
                case NULL -> readNull(false);
                default -> {
                    // For all other tokens there is nothing to do; token has already been consumed from
                    // underlying reader
                }
            }
            peekStatus = PeekStatus.NONE;
        } while (count > 0);

        pathIndices[stackSize - 1]++;
    }

    private void skipUnquotedValue() {
        for (int i = 0; nextPos + i < sourceLength; i++) {
            if (!isLiteral(source.charAt(nextPos + i))) {
                nextPos += i;
                return;
            }
            // skip the character
        }

        nextPos = sourceLength -1;
    }

    private void skipQuoted(final char quoteChar) {
        // Like nextNonWhitespace, this uses local 'localPos' to save inner-loop field access.
        int localPos = nextPos;
        while (localPos < sourceLength) {
            final char charAt = source.charAt(localPos++);

            if (charAt == quoteChar) {
                nextPos = localPos;
                return;
            } else if (charAt == ESCAPE_CHARACTER) {
                nextPos = localPos;
                @SuppressWarnings("unused")
                final @Nullable Character unused = readEscapeCharacter(); // assign to a var, so the compiler can't optimize the call away
                localPos = nextPos;
            } else if (charAt == LINE_BREAK_CHAR) {
                lineNumber++;
            }
        }
    }

    /**
     * Advances the position until after the next newline character. If the line is terminated by
     * "\r\n", the '\n' must be consumed as whitespace by the caller.
     */
    protected void skipToEndOfLine() {
        while (canRead(1)) {
            final char charAt =source.charAt(nextPos++);
            if (charAt == LINE_BREAK_CHAR) {
                lineNumber++;
                break;
            } else if (charAt == CARRIAGE_RETURN) {
                break;
            }
        }
    }

    protected boolean skipComment() {
        for (; canRead(2); nextPos++) {
            if (source.charAt(nextPos) == LINE_BREAK_CHAR) {
                lineNumber++;
                continue;
            }

            if (source.charAt(nextPos) != '*') {
                continue;
            }
            if (source.charAt(nextPos + 1) != '/') {
                continue;
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + locationString();
    }

    protected @NotNull String locationString() {
        int line = lineNumber + 1;
        int column = nextPos - 1;
        return " at line " + line + " column " + column + " path " + getPath();
    }

    protected @NotNull String getPath() {
        StringBuilder result = new StringBuilder().append('$');
        for (int i = 0; i < stackSize; i++) {
            final @NotNull JsonScope scope = stack[i];
            switch (scope) {
                case JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY ->
                    result.append(ARRAY_OPEN_CHAR).append(pathIndices[i]).append(ARRAY_CLOSE_CHAR);
                case JsonScope.EMPTY_OBJECT, JsonScope.DANGLING_NAME, JsonScope.NONEMPTY_OBJECT -> {
                    result.append('.');
                    if (pathNames[i] != null) {
                        result.append(pathNames[i]);
                    }
                }
                case JsonScope.NONEMPTY_DOCUMENT, JsonScope.EMPTY_DOCUMENT -> {
                }
                default -> throw new AssertionError("Unknown scope value: " + scope);
            }
        }

        return result.toString();
    }

    /**
     * Unescapes the character identified by the character or characters that immediately follow a
     * backslash. The backslash '\' should have already been read. This supports both Unicode escapes
     * "u000A" and two-character escapes "\n".
     *
     */
    protected @Nullable Character readEscapeCharacter() {
        if (nextPos >= sourceLength) {
           return null;
        }

        char escaped = source.charAt(nextPos++);
        switch (escaped) {
            case 'u':
                if (!canRead(4)) {
                    nextPos--; // roll back
                    return null; // unterminated escaped char
                }
                // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
                int result = 0;
                for (int i = nextPos, end = nextPos + 4; i < end; i++) {
                    final char charAt = source.charAt(i);
                    result <<= 4;
                    if (charAt >= '0' && charAt <= '9') {
                        result += (charAt - '0');
                    } else if (charAt >= 'a' && charAt <= 'f') {
                        result += (charAt - 'a' + 10);
                    } else if (charAt >= 'A' && charAt <= 'F') {
                        result += (charAt - 'A' + 10);
                    } else {
                        nextPos--; // roll back
                        return null; // Malformed Unicode escape
                    }
                }
                nextPos += 4;
                return (char) result;

            case 't':
                return TAB_CHAR;
            case 'b':
                return '\b';

            case 'n':
                return LINE_BREAK_CHAR;

            case 'r':
                return CARRIAGE_RETURN;

            case 'f':
                return FROM_FEED_CHAR;

            case LINE_BREAK_CHAR:
                lineNumber++;
                // fall-through

            case SINGLE_QUOTE_CHAR,
                 DOUBLE_QUOTE_CHAR, ESCAPE_CHARACTER, SLASH_CHAR:
                return escaped;
            default:
                nextPos--; // roll back
                return null; // Invalid escape sequence
        }
    }

    public enum PeekStatus {
        NONE,

        SINGLE_QUOTED_NAME,
        DOUBLE_QUOTED_NAME,
        UNQUOTED_NAME,
        DANGLING_NAME,

        NAME_VALUE_SEPARATOR,

        BEGIN_OBJECT,
        END_OBJECT,
        BEGIN_ARRAY,
        END_ARRAY,
        BOOLEAN,
        NULL,
        SINGLE_QUOTED_VALUE,
        DOUBLE_QUOTED_VALUE,
        UNQUOTED_VALUE,
        NUMBER,

        INVALID_UNTERMINATED_ARRAY,
        INVALID_UNTERMINATED_OBJECT,
        INVALID_MISSING_NAME,
        INVALID_UNKNOWN,

        END_DOCUMENT,
    }

    protected enum JsonScope {
        /// An array with no elements requires no separator before the next element.
        EMPTY_ARRAY,

        /// An array with at least one value requires a separator before the next element.
        NONEMPTY_ARRAY,

        /// An object with no name/value pairs requires no separator before the next element.
        EMPTY_OBJECT,

        /// An object whose most recent element is a key. The next element must be a value.
        DANGLING_NAME,

        NAME_VALUE_SEPARATOR,

        /// An object with at least one name/value pair requires a separator before the next element.
        NONEMPTY_OBJECT,

        /// No top-level value has been started yet.
        EMPTY_DOCUMENT,

        /// A top-level value has already been started.
        NONEMPTY_DOCUMENT
    }

    /* State machine when parsing numbers */
    protected enum NumberParsingState {
        NUMBER_CHAR_NONE,
        NUMBER_CHAR_SIGN,
        NUMBER_CHAR_DIGIT,
        NUMBER_CHAR_DECIMAL,
        NUMBER_CHAR_FRACTION_DIGIT,
        NUMBER_CHAR_EXP_E,
        NUMBER_CHAR_EXP_SIGN,
        NUMBER_CHAR_EXP_DIGIT
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public class PartReader extends Reader {

        protected PartReader() {
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (nextPos >= sourceLength) {
                    return -1;
                }
                return source.charAt(nextPos++);
            }
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            synchronized (lock) {
                ensureOpen();
                Objects.checkFromIndexSize(off, len, cbuf.length);
                if (len == 0) {
                    return 0;
                }
                if (nextPos >= sourceLength) {
                    return -1;
                }
                int charsRead = Math.min(sourceLength - nextPos, len);
                source.getChars(nextPos, nextPos + charsRead, cbuf, off);
                nextPos += charsRead;

                return charsRead;
            }
        }

        @Override
        public long skip(long numberCharsMaxToSkip) throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (nextPos >= sourceLength) {
                    return 0;
                }

                // Bound skip by beginning and end of the source
                final long numberCharsToSkip = Math.clamp(numberCharsMaxToSkip, -nextPos, sourceLength - nextPos);
                nextPos += (int)numberCharsToSkip;
                return numberCharsToSkip;
            }
        }

        public boolean ready() throws IOException {
            synchronized (lock) {
                ensureOpen();
                return true;
            }
        }

        public void ensureOpen() throws IOException {
            if (nextPos > sourceLength) {
                throw new IOException("Stream closed");
            }
        }

        /** Please note: closing this reader also closes the StringJsonReader this steams from!
         * @inheritDoc
         */
        @Override
        public void close() {
            synchronized (lock) {
                nextPos = Integer.MAX_VALUE;
            }
        }
    }
}
