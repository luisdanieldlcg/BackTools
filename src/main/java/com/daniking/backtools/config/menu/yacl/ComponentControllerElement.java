package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.utils.Either;
import com.daniking.backtools.utils.StringJsonReader;
import com.daniking.backtools.utils.StringJsonReader.PeekStatus;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// todo multiline
public class ComponentControllerElement extends AbstractDropdownControllerElement<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>, String> {
    protected static final Pattern CLOSED_CURLY_BRACKET_PATTERN = Pattern.compile(StringJsonReader.CLOSE_OBJECT_CHAR + "\\s*$");
    protected static final String EXCLAMATION_MARK = "!";

    public ComponentControllerElement(final @NotNull ComponentController control,
                                      final @NotNull YACLScreen screen,
                                      final @NotNull Dimension<@NotNull Integer> dim) {
        super(control, screen, dim);
    }

    // Restore original StringControllerElement-behavior here,
    // we do NOT want to call computeMatchingValues like super yet.
    // The reason behind this is that we need the updated caretPos.
    @Override
    public boolean modifyInput(Consumer<StringBuilder> consumer) {
        StringBuilder temp = new StringBuilder(inputField);
        consumer.accept(temp);
        if (!control.isInputValid(temp.toString()))
            return false;
        inputField = temp.toString();
        if (instantApply)
            updateControl();
        return true;
    }

    @Override
    protected void doBackspace() {
        if (selectionLength != 0) {
            write("");
        } else if (caretPos > 0) {
            if (modifyInput(builder -> builder.deleteCharAt(caretPos - 1))) {
                caretPos--;
                matchingValues = computeMatchingValues(); // moved this call after the caretPos update
                checkRenderOffset();
            }
        }
        updateUndoHistory();
    }

    @Override
    protected void doDelete() {
        if (selectionLength != 0) {
            write("");
        } else if (caretPos < inputField.length()) {
            modifyInput(builder -> builder.deleteCharAt(caretPos));
            matchingValues = computeMatchingValues();
        }
        updateUndoHistory();
    }

    @Override
    public void write(String string) {
        if (selectionLength == 0) {
            if (modifyInput(builder -> builder.insert(caretPos, string))) {
                caretPos += string.length();
                matchingValues = computeMatchingValues(); // moved this call after the caretPos update
                checkRenderOffset();
            }
        } else {
            int start = getSelectionStart();
            int end = getSelectionEnd();

            if (modifyInput(builder -> builder.replace(start, end, string))) {
                caretPos = start + string.length();
                matchingValues = computeMatchingValues(); // moved this call after the caretPos update
                selectionLength = 0;
                checkRenderOffset();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        final boolean success = super.keyPressed(keyCode, scanCode, modifiers);

        // since our matching values depend on the caretPos, we need to update them when the caret moves
        if (success && (
            keyCode == InputUtil.GLFW_KEY_LEFT ||
            keyCode == InputUtil.GLFW_KEY_RIGHT ||
            keyCode == InputUtil.GLFW_KEY_END ||
            keyCode == InputUtil.GLFW_KEY_HOME)) {
            matchingValues = computeMatchingValues();
        }

        return success;
    }

    @Override
    protected int getDefaultCaretPos() {
        Matcher matcher = CLOSED_CURLY_BRACKET_PATTERN.matcher(inputField);
        if (matcher.matches()) {
            return matcher.start();
        }

        return super.getDefaultCaretPos();
    }

    @Override
    public @NotNull List<@NotNull String> computeMatchingValues() {
        if (inputField.length() < caretPos || !dropdownVisible) { // First: invalid state, may happen when resizing the window, second: at start
            return Collections.emptyList();
        }

        final @NotNull StringJsonReader jsonReader = new StringJsonReader(inputField);

        if (jsonReader.tryBeginObject()) {
            boolean isClosed = CLOSED_CURLY_BRACKET_PATTERN.matcher(inputField).find(); // first estimate may be proven wrong later
            final @NotNull Set<@NotNull ComponentType<?>> alreadyAddedComponents = new ReferenceArraySet<>();

            @NotNull PeekStatus peekStatus = jsonReader.doPeek(true);
            while ( // note: no dangling name here. dangling name means we have already seen the end of the object
                peekStatus == PeekStatus.SINGLE_QUOTED_NAME ||
                peekStatus == PeekStatus.DOUBLE_QUOTED_NAME ||
                peekStatus == PeekStatus.UNQUOTED_NAME) {

                final boolean isQuoted = peekStatus == PeekStatus.SINGLE_QUOTED_NAME || peekStatus == PeekStatus.DOUBLE_QUOTED_NAME;
                final @NotNull Either<@Nullable String, @NotNull String> nameFetchEither = jsonReader.tryNextName();

                // note: because we don't know where this name ends, we can't make any suggestions until we got a closing quote char
                if (nameFetchEither.isLeft()) {
                    return List.of(inputField.substring(0, caretPos) + StringJsonReader.DOUBLE_QUOTE_CHAR + inputField.substring(caretPos)); // the rest of the json is invalid
                }

                final @NotNull String componentTypeStr = nameFetchEither.getRight();
                final @Nullable ComponentType<?> componentType;

                if (componentTypeStr.startsWith(EXCLAMATION_MARK)) {
                    componentType = readComponentType(componentTypeStr.substring(1));

                    if (componentType != null) {
                        if (alreadyAddedComponents.add(componentType)) {

                            peekStatus = jsonReader.doPeek(true);
                            if (jsonReader.getPreviousPos() <= caretPos && caretPos < jsonReader.getNextPosition() &&
                                peekStatus == PeekStatus.DANGLING_NAME) {
                                return List.of(inputField.substring(0, caretPos) + ":{}" + inputField.substring(caretPos));
                            }

                            jsonReader.skipValue();
                        } else {
                            return Collections.emptyList(); // don't recommend anything for repeated values, they have undefined behavior
                        }
                    } else {
                        if (caretPos == jsonReader.getNextPosition()) {
                            return addComponentsToResult(componentTypeStr.substring(1), alreadyAddedComponents, jsonReader.getPreviousPos(), isQuoted ? caretPos : caretPos + 1, isQuoted, false, ComponentStatus.REMOVE);
                        }
                    }
                } else {
                    componentType = readComponentType(componentTypeStr);

                    if  (componentType != null) {
                        if (alreadyAddedComponents.add(componentType)) {
                            peekStatus = jsonReader.doPeek(true);

                            if (jsonReader.getPreviousPos() <= caretPos && caretPos <= jsonReader.getNextPosition() &&
                                peekStatus == PeekStatus.DANGLING_NAME) {
                                return List.of(inputField.substring(0, caretPos) + StringJsonReader.KEY_VALUE_SEPARATOR + inputField.substring(caretPos));
                            }

                            peekStatus = jsonReader.doPeek(true);
                            switch (peekStatus) {
                                case SINGLE_QUOTED_VALUE, DOUBLE_QUOTED_VALUE,
                                     BEGIN_ARRAY,
                                     BEGIN_OBJECT,
                                     UNQUOTED_VALUE, NULL-> { // todo currently we have no assistance in creating component values
                                    final @NotNull Either <Boolean, ?> componentValue = readComponentValue(jsonReader, componentType);

                                    if (componentValue.isLeft()) {
                                        if (componentValue.getLeft()) {
                                            // invalid json, can't parse further
                                            return Collections.emptyList();
                                        }
                                    } else {
                                        // do stuff with value here
                                    }

                                    if (caretPos == jsonReader.getNextPosition()) {
                                        final @NotNull List<@NotNull String> suggestions = new ArrayList<>();

                                        if (inputField.length() > caretPos) {
                                            // Handle case when the caret is within the text
                                            suggestions.add(inputField.substring(0, caretPos) + StringJsonReader.COMMA_CHAR + inputField.substring(caretPos));
                                            if (!isClosed) {
                                                suggestions.add(inputField.substring(0, caretPos) + StringJsonReader.CLOSE_OBJECT_CHAR + inputField.substring(caretPos));
                                            }
                                        } else {
                                            // Handle case when the caret is at the end
                                            suggestions.add(inputField + StringJsonReader.COMMA_CHAR);
                                            if (!isClosed) {
                                                suggestions.add(inputField + StringJsonReader.CLOSE_OBJECT_CHAR);
                                            }
                                        }

                                        return suggestions;
                                    }
                                }
                                case END_DOCUMENT, // no closing curly bracket, no value
                                     END_OBJECT, SINGLE_QUOTED_NAME, DOUBLE_QUOTED_NAME, UNQUOTED_NAME, // no value
                                     END_ARRAY /* unexpected */ -> {
                                         if (caretPos == jsonReader.getPreviousPos()) {
                                             return Collections.emptyList();
                                         }
                                }
                            }
                        } else {
                            return Collections.emptyList(); // don't recommend anything for repeated values, they have undefined behavior
                        }
                    } else {
                        if ((isQuoted ? caretPos + 1 : caretPos) == jsonReader.getNextPosition()) {
                            return addComponentsToResult(componentTypeStr, alreadyAddedComponents, jsonReader.getPreviousPos(), isQuoted ? caretPos : caretPos + 1, isQuoted, false, ComponentStatus.ADD);
                        }
                    }
                }

                peekStatus = jsonReader.doPeek(true);
            }


            final @NotNull List<@NotNull String> result;
            switch (jsonReader.doPeek(false)) {
                case END_OBJECT,
                     INVALID_UNTERMINATED_OBJECT,
                     INVALID_MISSING_NAME,
                     END_DOCUMENT -> {
                    result = new ArrayList<>(
                        addComponentsToResult("", alreadyAddedComponents, jsonReader.getPreviousPos(), jsonReader.getPreviousPos(),false, !alreadyAddedComponents.isEmpty(), ComponentStatus.BOTH)
                    );
                }

                default -> result = new ArrayList<>();
            }


            if (!jsonReader.tryEndObject()) {
                switch (jsonReader.doPeek(false)) {
                    case INVALID_UNKNOWN,
                         //INVALID_MISSING_NAME,
                         INVALID_UNTERMINATED_ARRAY,
                         INVALID_UNTERMINATED_OBJECT -> {
                        // ignore invalid json, don't try to close it
                        return Collections.emptyList();
                    }
                    default -> {
                        if (inputField.replaceAll("\\s+$", "").length() == caretPos) { // ignore tailing whitespace
                            result.add(inputField.substring(0, caretPos) + StringJsonReader.CLOSE_OBJECT_CHAR);
                        }
                    }
                }
            }

            return result;
        } else {
            return List.of(StringJsonReader.OPEN_OBJECT_CHAR + inputField);
        }
    }

    protected List<String> addComponentsToResult(final @NotNull String componentTypePart,
                                                 final @NotNull Set<ComponentType<?>> alreadyAdded,
                                                 final @Range(from = 0, to = Integer.MAX_VALUE) int startPos,
                                                 final @Range(from = -1, to = Integer.MAX_VALUE) int remainingPos,
                                                 final boolean alreadyQuoted, final boolean insertComma,
                                                 final @NotNull ComponentStatus componentStatus) {
        final @NotNull List<@NotNull String> componentTypesToAdd = new ArrayList<>();

        final @Nullable String remaining = inputField.length() > remainingPos ? inputField.substring(remainingPos) : null;
        final String substringBefore = inputField.substring(0, startPos);


        CommandSource.forEachMatching(Registries.DATA_COMPONENT_TYPE.getEntrySet(), componentTypePart, (entry) -> (entry.getKey()).getValue(),
            entry -> {
                ComponentType<?> componentType = entry.getValue();

                if (!alreadyAdded.contains(componentType) && !componentType.shouldSkipSerialization()) {
                    final @NotNull Identifier identifier = entry.getKey().getValue();
                    final @NotNull StringBuilder builder = new StringBuilder();

                    if (identifier.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) && !componentTypePart.startsWith(Identifier.DEFAULT_NAMESPACE)) {
                        // Handle ADD or BOTH statuses
                        if (componentStatus != ComponentStatus.REMOVE) {
                            builder.append(substringBefore);
                            if (insertComma) {
                                builder.append(StringJsonReader.COMMA_CHAR);
                            }

                            if (!alreadyQuoted) {
                                builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                            }
                            builder.append(identifier.getPath());
                            if (!alreadyQuoted) {
                                builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                            }

                            if (remaining != null) {
                                builder.append(remaining);
                            }

                            componentTypesToAdd.add(builder.toString());
                        }

                        // Handle REMOVE or BOTH statuses
                        if (componentStatus != ComponentStatus.ADD) {
                            builder.setLength(0);  // Reset StringBuilder for new string
                            builder.append(substringBefore);
                            if (insertComma) {
                                builder.append(StringJsonReader.COMMA_CHAR);
                            }

                            if (!alreadyQuoted) {
                                builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                            }
                            builder.append(EXCLAMATION_MARK);
                            builder.append(identifier.getPath());
                            if (!alreadyQuoted) {
                                builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                            }

                            builder.append(":{}");

                            if (remaining != null) {
                                builder.append(remaining);
                            }

                            componentTypesToAdd.add(builder.toString());
                        }
                    }

                    // Handle ADD or BOTH statuses (outside of namespace condition)
                    if (componentStatus != ComponentStatus.REMOVE) {
                        builder.setLength(0);  // Reset StringBuilder for new string
                        builder.append(substringBefore);
                        if (insertComma) {
                            builder.append(StringJsonReader.COMMA_CHAR);
                        }

                        if (!alreadyQuoted) {
                            builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                        }

                        builder.append(identifier).append(StringJsonReader.DOUBLE_QUOTE_CHAR);

                        if (remaining != null) {
                            builder.append(remaining);
                        }

                        componentTypesToAdd.add(builder.toString());
                    }

                    // Handle REMOVE or BOTH statuses (outside of namespace condition)
                    if (componentStatus != ComponentStatus.ADD) {
                        builder.setLength(0);  // Reset StringBuilder for new string
                        builder.append(substringBefore);

                        if (insertComma) {
                            builder.append(StringJsonReader.COMMA_CHAR);
                        }

                        if (!alreadyQuoted) {
                            builder.append(StringJsonReader.DOUBLE_QUOTE_CHAR);
                        }

                        builder.append(EXCLAMATION_MARK).append(identifier).append(StringJsonReader.DOUBLE_QUOTE_CHAR).append(":{}");

                        if (remaining != null) {
                            builder.append(remaining);
                        }

                        componentTypesToAdd.add(builder.toString());
                    }
                }
            });

        return componentTypesToAdd;
    }

    protected static @Nullable ComponentType<?> readComponentType(final @NotNull String componentTypeStr) {
        final @Nullable Identifier identifier = Identifier.tryParse(componentTypeStr);

        if (identifier != null) {
            ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(identifier);

            if (componentType != null && !componentType.shouldSkipSerialization()) {
                return componentType;
            }
        }

        return null;
    }

    private <T> Either<@NotNull Boolean, T> readComponentValue(final @NotNull StringJsonReader jsonReader,
                                               final @NotNull ComponentType<T> type) {
        try {
            final @Nullable JsonElement jsonElement = jsonReader.readJsonObject();

            if (jsonElement != null) {
                final @Nullable Codec<T> valueCodec = type.getCodec();

                if (valueCodec != null) {
                    final DataResult<Pair<T, JsonElement>> dataResult = valueCodec.decode(BackTools.getConfigHandler().getDynamicJSONOps(), jsonElement);

                    if (dataResult.isSuccess()) {
                        return Either.right(dataResult.getOrThrow().getFirst());
                    } else {
                        BackTools.LOGGER.debug("Got error when decoding '{}' for ComponentType '{}': {}", jsonElement, type, dataResult.error().get().message());
                        return Either.left(false);
                    }
                } else {
                    BackTools.LOGGER.debug("could not decode '{}', because no codec for ComponentType '{}' could be found", jsonElement, type);
                    return Either.left(false);
                }
            } else {
                BackTools.LOGGER.debug("could not read '{}' at {} as json object", jsonReader, jsonReader.getPreviousPos());

                return Either.left(true);
            }
        } catch (final @NotNull JsonParseException parseException) {
            BackTools.LOGGER.debug("could not read '{}' as json object", jsonReader, parseException);

            return Either.left(true);
        }
    }

    @Override
    public String getString(final String string) {
        return string;
    }

    @Override
    public @NotNull Text shortenString(@NotNull String string) {
        if (string.isEmpty()) {
            return Text.literal(string);
        } else {
            final @NotNull String ellipsis = "...";
            final int maxWidth = this.getDimension().width() - 20;

            if (textRenderer.getWidth(ellipsis + ellipsis) >= maxWidth) {
                return Text.literal(ellipsis);
            }

            if (textRenderer.getWidth(string) <= maxWidth) {
                return Text.literal(string);
            } else if (string.length() > caretPos) {

                String left = string.substring(0, caretPos);
                String right = string.substring(caretPos);
                int combindedWidth = textRenderer.getWidth(left + right);

                while (combindedWidth > maxWidth) {
                    int renderedLeftLength = textRenderer.getWidth(left);
                    int renderedRightLength = textRenderer.getWidth(right);

                  //  BackTools.LOGGER.info("combindedWidth: " + combindedWidth + ", maxWidth: " + maxWidth + ", left: " + left  +" (" + ((double)renderedLeftLength / combindedWidth) + "), right: " + right + " (" + ((double)renderedRightLength / combindedWidth) + ")" );

                    if ((double) renderedLeftLength / combindedWidth >= 0.8) {
                      //  BackTools.LOGGER.info("Left string is too long: " + left);
                        left = left.substring(Math.min(left.length(), 1 + (ellipsis.length() + 1)));
                        left = ellipsis + left;
                    }

                    if ((double) renderedRightLength / combindedWidth >= 0.2) {
                    //    BackTools.LOGGER.info("right string is too long: " + right);
                        right = right.substring(0, Math.max(right.length() - 1 - (ellipsis.length() + 1), 0));
                        right += ellipsis;
                    }

                    combindedWidth = textRenderer.getWidth(left + right);
                }

                return Text.literal(left + right);

            } else {
                return Text.literal(GuiUtils.shortenString(string, textRenderer, maxWidth, ellipsis));
            }
        }
    }

    protected enum ComponentStatus {
        REMOVE,
        BOTH,
        ADD
    }
}
