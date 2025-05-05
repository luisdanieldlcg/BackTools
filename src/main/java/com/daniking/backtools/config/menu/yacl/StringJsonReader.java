package com.daniking.backtools.config.menu.yacl;

/*
 * This is a heavily modified JsonReader, written by google and licensed under the Apache Licence below.
 * The modifications were made to access the underling position on the input string and to gain access to
 * the internal handled structure of json (without having to deal with exceptions)
 *
 *
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.daniking.backtools.config.Either;
import com.google.gson.internal.TroubleshootingGuide;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
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
    protected int pos = 0;

    protected int lineNumber = 0;
    protected int lineStart = 0;

    protected @NotNull PeekStatus peeked = PeekStatus.NONE;

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
    public int getPosition() {
        return pos;
    }

    public @NotNull String getRemaining() {
        return source.substring(pos);
    }

    public @NotNull PartReader getPartReaderAtPos() {
        return new PartReader();
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
     * object.
     *
     * @return the success status of this action
     */
    public boolean tryBeginObject() throws IOException {
        @NotNull PeekStatus peekStatus = peeked;
        if (peekStatus == PeekStatus.NONE) {
            peekStatus = doPeek();
        }

        if (peekStatus == PeekStatus.BEGIN_OBJECT) {
            push(JsonScope.EMPTY_OBJECT);
            peeked = PeekStatus.NONE;
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
    public boolean tryEndObject() throws IOException {
        @NotNull PeekStatus peekStatus = peeked;
        if (peekStatus == PeekStatus.NONE) {
            peekStatus = doPeek();
        }
        if (peekStatus == PeekStatus.END_OBJECT) {
            stackSize--;
            pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
            pathIndices[stackSize - 1]++;
            peeked = PeekStatus.NONE;

            return true;
        } else {
            return false;
        }
    }

    public @NotNull PeekStatus doPeek() throws IOException {
        final @NotNull JsonScope peekStackNow = stack[stackSize - 1];
        switch (peekStackNow) {
            case EMPTY_ARRAY -> stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
            case NONEMPTY_ARRAY -> {
                // Look for a comma before the next element.
                final @Nullable Character character = nextNonWhitespace();
                switch (character) {
                    case ARRAY_CLOSE_CHAR -> {
                        return peeked = PeekStatus.END_ARRAY;
                    }
                    case SEMICOLON_CHAR, COMMA_CHAR -> {
                    }
                    case null -> {
                        return PeekStatus.END_DOCUMENT;
                    }
                    default -> {
                        return PeekStatus.UNTERMINATED_ARRAY;
                    }
                }
            }
            case EMPTY_OBJECT, NONEMPTY_OBJECT -> {
                // Look for a comma before the next element.
                if (peekStackNow == JsonScope.NONEMPTY_OBJECT) {
                    final @Nullable Character character = nextNonWhitespace();

                    switch (character) {
                        case CLOSE_OBJECT_CHAR -> {
                            return peeked = PeekStatus.END_OBJECT;
                        }
                        case SEMICOLON_CHAR,
                             COMMA_CHAR -> {
                        }
                        case null -> {
                            return PeekStatus.END_DOCUMENT;
                        }
                        default -> {
                            return PeekStatus.UNTERMINATED_OBJECT;
                        }
                    }
                }

                final @Nullable Character character = nextNonWhitespace();
                switch (character) {
                    case DOUBLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                        return peeked = PeekStatus.DOUBLE_QUOTED_NAME;
                    }
                    case SINGLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                        return peeked = PeekStatus.SINGLE_QUOTED_NAME;
                    }
                    case CLOSE_OBJECT_CHAR -> {
                        if (peekStackNow != JsonScope.NONEMPTY_OBJECT) {
                            return peeked = PeekStatus.END_OBJECT;
                        } else {
                            throw makeSyntaxError("Expected name");
                        }
                    }
                    case null -> {
                        return PeekStatus.END_DOCUMENT;
                    }
                    default -> {
                        pos--; // Don't consume the first character in an unquoted string.
                        if (isLiteral(character)) {
                            stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                            return peeked = PeekStatus.UNQUOTED_NAME;
                        } else {
                            throw makeSyntaxError("Expected name");
                        }
                    }
                }
            }
            case DANGLING_NAME -> {
                stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;

                // Look for a colon before the value.
                switch (nextNonWhitespace()) {
                    case EQUALS_SIGN:
                        if (canRead(1) && source.charAt(pos) == '>') {
                            pos++;
                        }
                    case KEY_VALUE_SEPARATOR:
                        stack[stackSize - 1] = JsonScope.NAME_VALUE_SEPARATOR;

                        return PeekStatus.NAME_VALUE_SEPARATOR;
                    case null, default:
                        return PeekStatus.DANGLING_NAME;
                }
            }
            case NAME_VALUE_SEPARATOR -> stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
            case EMPTY_DOCUMENT -> stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
            case NONEMPTY_DOCUMENT -> {
                final @Nullable Character character = nextNonWhitespace();
                if (character == null) {
                    return peeked = PeekStatus.END_DOCUMENT;
                } else {
                    pos--;
                }
            }
            case CLOSED -> throw new IllegalStateException("JsonReader is closed");
        }

        final @Nullable Character character = nextNonWhitespace();
        switch (character) {
            case ARRAY_CLOSE_CHAR:
                if (peekStackNow == JsonScope.EMPTY_ARRAY) {
                    return peeked = PeekStatus.END_ARRAY;
                }
            case SEMICOLON_CHAR, COMMA_CHAR:
                // a 0-length literal in an array means 'null'.
                if (peekStackNow == JsonScope.EMPTY_ARRAY || peekStackNow == JsonScope.NONEMPTY_ARRAY) {
                    pos--;
                    return peeked = PeekStatus.NULL;
                } else {
                    throw makeSyntaxError("Unexpected value");
                }
            case SINGLE_QUOTE_CHAR:
                return peeked = PeekStatus.SINGLE_QUOTED_VALUE;
            case DOUBLE_QUOTE_CHAR:
                return peeked = PeekStatus.DOUBLE_QUOTED_VALUE;
            case ARRAY_OPEN_CHAR:
                return peeked = PeekStatus.BEGIN_ARRAY;
            case OPEN_OBJECT_CHAR:
                return peeked = PeekStatus.BEGIN_OBJECT;
            case null:
                return PeekStatus.END_DOCUMENT;
            default:
                pos--; // Don't consume the first character in a literal value.
        }

        if (readNull(true) ||
            readBoolean(true) != null ||
            readNumber(true) != null) {
            return peeked;
        }

        if (!isLiteral(source.charAt(pos))) {
            throw makeSyntaxError("Expected value");
        }

        return peeked = PeekStatus.UNQUOTED_VALUE;
    }

    /// returns true if the next value is null
    public boolean readNull(final boolean peek) {
        final @NotNull String remaining = getRemaining();

        if (nullPattern.matcher(remaining).find()) {
            final int expectedLength = truePattern.pattern().length() - 1;

            if ((canRead(expectedLength + 1)) && isLiteral(source.charAt(pos + expectedLength))) {
                return false; // Don't match nullsoft!
            }

            // We've found the keyword followed either by EOF or by a non-literal character.
            if (!peek) {
                pos += expectedLength;
            }
            peeked = PeekStatus.NULL;

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

        if ((canRead(expectedLength + 1)) && isLiteral(source.charAt(pos + expectedLength))) {
            return null; // Don't match trues, falsey!
        }

        // We've found the keyword followed either by EOF or by a non-literal character.
        if (!peek) {
            pos += expectedLength;
        }

        peeked = PeekStatus.BOOLEAN;

        return result;
    }

    protected @Nullable Number readNumber(final boolean peek) {
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

            final char charAt = source.charAt(pos + numberLengthLookahead);
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
                pos += numberLengthLookahead;
            }
            peeked = PeekStatus.NUMBER;

            return NumberUtils.createNumber(source.substring(pos, pos + numberLengthLookahead));

        } else {
            return null;
        }
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

    public @NotNull Either<@NotNull String, @NotNull String> tryNextName() throws IOException {
        @NotNull PeekStatus peekedStatus = peeked;
        if (peekedStatus == PeekStatus.NONE) {
            peekedStatus = doPeek();
        }

        final @NotNull Either<@NotNull String, @NotNull String> result;
        if (peekedStatus == PeekStatus.UNQUOTED_NAME) {
            result = Either.right(nextUnquotedValue());
        } else if (peekedStatus == PeekStatus.SINGLE_QUOTED_NAME) {
            result = tryNextQuoted(SINGLE_QUOTE_CHAR);
        } else if (peekedStatus == PeekStatus.DOUBLE_QUOTED_NAME) {
            result = tryNextQuoted(DOUBLE_QUOTE_CHAR);
        } else {
            // todo just return left here!
            String troubleshootingId = peekedStatus == PeekStatus.NULL ? "adapter-not-null-safe" : "unexpected-json-structure";
            throw new IllegalStateException(
                "Expected a name but was "
                    + peekedStatus
                    + locationString()
                    + "\nSee "
                    + TroubleshootingGuide.createUrl(troubleshootingId));
        }

        if (result.isRight()) {
            peeked = PeekStatus.NONE;
            pathNames[stackSize - 1] = result.getRight();
        }
        return result;
    }

    protected @NotNull Either<@NotNull String, @NotNull String> tryNextQuoted(char quote) throws IOException {
        // Like nextNonWhitespace, this uses locals 'posNow' to save inner-loop field access.
        StringBuilder builder = null;
        while (true) {
            int posNow = pos;
            /* the index of the first character not yet appended to the builder. */
            int start = posNow;
            while (posNow < sourceLength) {
                final char charAt = source.charAt(posNow++);

                if (charAt == quote) {
                    pos = posNow;
                    int len = posNow - start - 1;
                    if (builder == null) {
                        return Either.right(source.substring(start, start + len));
                    } else {
                        builder.append(source, start, start + len);
                        return Either.right(builder.toString());
                    }
                } else if (charAt == ESCAPE_CHARACTER) {
                    pos = posNow;
                    int len = posNow - start - 1;
                    if (builder == null) {
                        int estimatedLength = (len + 1) * 2;
                        builder = new StringBuilder(Math.max(estimatedLength, 16));
                    }
                    builder.append(source, start, start + len);
                    builder.append(readEscapeCharacter());
                    posNow = pos;
                    start = posNow;
                } else if (charAt == LINE_BREAK_CHAR) {
                    lineNumber++;
                    lineStart = posNow;
                }
            }

            if (builder == null) {
                int estimatedLength = (posNow - start) * 2;
                builder = new StringBuilder(Math.max(estimatedLength, 16));
            }

            builder.append(source, start, posNow);
            pos = posNow;
            if (!canRead(1)) {
                return Either.left(builder.toString());
            }
        }
    }

    /**
     * Returns an unquoted value as a string.
     */
    protected @NotNull String nextUnquotedValue() {
        StringBuilder builder = null;
        int i = 0;

        findNonLiteralCharacter:
        while (true) {
            for (; pos + i < sourceLength; i++) {
                switch (source.charAt(pos + i)) {
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
                         LINE_BREAK_CHAR:
                        break findNonLiteralCharacter;
                    default:
                        // skip character to be included in string value
                }
            }

            // Attempt to load the entire literal into the buffer at once.
            if (i < sourceLength) {
                if (canRead(i + 1)) {
                    continue;
                } else {
                    break;
                }
            }

            // use a StringBuilder when the value is too long. This is too long to be a number!
            if (builder == null) {
                builder = new StringBuilder(Math.max(i, 16));
            }
            builder.append(source, pos, pos + i);
            pos += i;
            i = 0;
            if (!canRead(1)) {
                break;
            }
        }

        String result = (builder == null) ? source.substring(pos, pos + i) : builder.append(source, pos, pos + i).toString();
        pos += i;
        return result;
    }

    public void skipValue() throws IOException {
        // we could parse ourselves and get a bit of speed here,
        // but that would result in a lot of copied code.
        // just use the original here
        new JsonReader(getPartReaderAtPos()).skipValue();
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
        lineStart -= pos; // todo check

        // if this is the first time, consume an optional byte order mark (BOM) if it exists
        if (lineNumber == 0 && lineStart == 0 && source.charAt(0) == '\ufeff') {
            pos++;
            lineStart++;
            minimum++;
        }

        return sourceLength > pos + minimum;
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
        int localPos = pos;
        while (true) {
            if (localPos == sourceLength) {
                pos = localPos;
                break;
            }

            final char charAt = source.charAt(localPos++);
            if (charAt == LINE_BREAK_CHAR) {
                lineNumber++;
                lineStart = localPos;
                continue;
            } else if (charAt == SPACE_CHAR || charAt == CARRIAGE_RETURN || charAt == TAB_CHAR) {
                continue;
            }

            if (charAt == '/') {
                pos = localPos;
                if (localPos == sourceLength) {
                    if (!canRead(1)) {
                        return charAt;
                    }
                }

                char peek = source.charAt(pos);
                switch (peek) {
                    case '*' -> {
                        // skip a /* charAt-style comment */
                        pos++;
                        if (!skipComment()) {
                            return null; // Unterminated comment
                        }
                        localPos = pos + 2;
                    }
                    case '/' -> {
                        // skip a // end-of-line comment
                        pos++;
                        skipToEndOfLine();
                        localPos = pos;
                    }
                    default -> {
                        return charAt;
                    }
                }
            } else if (charAt == COMMENT_HASH_CHAR) {
                pos = localPos;
                /*
                 * Skip a # hash end-of-line comment. The JSON RFC doesn't
                 * specify this behaviour, but it's required to parse
                 * existing documents. See http://b/2571423.
                 */
                skipToEndOfLine();
                localPos = pos;
            } else {
                pos = localPos;
                return charAt;
            }
        }

        return null;
    }

    /**
     * Advances the position until after the next newline character. If the line is terminated by
     * "\r\n", the '\n' must be consumed as whitespace by the caller.
     */
    protected void skipToEndOfLine() {
        while (canRead(1)) {
            final char charAt =source.charAt(pos++);
            if (charAt == LINE_BREAK_CHAR) {
                lineNumber++;
                lineStart = pos;
                break;
            } else if (charAt == CARRIAGE_RETURN) {
                break;
            }
        }
    }

    protected boolean skipComment() {
        for (; canRead(2); pos++) {
            if (source.charAt(pos) == LINE_BREAK_CHAR) {
                lineNumber++;
                lineStart = pos + 1;
                continue;
            }

            if (source.charAt(pos) != '*') {
                continue;
            }
            if (source.charAt(pos + 1) != '/') {
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
        int column = pos - lineStart + 1;
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
                case JsonScope.NONEMPTY_DOCUMENT, JsonScope.EMPTY_DOCUMENT, JsonScope.CLOSED -> {
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
     * @throws MalformedJsonException if the escape sequence is malformed
     */
    protected char readEscapeCharacter() throws IOException {
        if (pos >= sourceLength) {
            throw makeSyntaxError("Unterminated escape sequence");
        }

        char escaped = source.charAt(pos++);
        switch (escaped) {
            case 'u':
                if (!canRead(4)) {
                    throw makeSyntaxError("Unterminated escape sequence");
                }
                // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
                int result = 0;
                for (int i = pos, end = pos + 4; i < end; i++) {
                    final char charAt = source.charAt(i);
                    result <<= 4;
                    if (charAt >= '0' && charAt <= '9') {
                        result += (charAt - '0');
                    } else if (charAt >= 'a' && charAt <= 'f') {
                        result += (charAt - 'a' + 10);
                    } else if (charAt >= 'A' && charAt <= 'F') {
                        result += (charAt - 'A' + 10);
                    } else {
                        throw makeSyntaxError("Malformed Unicode escape \\u");
                    }
                }
                pos += 4;
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
                lineStart = pos;
                // fall-through

            case SINGLE_QUOTE_CHAR,
                 DOUBLE_QUOTE_CHAR, ESCAPE_CHARACTER, SLASH_CHAR:
                return escaped;
            default:
                // throw error when none of the above cases are matched
                throw makeSyntaxError("Invalid escape sequence");
        }
    }

    /**
     * Throws a new {@link MalformedJsonException} with the given message and information about the
     * current location.
     */
    protected MalformedJsonException makeSyntaxError(final @NotNull String message) {
        return new MalformedJsonException(message + locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json"));
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

        UNTERMINATED_ARRAY,
        UNTERMINATED_OBJECT,

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
        NONEMPTY_DOCUMENT,

        /// A document that's been closed and cannot be accessed.
        CLOSED
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

    public class PartReader extends Reader {

        protected PartReader() {
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (pos >= sourceLength) {
                    return -1;
                }
                return source.charAt(pos++);
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
                if (pos >= sourceLength) {
                    return -1;
                }
                int charsRead = Math.min(sourceLength - pos, len);
                source.getChars(pos, pos + charsRead, cbuf, off);
                pos += charsRead;

                return charsRead;
            }
        }

        @Override
        public long skip(long numberCharsMaxToSkip) throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (pos >= sourceLength) {
                    return 0;
                }

                // Bound skip by beginning and end of the source
                final long numberCharsToSkip = Math.clamp(numberCharsMaxToSkip, -pos, sourceLength - pos);
                pos += (int)numberCharsToSkip;
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
            if (pos > sourceLength) {
                throw new IOException("Stream closed");
            }
        }

        /** Please note: closing this reader also closes the StringJsonReader this steams from!
         * @inheritDoc
         */
        @Override
        public void close() {
            synchronized (lock) {
                pos = Integer.MAX_VALUE;
            }
        }
    }
}
