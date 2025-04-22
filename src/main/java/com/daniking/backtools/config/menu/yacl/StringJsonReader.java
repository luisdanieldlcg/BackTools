package com.daniking.backtools.config.menu.yacl;

/*
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
import com.google.gson.Strictness;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.TroubleshootingGuide;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * This class was copied from Gson and modified, so it works internally directly on the given String,
 * skipping the (String)Reader.
 * This extends JsonReader, so it can be used as a drop-in replacement,
 * but except for Javadocs this doesn't share any code with Gson's JsonReader.
 * I really wish I could, but Gson nailed everything internal down using private access instead of protected.
 */
public class StringJsonReader extends JsonReader implements Closeable {
    protected static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;

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
    final int sourceLength;

    protected int pos = 0;

    protected int lineNumber = 0;
    protected int lineStart = 0;

    protected @NotNull PeekedType peeked = PeekedType.PEEKED_NONE;

    /**
     * A peeked value that was composed entirely of digits with an optional leading dash. Positive
     * values may not have a leading 0.
     */
    protected long peekedLong;

    /**
     * The number of characters in a peeked number literal. Increment 'pos' by this after reading a
     * number.
     */
    protected int peekedNumberLength;

    /**
     * A peeked string that should be parsed on the next double, long or string. This is populated
     * before a numeric value is parsed and used if that parsing fails.
     */
    protected String peekedString;

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

    /**
     * Creates a new instance that reads a JSON-encoded stream from {@code in}.
     */
    public StringJsonReader(final @NotNull String in) {
        super(Reader.nullReader()); // ignored

        this.source = in;
        this.sourceLength = in.length();
    }

    @Override
    public void beginArray() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_BEGIN_ARRAY) {
            push(JsonScope.EMPTY_ARRAY);
            pathIndices[stackSize - 1] = 0;
            peeked = PeekedType.PEEKED_NONE;
        } else {
            throw unexpectedTokenError("BEGIN_ARRAY");
        }
    }

    @Override
    public void endArray() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_END_ARRAY) {
            stackSize--;
            pathIndices[stackSize - 1]++;
            peeked = PeekedType.PEEKED_NONE;
        } else {
            throw unexpectedTokenError("END_ARRAY");
        }
    }

    @Override
    public void beginObject() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_BEGIN_OBJECT) {
            push(JsonScope.EMPTY_OBJECT);
            peeked = PeekedType.PEEKED_NONE;
        } else {
            throw unexpectedTokenError("BEGIN_OBJECT");
        }
    }

    @Override
    public void endObject() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_END_OBJECT) {
            stackSize--;
            pathNames[stackSize] = null; // Free the last path name so that it can be garbage collected!
            pathIndices[stackSize - 1]++;
            peeked = PeekedType.PEEKED_NONE;
        } else {
            throw unexpectedTokenError("END_OBJECT");
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        return peekedType != PeekedType.PEEKED_END_OBJECT && peekedType != PeekedType.PEEKED_END_ARRAY && peekedType != PeekedType.PEEKED_EOF;
    }

    @Override
    public JsonToken peek() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }

        return switch (peekedType) {
            case PEEKED_BEGIN_OBJECT -> JsonToken.BEGIN_OBJECT;
            case PEEKED_END_OBJECT -> JsonToken.END_OBJECT;
            case PEEKED_BEGIN_ARRAY -> JsonToken.BEGIN_ARRAY;
            case PEEKED_END_ARRAY -> JsonToken.END_ARRAY;
            case PEEKED_SINGLE_QUOTED_NAME, PEEKED_DOUBLE_QUOTED_NAME, PEEKED_UNQUOTED_NAME -> JsonToken.NAME;
            case PEEKED_TRUE, PEEKED_FALSE -> JsonToken.BOOLEAN;
            case PEEKED_NULL -> JsonToken.NULL;
            case PEEKED_SINGLE_QUOTED, PEEKED_DOUBLE_QUOTED, PEEKED_UNQUOTED, PEEKED_BUFFERED -> JsonToken.STRING;
            case PEEKED_LONG, PEEKED_NUMBER -> JsonToken.NUMBER;
            case PEEKED_EOF -> JsonToken.END_DOCUMENT;
            default -> throw new AssertionError();
        };
    }

    public @NotNull StructureJsonToken structurePeek() throws IOException {
        @NotNull PeekedType peekedType = peeked;

        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(true);
        }

        return switch (peekedType) {
            case PEEKED_BEGIN_OBJECT -> StructureJsonToken.BEGIN_OBJECT;
            case PEEKED_END_OBJECT -> StructureJsonToken.END_OBJECT;
            case PEEKED_BEGIN_ARRAY -> StructureJsonToken.BEGIN_ARRAY;
            case PEEKED_END_ARRAY -> StructureJsonToken.END_ARRAY;
            case PEEKED_UNQUOTED_NAME -> StructureJsonToken.UNQUOTED_NAME;
            case PEEKED_DANGLING_NAME -> StructureJsonToken.DANGLING_NAME;
            case PEEKED_SINGLE_QUOTED_NAME, PEEKED_DOUBLE_QUOTED_NAME -> StructureJsonToken.QUOTED_NAME;
            case PEEKED_NAME_VALUE_SEPARATOR -> StructureJsonToken.NAME_VALUE_SEPARATOR;
            case PEEKED_LONG, PEEKED_NUMBER, PEEKED_UNQUOTED, PEEKED_BUFFERED,
                 PEEKED_TRUE, PEEKED_FALSE -> StructureJsonToken.UNQUOTED_VALUE;
            case PEEKED_SINGLE_QUOTED, PEEKED_DOUBLE_QUOTED -> StructureJsonToken.QUOTED_VALUE;
            case PEEKED_NULL -> StructureJsonToken.NULL;
            case PEEKED_EOF -> StructureJsonToken.END_DOCUMENT;
            case PEEKED_NONE -> throw new IllegalStateException("JsonReader is closed");
        };
    }

    protected @NotNull PeekedType doPeek(final boolean allowInvalid) throws IOException {
        final @NotNull JsonScope peekStack = stack[stackSize - 1];
        switch (peekStack) {
            case EMPTY_ARRAY -> stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
            case NONEMPTY_ARRAY -> {
                // Look for a comma before the next element.
                final @Nullable Character character = nextNonWhitespace();
                switch (character) {
                    case ARRAY_CLOSE_CHAR:
                        return peeked = PeekedType.PEEKED_END_ARRAY;
                    case SEMICOLON_CHAR:
                        checkLenient(); // fall-through
                    case COMMA_CHAR:
                        break;
                    case null:
                        if (allowInvalid) {
                            return PeekedType.PEEKED_EOF;
                        } else {
                            throw new EOFException("End of input" + locationString());
                        }
                    default:
                        throw syntaxError("Unterminated array");
                }
            }
            case EMPTY_OBJECT, NONEMPTY_OBJECT -> {
                // Look for a comma before the next element.
                if (peekStack == JsonScope.NONEMPTY_OBJECT) {
                    final @Nullable Character character = nextNonWhitespace();

                    switch (character) {
                        case CLOSE_OBJECT_CHAR:
                            return peeked = PeekedType.PEEKED_END_OBJECT;
                        case SEMICOLON_CHAR:
                            checkLenient(); // fall-through
                        case COMMA_CHAR:
                            break;
                        case null:
                            if (allowInvalid) {
                                return PeekedType.PEEKED_EOF;
                            } else {
                                throw new EOFException("End of input" + locationString());
                            }
                        default:
                            throw syntaxError("Unterminated object");
                    }
                }

                final @Nullable Character character = nextNonWhitespace();
                switch (character) {
                    case DOUBLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                        return peeked = PeekedType.PEEKED_DOUBLE_QUOTED_NAME;
                    }
                    case SINGLE_QUOTE_CHAR -> {
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                        checkLenient();
                        return peeked = PeekedType.PEEKED_SINGLE_QUOTED_NAME;
                    }
                    case CLOSE_OBJECT_CHAR -> {
                        if (peekStack != JsonScope.NONEMPTY_OBJECT) {
                            return peeked = PeekedType.PEEKED_END_OBJECT;
                        } else {
                            throw syntaxError("Expected name");
                        }
                    }
                    case null -> {
                        if (allowInvalid) {
                            return PeekedType.PEEKED_EOF;
                        } else {
                            throw new EOFException("End of input" + locationString());
                        }
                    }
                    default -> {
                        checkLenient();
                        pos--; // Don't consume the first character in an unquoted string.
                        stack[stackSize - 1] = JsonScope.DANGLING_NAME;
                        if (isLiteral(character)) {
                            return peeked = PeekedType.PEEKED_UNQUOTED_NAME;
                        } else {
                            throw syntaxError("Expected name");
                        }
                    }
                }
            }
            case DANGLING_NAME -> {
                stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
                // Look for a colon before the value.
                final @Nullable Character character = nextNonWhitespace();

                switch (character) {
                    case EQUALS_SIGN:
                        checkLenient();

                        if (checkRemaining(1) && source.charAt(pos) == '>') {
                            pos++;
                        }
                    case KEY_VALUE_SEPARATOR:
                        if (allowInvalid) {
                            stack[stackSize - 1] = JsonScope.NAME_VALUE_SEPARATOR;

                            return PeekedType.PEEKED_NAME_VALUE_SEPARATOR;
                        }
                        break;
                    case null:
                        if (allowInvalid) {
                            return PeekedType.PEEKED_DANGLING_NAME;
                        } else {
                            throw new EOFException("End of input" + locationString());
                        }
                    default:
                        if (allowInvalid) {
                            return PeekedType.PEEKED_DANGLING_NAME;
                        }
                        throw syntaxError("Expected ':'");
                }
            }
            case NAME_VALUE_SEPARATOR -> stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
            case EMPTY_DOCUMENT -> stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
            case NONEMPTY_DOCUMENT -> {
                final @Nullable Character character = nextNonWhitespace();
                if (character == null) {
                    return peeked = PeekedType.PEEKED_EOF;
                } else {
                    checkLenient();
                    pos--;
                }
            }
            case CLOSED -> throw new IllegalStateException("JsonReader is closed");
        }

        final @Nullable Character character = nextNonWhitespace();
        switch (character) {
            case ARRAY_CLOSE_CHAR:
                if (peekStack == JsonScope.EMPTY_ARRAY) {
                    return peeked = PeekedType.PEEKED_END_ARRAY;
                }
            case SEMICOLON_CHAR, COMMA_CHAR:
                // In lenient mode, a 0-length literal in an array means 'null'.
                if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
                    checkLenient();
                    pos--;
                    return peeked = PeekedType.PEEKED_NULL;
                } else {
                    throw syntaxError("Unexpected value");
                }
            case SINGLE_QUOTE_CHAR:
                checkLenient();
                return peeked = PeekedType.PEEKED_SINGLE_QUOTED;
            case DOUBLE_QUOTE_CHAR:
                return peeked = PeekedType.PEEKED_DOUBLE_QUOTED;
            case ARRAY_OPEN_CHAR:
                return peeked = PeekedType.PEEKED_BEGIN_ARRAY;
            case OPEN_OBJECT_CHAR:
                return peeked = PeekedType.PEEKED_BEGIN_OBJECT;
            case null:
                if (allowInvalid) {
                    return PeekedType.PEEKED_EOF;
                } else {
                    throw new EOFException("End of input" + locationString());
                }
            default:
                pos--; // Don't consume the first character in a literal value.
        }

        @NotNull PeekedType result;
        if ((result = peekKeyword()) != PeekedType.PEEKED_NONE) {
            return result;
        }

        if ((result = peekNumber()) != PeekedType.PEEKED_NONE) {
            return result;
        }

        if (!isLiteral(source.charAt(pos))) {
            throw syntaxError("Expected value");
        }

        checkLenient();
        return peeked = PeekedType.PEEKED_UNQUOTED;
    }

    protected @NotNull PeekedType peekKeyword() throws IOException {
        // Figure out which keyword we're matching against by its first character.
        char charAt = source.charAt(pos);
        String keyword;
        String keywordUpper;
        @NotNull PeekedType peeking;

        // Look at the first letter to determine what keyword we are trying to match.
        if (charAt == 't' || charAt == 'T') {
            keyword = "true";
            keywordUpper = "TRUE";
            peeking = PeekedType.PEEKED_TRUE;
        } else if (charAt == 'f' || charAt == 'F') {
            keyword = "false";
            keywordUpper = "FALSE";
            peeking = PeekedType.PEEKED_FALSE;
        } else if (charAt == 'n' || charAt == 'N') {
            keyword = "null";
            keywordUpper = "NULL";
            peeking = PeekedType.PEEKED_NULL;
        } else {
            return PeekedType.PEEKED_NONE;
        }

        // Uppercased keywords are not allowed in STRICT mode
        boolean allowsUpperCased = getStrictness() != Strictness.STRICT;

        // Confirm that chars [0..expectedLength) match the keyword.
        int expectedLength = keyword.length();
        for (int i = 0; i < expectedLength; i++) {
            if (pos + i >= sourceLength) {
                return PeekedType.PEEKED_NONE;
            }
            charAt = source.charAt(pos + i);
            boolean matched = charAt == keyword.charAt(i) || (allowsUpperCased && charAt == keywordUpper.charAt(i));
            if (!matched) {
                return PeekedType.PEEKED_NONE;
            }
        }

        if ((checkRemaining(expectedLength + 1)) && isLiteral(source.charAt(pos + expectedLength))) {
            return PeekedType.PEEKED_NONE; // Don't match trues, falsey or nullsoft!
        }

        // We've found the keyword followed either by EOF or by a non-literal character.
        pos += expectedLength;
        return peeked = peeking;
    }

    protected @NotNull PeekedType peekNumber() throws IOException {
        long value = 0; // Negative to accommodate Long.MIN_VALUE more easily.
        boolean negative = false;
        boolean fitsInLong = true;
        @NotNull NumberParsingState last = NumberParsingState.NUMBER_CHAR_NONE;

        int numberLengthLookahead = 0;

        charactersOfNumber:
        while (true) {
            if (pos + numberLengthLookahead == sourceLength) {
                if (numberLengthLookahead >= 1024) {
                    // Though this looks like a well-formed number, it's too long to continue reading. Give up
                    // and let the application handle this as an unquoted literal.
                    return PeekedType.PEEKED_NONE;
                }
            }

            final char charAt = source.charAt(pos + numberLengthLookahead);
            switch (charAt) {
                case MINUS_SIGN -> {
                    if (last == NumberParsingState.NUMBER_CHAR_NONE) {
                        negative = true;
                        last = NumberParsingState.NUMBER_CHAR_SIGN;
                        numberLengthLookahead++;
                        continue;
                    } else if (last == NumberParsingState.NUMBER_CHAR_EXP_E) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_SIGN;
                        numberLengthLookahead++;
                        continue;
                    }
                    return PeekedType.PEEKED_NONE;
                }
                case PLUS_SIGN -> {
                    if (last == NumberParsingState.NUMBER_CHAR_EXP_E) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_SIGN;
                        numberLengthLookahead++;
                        continue;
                    }
                    return PeekedType.PEEKED_NONE;
                }
                case 'e', 'E' -> {
                    if (last == NumberParsingState.NUMBER_CHAR_DIGIT || last == NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_E;
                        numberLengthLookahead++;
                        continue;
                    }
                    return PeekedType.PEEKED_NONE;
                }
                case '.' -> {
                    if (last == NumberParsingState.NUMBER_CHAR_DIGIT) {
                        last = NumberParsingState.NUMBER_CHAR_DECIMAL;
                        numberLengthLookahead++;
                        continue;
                    }
                    return PeekedType.PEEKED_NONE;
                }
                default -> {
                    if (charAt < '0' || charAt > '9') {
                        if (!isLiteral(charAt)) {
                            break charactersOfNumber;
                        }
                        return PeekedType.PEEKED_NONE;
                    }
                    if (last == NumberParsingState.NUMBER_CHAR_SIGN || last == NumberParsingState.NUMBER_CHAR_NONE) {
                        value = -(charAt - '0');
                        last = NumberParsingState.NUMBER_CHAR_DIGIT;
                    } else if (last == NumberParsingState.NUMBER_CHAR_DIGIT) {
                        if (value == 0) {
                            return PeekedType.PEEKED_NONE; // Leading '0' prefix is not allowed (since it could be octal).
                        }
                        long newValue = value * 10 - (charAt - '0');
                        fitsInLong &=
                            value > MIN_INCOMPLETE_INTEGER
                                || (value == MIN_INCOMPLETE_INTEGER && newValue < value);
                        value = newValue;
                    } else if (last == NumberParsingState.NUMBER_CHAR_DECIMAL) {
                        last = NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT;
                    } else if (last == NumberParsingState.NUMBER_CHAR_EXP_E || last == NumberParsingState.NUMBER_CHAR_EXP_SIGN) {
                        last = NumberParsingState.NUMBER_CHAR_EXP_DIGIT;
                    }
                }
            }
            numberLengthLookahead++;
        }

        // We've read a complete number. Decide if it's a PEEKED_LONG or a PEEKED_NUMBER.
        // Don't store -0 as long; user might want to read it as double -0.0
        // Don't try to convert Long.MIN_VALUE to positive long; it would overflow MAX_VALUE
        if (last == NumberParsingState.NUMBER_CHAR_DIGIT
            && fitsInLong
            && (value != Long.MIN_VALUE || negative)
            && (value != 0 || !negative)) {
            peekedLong = negative ? value : -value;
            pos += numberLengthLookahead;
            return peeked = PeekedType.PEEKED_LONG;
        } else if (
            last == NumberParsingState.NUMBER_CHAR_DIGIT ||
                last == NumberParsingState.NUMBER_CHAR_FRACTION_DIGIT ||
                last == NumberParsingState.NUMBER_CHAR_EXP_DIGIT) {
            peekedNumberLength = numberLengthLookahead;
            return peeked = PeekedType.PEEKED_NUMBER;
        } else {
            return PeekedType.PEEKED_NONE;
        }
    }

    protected boolean isLiteral(final char char_) throws IOException {
        switch (char_) {
            case SLASH_CHAR,
                 ESCAPE_CHARACTER,
                 SEMICOLON_CHAR,
                 COMMENT_HASH_CHAR,
                 EQUALS_SIGN:
                checkLenient(); // fall-through
            case OPEN_OBJECT_CHAR,
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
                return false;
            default:
                return true;
        }
    }

    public @NotNull Either<@NotNull String, @NotNull String> tryNextName() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }

        final @NotNull Either<@NotNull String, @NotNull String> result;
        if (peekedType == PeekedType.PEEKED_UNQUOTED_NAME) {
            result = Either.right(nextUnquotedValue());
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED_NAME) {
            result = tryNextQuotedValue(SINGLE_QUOTE_CHAR);
        } else if (peekedType == PeekedType.PEEKED_DOUBLE_QUOTED_NAME) {
            result = tryNextQuotedValue(DOUBLE_QUOTE_CHAR);
        } else {
            throw unexpectedTokenError("a name");
        }

        if (result.isRight()) {
            peeked = PeekedType.PEEKED_NONE;
            pathNames[stackSize - 1] = result.getRight();
        }
        return result;
    }

    @Override
    public @NotNull String nextName() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        String result;
        if (peekedType == PeekedType.PEEKED_UNQUOTED_NAME) {
            result = nextUnquotedValue();
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED_NAME) {
            result = nextQuotedValue(SINGLE_QUOTE_CHAR);
        } else if (peekedType == PeekedType.PEEKED_DOUBLE_QUOTED_NAME) {
            result = nextQuotedValue(DOUBLE_QUOTE_CHAR);
        } else {
            throw unexpectedTokenError("a name");
        }
        peeked = PeekedType.PEEKED_NONE;
        pathNames[stackSize - 1] = result;
        return result;
    }

    @Override
    public @NotNull String nextString() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        String result;
        if (peekedType == PeekedType.PEEKED_UNQUOTED) {
            result = nextUnquotedValue();
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED) {
            result = nextQuotedValue(SINGLE_QUOTE_CHAR);
        } else if (peekedType == PeekedType.PEEKED_DOUBLE_QUOTED) {
            result = nextQuotedValue(DOUBLE_QUOTE_CHAR);
        } else if (peekedType == PeekedType.PEEKED_BUFFERED) {
            result = peekedString;
            peekedString = null;
        } else if (peekedType == PeekedType.PEEKED_LONG) {
            result = Long.toString(peekedLong);
        } else if (peekedType == PeekedType.PEEKED_NUMBER) {
            result = source.substring(pos, pos + peekedNumberLength);
            pos += peekedNumberLength;
        } else {
            throw unexpectedTokenError("a string");
        }
        peeked = PeekedType.PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
    }

    @Override
    public boolean nextBoolean() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_TRUE) {
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
            return true;
        } else if (peekedType == PeekedType.PEEKED_FALSE) {
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
            return false;
        }
        throw unexpectedTokenError("a boolean");
    }

    @Override
    public void nextNull() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }
        if (peekedType == PeekedType.PEEKED_NULL) {
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
        } else {
            throw unexpectedTokenError("null");
        }
    }

    @Override
    public double nextDouble() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }

        if (peekedType == PeekedType.PEEKED_LONG) {
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
            return (double) peekedLong;
        }

        if (peekedType == PeekedType.PEEKED_NUMBER) {
            peekedString = source.substring(pos, pos + peekedNumberLength);
            pos += peekedNumberLength;
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED || peekedType == PeekedType.PEEKED_DOUBLE_QUOTED) {
            peekedString = nextQuotedValue(peekedType == PeekedType.PEEKED_SINGLE_QUOTED ? SINGLE_QUOTE_CHAR : DOUBLE_QUOTE_CHAR);
        } else if (peekedType == PeekedType.PEEKED_UNQUOTED) {
            peekedString = nextUnquotedValue();
        } else if (peekedType != PeekedType.PEEKED_BUFFERED) {
            throw unexpectedTokenError("a double");
        }

        peeked = PeekedType.PEEKED_BUFFERED;
        double result = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
        if (getStrictness() != Strictness.LENIENT && (Double.isNaN(result) || Double.isInfinite(result))) {
            throw syntaxError("JSON forbids NaN and infinities: " + result);
        }
        peekedString = null;
        peeked = PeekedType.PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
    }

    @Override
    public long nextLong() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }

        if (peekedType == PeekedType.PEEKED_LONG) {
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
            return peekedLong;
        }

        if (peekedType == PeekedType.PEEKED_NUMBER) {
            peekedString = source.substring(pos, pos + peekedNumberLength);
            pos += peekedNumberLength;
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED || peekedType == PeekedType.PEEKED_DOUBLE_QUOTED || peekedType == PeekedType.PEEKED_UNQUOTED) {
            if (peekedType == PeekedType.PEEKED_UNQUOTED) {
                peekedString = nextUnquotedValue();
            } else {
                peekedString = nextQuotedValue(peekedType == PeekedType.PEEKED_SINGLE_QUOTED ? SINGLE_QUOTE_CHAR : DOUBLE_QUOTE_CHAR);
            }
            try {
                long result = Long.parseLong(peekedString);
                peeked = PeekedType.PEEKED_NONE;
                pathIndices[stackSize - 1]++;
                return result;
            } catch (NumberFormatException ignored) {
                // Fall back to parse as a double below.
            }
        } else {
            throw unexpectedTokenError("a long");
        }

        peeked = PeekedType.PEEKED_BUFFERED;
        double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
        long result = (long) asDouble;
        if (result != asDouble) { // Make sure no precision was lost casting to 'long'.
            throw new NumberFormatException("Expected a long but was " + peekedString + locationString());
        }
        peekedString = null;
        peeked = PeekedType.PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
    }

    /**
     * Returns the string up to but not including {@code quote}, unescaping any character escape
     * sequences encountered along the way. The opening quote should have already been read. This
     * consumes the closing quote, but does not include it in the returned string.
     *
     * @param quote either ' or ".
     */
    protected String nextQuotedValue(char quote) throws IOException {
        return tryNextQuotedValue(quote).getRightOrElseThrow(ignored -> new MalformedJsonException(
            "Unterminated string" + locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json")));
    }

    protected @NotNull Either<@NotNull String, @NotNull String> tryNextQuotedValue(char quote) throws IOException {
        // Like nextNonWhitespace, this uses locals 'posNow' to save inner-loop field access.
        StringBuilder builder = null;
        while (true) {
            int posNow = pos;
            /* the index of the first character not yet appended to the builder. */
            int start = posNow;
            while (posNow < sourceLength) {
                char charAt = source.charAt(posNow++);

                // In strict mode, throw an exception when meeting unescaped control characters (U+0000
                // through U+001F)
                if (getStrictness() == Strictness.STRICT && charAt < 0x20) {
                    throw syntaxError("Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");
                } else if (charAt == quote) {
                    pos = posNow;
                    int len = posNow - start - 1;
                    if (builder == null) {
                        return Either.right(source.substring(start, start + len));
                    } else {
                        builder.append(source, start, start + len);
                        return Either.right(builder.toString());
                    }
                } else if (charAt == '\\') {
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
            if (!checkRemaining(1)) {
                return Either.left(builder.toString());
            }
        }
    }

    /**
     * Returns an unquoted value as a string.
     */
    protected @NotNull String nextUnquotedValue() throws IOException {
        StringBuilder builder = null;
        int i = 0;

        findNonLiteralCharacter:
        while (true) {
            for (; pos + i < sourceLength; i++) {
                switch (source.charAt(pos + i)) {
                    case '/', '\\', SEMICOLON_CHAR, COMMENT_HASH_CHAR, '=':
                        checkLenient(); // fall-through
                    case OPEN_OBJECT_CHAR, CLOSE_OBJECT_CHAR, ARRAY_OPEN_CHAR, ARRAY_CLOSE_CHAR, KEY_VALUE_SEPARATOR,
                         COMMA_CHAR, SPACE_CHAR, TAB_CHAR, FROM_FEED_CHAR, CARRIAGE_RETURN, LINE_BREAK_CHAR:
                        break findNonLiteralCharacter;
                    default:
                        // skip character to be included in string value
                }
            }

            // Attempt to load the entire literal into the buffer at once.
            if (i < sourceLength) {
                if (checkRemaining(i + 1)) {
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
            if (!checkRemaining(1)) {
                break;
            }
        }

        String result = (builder == null) ? source.substring(pos, pos + i) : builder.append(source, pos, pos + i).toString();
        pos += i;
        return result;
    }

    protected void skipQuotedValue(char quote) throws IOException {
        // Like nextNonWhitespace, this uses locals 'localPos' to save inner-loop field access.
        do {
            int localPos = pos;
            /* the index of the first character not yet appended to the builder. */
            while (localPos < sourceLength) {
                int c = source.charAt(localPos++);
                if (c == quote) {
                    pos = localPos;
                    return;
                } else if (c == ESCAPE_CHARACTER) {
                    pos = localPos;
                    readEscapeCharacter();
                    localPos = pos;
                } else if (c == LINE_BREAK_CHAR) {
                    lineNumber++;
                    lineStart = localPos;
                }
            }
            pos = localPos;
        } while (checkRemaining(1));
        throw syntaxError("Unterminated string");
    }

    protected void skipUnquotedValue() throws IOException {
        do {
            int i = 0;
            for (; pos + i < sourceLength; i++) {
                switch (source.charAt(pos + i)) {
                    case SLASH_CHAR:
                    case ESCAPE_CHARACTER:
                    case SEMICOLON_CHAR:
                    case COMMENT_HASH_CHAR:
                    case EQUALS_SIGN:
                        checkLenient(); // fall-through
                    case OPEN_OBJECT_CHAR:
                    case CLOSE_OBJECT_CHAR:
                    case ARRAY_OPEN_CHAR:
                    case ARRAY_CLOSE_CHAR:
                    case KEY_VALUE_SEPARATOR:
                    case COMMA_CHAR:
                    case SPACE_CHAR:
                    case TAB_CHAR:
                    case FROM_FEED_CHAR:
                    case CARRIAGE_RETURN:
                    case LINE_BREAK_CHAR:
                        pos += i;
                        return;
                    default:
                        // skip the character
                }
            }
            pos += i;
        } while (checkRemaining(1));
    }

    @Override
    public int nextInt() throws IOException {
        @NotNull PeekedType peekedType = peeked;
        if (peekedType == PeekedType.PEEKED_NONE) {
            peekedType = doPeek(false);
        }

        int result;
        if (peekedType == PeekedType.PEEKED_LONG) {
            result = (int) peekedLong;
            if (peekedLong != result) { // Make sure no precision was lost casting to 'int'.
                throw new NumberFormatException("Expected an int but was " + peekedLong + locationString());
            }
            peeked = PeekedType.PEEKED_NONE;
            pathIndices[stackSize - 1]++;
            return result;
        }

        if (peekedType == PeekedType.PEEKED_NUMBER) {
            peekedString = source.substring(pos, pos + peekedNumberLength);
            pos += peekedNumberLength;
        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED || peekedType == PeekedType.PEEKED_DOUBLE_QUOTED || peekedType == PeekedType.PEEKED_UNQUOTED) {
            if (peekedType == PeekedType.PEEKED_UNQUOTED) {
                peekedString = nextUnquotedValue();
            } else {
                peekedString = nextQuotedValue(peekedType == PeekedType.PEEKED_SINGLE_QUOTED ? SINGLE_QUOTE_CHAR : DOUBLE_QUOTE_CHAR);
            }
            try {
                result = Integer.parseInt(peekedString);
                peeked = PeekedType.PEEKED_NONE;
                pathIndices[stackSize - 1]++;
                return result;
            } catch (NumberFormatException ignored) {
                // Fall back to parse as a double below.
            }
        } else {
            throw unexpectedTokenError("an int");
        }

        peeked = PeekedType.PEEKED_BUFFERED;
        double asDouble = Double.parseDouble(peekedString); // don't catch this NumberFormatException.
        result = (int) asDouble;
        if (result != asDouble) { // Make sure no precision was lost casting to 'int'.
            throw new NumberFormatException("Expected an int but was " + peekedString + locationString());
        }
        peekedString = null;
        peeked = PeekedType.PEEKED_NONE;
        pathIndices[stackSize - 1]++;
        return result;
    }

    @Override
    public void close() throws IOException {
        peeked = PeekedType.PEEKED_NONE;
        stack[0] = JsonScope.CLOSED;
        stackSize = 1;

        super.close();
    }

    @Override
    public void skipValue() throws IOException {
        int count = 0;
        do {
            @NotNull PeekedType peekedType = peeked;
            if (peekedType == PeekedType.PEEKED_NONE) {
                peekedType = doPeek(false);
            }

            switch (peekedType) {
                case PEEKED_BEGIN_ARRAY -> {
                    push(JsonScope.EMPTY_ARRAY);
                    count++;
                }
                case PEEKED_BEGIN_OBJECT -> {
                    push(JsonScope.EMPTY_OBJECT);
                    count++;
                }
                case PEEKED_END_ARRAY -> {
                    stackSize--;
                    count--;
                }
                case PEEKED_END_OBJECT -> {
                    // Only update when object end is explicitly skipped, otherwise stack is not updated
                    // anyways
                    if (count == 0) {
                        // Free the last path name so that it can be garbage collected
                        pathNames[stackSize - 1] = null;
                    }
                    stackSize--;
                    count--;
                }
                case PEEKED_UNQUOTED -> skipUnquotedValue();
                case PEEKED_SINGLE_QUOTED -> skipQuotedValue(SINGLE_QUOTE_CHAR);
                case PEEKED_DOUBLE_QUOTED -> skipQuotedValue(DOUBLE_QUOTE_CHAR);
                case PEEKED_UNQUOTED_NAME -> {
                    skipUnquotedValue();
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case PEEKED_SINGLE_QUOTED_NAME -> {
                    skipQuotedValue(SINGLE_QUOTE_CHAR);
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case PEEKED_DOUBLE_QUOTED_NAME -> {
                    skipQuotedValue(DOUBLE_QUOTE_CHAR);
                    // Only update when name is explicitly skipped, otherwise stack is not updated anyways
                    if (count == 0) {
                        pathNames[stackSize - 1] = "<skipped>";
                    }
                }
                case PEEKED_NUMBER -> pos += peekedNumberLength;
                case PEEKED_EOF -> {
                    return; // Do nothing
                }
                default -> {
                    // For all other tokens there is nothing to do; token has already been consumed from
                    // underlying reader
                }
            }
            peeked = PeekedType.PEEKED_NONE;
        } while (count > 0);

        pathIndices[stackSize - 1]++;
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
    protected boolean checkRemaining(int minimum) {
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
    protected @Nullable Character nextNonWhitespace() throws IOException {
        /*
         * This code uses ugly local variable 'localPos' representing the 'pos'.
         * Using locals rather than field saves a few field reads for each
         * whitespace character in a pretty-printed document, resulting in
         * a 5% speedup. We need to flush 'localPos' to its field before any
         * (potentially indirect) call to checkRemaining() and reread
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
                    if (!checkRemaining(1)) {
                        return charAt;
                    }
                }

                checkLenient();
                char peek = source.charAt(pos);
                switch (peek) {
                    case '*':
                        // skip a /* c-style comment */
                        pos++;
                        if (!skipComment()) {
                            throw syntaxError("Unterminated comment");
                        }
                        localPos = pos + 2;
                        continue;

                    case '/':
                        // skip a // end-of-line comment
                        pos++;
                        skipToEndOfLine();
                        localPos = pos;
                        continue;

                    default:
                        return charAt;
                }
            } else if (charAt == COMMENT_HASH_CHAR) {
                pos = localPos;
                /*
                 * Skip a # hash end-of-line comment. The JSON RFC doesn't
                 * specify this behaviour, but it's required to parse
                 * existing documents. See http://b/2571423.
                 */
                checkLenient();
                skipToEndOfLine();
                localPos = pos;
            } else {
                pos = localPos;
                return charAt;
            }
        }

        return null;
    }

    protected void checkLenient() throws MalformedJsonException {
        if (getStrictness() != Strictness.LENIENT) {
            throw syntaxError("Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON");
        }
    }

    /**
     * Advances the position until after the next newline character. If the line is terminated by
     * "\r\n", the '\n' must be consumed as whitespace by the caller.
     */
    protected void skipToEndOfLine() {
        while (checkRemaining(1)) {
            final char charAt = source.charAt(pos++);
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
        for (; checkRemaining(2); pos++) {
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

    protected String locationString() {
        int line = lineNumber + 1;
        int column = pos - lineStart + 1;
        return " at line " + line + " column " + column + " path " + getPath();
    }

    protected @NotNull String getPath(final boolean usePreviousPath) {
        final @NotNull StringBuilder result = new StringBuilder().append('$');

        for (int i = 0; i < stackSize; i++) {
            final @NotNull JsonScope scope = stack[i];
            switch (scope) {
                case JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY -> {
                    int pathIndex = pathIndices[i];
                    // If index is last path element it points to next array element; have to decrement
                    if (usePreviousPath && pathIndex > 0 && i == stackSize - 1) {
                        pathIndex--;
                    }
                    result.append(ARRAY_OPEN_CHAR).append(pathIndex).append(ARRAY_CLOSE_CHAR);
                }
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

    @Override
    public String getPath() {
        return getPath(false);
    }

    @Override
    public String getPreviousPath() {
        return getPath(true);
    }

    /**
     * @return the position of the last chat that got read. Will start with 0,
     * and end with String length -1.
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Unescapes the character identified by the character or characters that immediately follow a
     * backslash. The backslash '\' should have already been read. This supports both Unicode escapes
     * "u000A" and two-character escapes "\n".
     *
     * @throws MalformedJsonException if the escape sequence is malformed
     */
    protected char readEscapeCharacter() throws IOException {
        if (pos == sourceLength) {
            throw syntaxError("Unterminated escape sequence");
        }

        char escaped = source.charAt(pos++);
        switch (escaped) {
            case 'u':
                if (!checkRemaining(4)) {
                    throw syntaxError("Unterminated escape sequence");
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
                        throw syntaxError("Malformed Unicode escape \\u" + source.substring(pos, pos + 4));
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
                if (getStrictness() == Strictness.STRICT) {
                    throw syntaxError("Cannot escape a newline character in strict mode");
                }
                lineNumber++;
                lineStart = pos;
                // fall-through

            case SINGLE_QUOTE_CHAR:
                if (getStrictness() == Strictness.STRICT) {
                    throw syntaxError("Invalid escaped character \"'\" in strict mode");
                }
            case DOUBLE_QUOTE_CHAR, '\\', '/':
                return escaped;
            default:
                // throw error when none of the above cases are matched
                throw syntaxError("Invalid escape sequence");
        }
    }

    /**
     * Throws a new {@link MalformedJsonException} with the given message and information about the
     * current location.
     */
    protected MalformedJsonException syntaxError(String message) throws MalformedJsonException {
        throw new MalformedJsonException(
            message + locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json"));
    }

    protected IllegalStateException unexpectedTokenError(String expected) throws IOException {
        JsonToken peeked = peek();
        String troubleshootingId =
            peeked == JsonToken.NULL ? "adapter-not-null-safe" : "unexpected-json-structure";
        return new IllegalStateException(
            "Expected "
                + expected
                + " but was "
                + peek()
                + locationString()
                + "\nSee "
                + TroubleshootingGuide.createUrl(troubleshootingId));
    }

    static {
        JsonReaderInternalAccess.INSTANCE =
            new JsonReaderInternalAccess() {
                private final JsonReaderInternalAccess oldInstance = JsonReaderInternalAccess.INSTANCE;

                @Override
                public void promoteNameToValue(JsonReader reader) throws IOException {
                    if (reader instanceof StringJsonReader stringJsonReader) {
                        @NotNull PeekedType peekedType = stringJsonReader.peeked;
                        if (peekedType == PeekedType.PEEKED_NONE) {
                            peekedType = stringJsonReader.doPeek(false);
                        }
                        if (peekedType == PeekedType.PEEKED_DOUBLE_QUOTED_NAME) {
                            stringJsonReader.peeked = PeekedType.PEEKED_DOUBLE_QUOTED;
                        } else if (peekedType == PeekedType.PEEKED_SINGLE_QUOTED_NAME) {
                            stringJsonReader.peeked = PeekedType.PEEKED_SINGLE_QUOTED;
                        } else if (peekedType == PeekedType.PEEKED_UNQUOTED_NAME) {
                            stringJsonReader.peeked = PeekedType.PEEKED_UNQUOTED;
                        } else {
                            throw stringJsonReader.unexpectedTokenError("a name");
                        }
                    } else {
                        oldInstance.promoteNameToValue(reader);
                    }
                }
            };
    }

    protected enum PeekedType {
        PEEKED_NONE,
        PEEKED_BEGIN_OBJECT,
        PEEKED_END_OBJECT,
        PEEKED_BEGIN_ARRAY,
        PEEKED_END_ARRAY,
        PEEKED_TRUE,
        PEEKED_FALSE,
        PEEKED_NULL,
        PEEKED_SINGLE_QUOTED,
        PEEKED_DOUBLE_QUOTED,
        PEEKED_UNQUOTED,

        /**
         * When this is returned, the string value is stored in peekedString.
         */
        PEEKED_BUFFERED,

        PEEKED_SINGLE_QUOTED_NAME,
        PEEKED_DOUBLE_QUOTED_NAME,
        PEEKED_UNQUOTED_NAME,

        /**
         * When this is returned, the integer value is stored in peekedLong.
         */
        PEEKED_LONG,

        PEEKED_NUMBER,
        PEEKED_EOF,

        PEEKED_DANGLING_NAME,
        PEEKED_NAME_VALUE_SEPARATOR,
    }

    public enum StructureJsonToken {

        /**
         * The opening of a JSON array. Written using {@link JsonWriter#beginArray} and read using {@link
         * JsonReader#beginArray}.
         */
        BEGIN_ARRAY,

        /**
         * The closing of a JSON array. Written using {@link JsonWriter#endArray} and read using {@link
         * JsonReader#endArray}.
         */
        END_ARRAY,

        /**
         * The opening of a JSON object. Written using {@link JsonWriter#beginObject} and read using
         * {@link JsonReader#beginObject}.
         */
        BEGIN_OBJECT,

        /**
         * The closing of a JSON object. Written using {@link JsonWriter#endObject} and read using {@link
         * JsonReader#endObject}.
         */
        END_OBJECT,

        /**
         * A JSON property name. Within objects, tokens alternate between names and their values. Written
         * using {@link JsonWriter#name} and read using {@link JsonReader#nextName}
         */
        QUOTED_NAME,

        /**
         * A JSON property name. Within objects, tokens alternate between names and their values. Written
         * using {@link JsonWriter#name} and read using {@link JsonReader#nextName}
         */
        UNQUOTED_NAME,

        QUOTED_VALUE,

        UNQUOTED_VALUE,

        /**
         * A JSON {@code null}.
         */
        NULL,

        /**
         * The end of the JSON stream. This sentinel value is returned by {@link JsonReader#peek()} to
         * signal that the JSON-encoded value has no more tokens.
         */
        END_DOCUMENT,

        DANGLING_NAME,
        NAME_VALUE_SEPARATOR,
    }

    protected enum JsonScope {
        /**
         * An array with no elements requires no separator before the next element.
         */
        EMPTY_ARRAY,

        /**
         * An array with at least one value requires a separator before the next element.
         */
        NONEMPTY_ARRAY,

        /**
         * An object with no name/value pairs requires no separator before the next element.
         */
        EMPTY_OBJECT,

        /**
         * An object whose most recent element is a key. The next element must be a value.
         */
        DANGLING_NAME,

        NAME_VALUE_SEPARATOR,

        /**
         * An object with at least one name/value pair requires a separator before the next element.
         */
        NONEMPTY_OBJECT,

        /**
         * No top-level value has been started yet.
         */
        EMPTY_DOCUMENT,

        /**
         * A top-level value has already been started.
         */
        NONEMPTY_DOCUMENT,

        /**
         * A document that's been closed and cannot be accessed.
         */
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
}