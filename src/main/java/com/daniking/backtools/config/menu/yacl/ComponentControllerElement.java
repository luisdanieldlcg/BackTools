package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.Either;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonToken;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentControllerElement extends AbstractDropdownControllerElement<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>, String> {
    protected static final Pattern CLOSED_CURLY_BRACKET_PATTERN = Pattern.compile("}\\s*$");
    protected static final char OPEN_CURLY_BRACKET = '{';
    protected static final String CLOSED_CURLY_BRACKET = "}";
    protected static final char COMMA = ',';
    protected static final char COLON = ':';
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
        jsonReader.setStrictness(Strictness.LENIENT);

        try {
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                jsonReader.beginObject();

                final boolean isClosed = CLOSED_CURLY_BRACKET_PATTERN.matcher(inputField).find();
                final @NotNull Set<@NotNull ComponentType<?>> alreadyAddedComponents = new ReferenceArraySet<>();
                int posBefore = jsonReader.getPosition();

                while (jsonReader.peek() == JsonToken.NAME) {
                    posBefore = jsonReader.getPosition();

                    final boolean isQuoted = jsonReader.structurePeek() == StringJsonReader.StructureJsonToken.QUOTED_NAME;
                    final @NotNull Either<String, String> nameFetchEither = jsonReader.tryNextName();

                    if (nameFetchEither.isLeft()) {
                        return List.of(inputField.substring(0, caretPos) + "\"" + inputField.substring(caretPos)); // the rest of the json is invalid
                    }

                    final @NotNull String componentTypeStr = nameFetchEither.getRight();
                    final @Nullable ComponentType<?> componentType;

                    if (componentTypeStr.startsWith(EXCLAMATION_MARK)) {
                        componentType = readComponentType(componentTypeStr.substring(1));

                        if  (componentType != null) {
                            if (alreadyAddedComponents.add(componentType)) {
                                posBefore = jsonReader.getPosition();

                                final StringJsonReader.StructureJsonToken structureJsonToken = jsonReader.structurePeek();
                                if (posBefore <= caretPos && caretPos < jsonReader.getPosition() &&
                                    structureJsonToken == StringJsonReader.StructureJsonToken.DANGLING_NAME) {
                                    return List.of(inputField.substring(0, caretPos) + ":{}" + inputField.substring(caretPos));
                                }

                                jsonReader.skipValue();
                            } else {
                                // throw REPEATED_COMPONENT_EXCEPTION.create(componentType);
                            }
                        } else {
                            if (caretPos == jsonReader.getPosition()) {
                                final @NotNull List<@NotNull String> suggestedComponents = suggestComponents(EXCLAMATION_MARK, componentTypeStr.substring(1),
                                    alreadyAddedComponents);
                                final @NotNull List<@NotNull String> result = new ArrayList<>();

                                if (caretPos < inputField.length()) {
                                    for (String componentName : suggestedComponents) {
                                        result.add(inputField.substring(0, posBefore) + componentName + ":{}" + inputField.substring(caretPos + 1));
                                    }
                                } else {
                                    for (String componentName : suggestedComponents) {
                                        result.add(inputField.substring(0, posBefore) + componentName + ":{}");
                                    }
                                }

                                return result;
                            }
                        }
                    } else {
                        componentType = readComponentType(componentTypeStr);

                        if  (componentType != null) {
                            if (alreadyAddedComponents.add(componentType)) {
                                posBefore = jsonReader.getPosition();

                                final StringJsonReader.StructureJsonToken structureJsonToken = jsonReader.structurePeek();
                                if (posBefore <= caretPos && caretPos < jsonReader.getPosition() &&
                                    structureJsonToken == StringJsonReader.StructureJsonToken.DANGLING_NAME) {
                                    return List.of(inputField.substring(0, caretPos) + COLON + inputField.substring(caretPos));
                                }

                                posBefore = jsonReader.getPosition();
                                switch (jsonReader.structurePeek()) {
                                    case QUOTED_VALUE,
                                         BEGIN_ARRAY,
                                         BEGIN_OBJECT,
                                         UNQUOTED_VALUE, NULL-> { // todo currently we have no assistance in creating component values
                                        try {
                                            final @Nullable Object componentValue = readComponentValue(jsonReader, componentType);

                                            if (componentValue != null) {
                                                // do stuff with value here
                                            }

                                            if (caretPos == jsonReader.getPosition()) {
                                                final @NotNull List<@NotNull String> suggestions = new ArrayList<>();

                                                if (inputField.length() > caretPos) {
                                                    // Handle case when the caret is within the text
                                                    suggestions.add(inputField.substring(0, caretPos) + COMMA + inputField.substring(caretPos));
                                                    if (!isClosed) {
                                                        BackTools.LOGGER.info("1?");
                                                        suggestions.add(inputField.substring(0, caretPos) + CLOSED_CURLY_BRACKET + inputField.substring(caretPos));
                                                    }
                                                } else {
                                                    // Handle case when the caret is at the end
                                                    suggestions.add(inputField + COMMA);
                                                    if (!isClosed) {
                                                        BackTools.LOGGER.info("2?");

                                                        suggestions.add(inputField + CLOSED_CURLY_BRACKET);
                                                    }
                                                }

                                                return suggestions;
                                            }
                                        } catch (final @NotNull JsonParseException ignored) { // invalid json, can't parse further
                                            return Collections.emptyList();
                                        }
                                    }
                                    case END_DOCUMENT, // no closing curly bracket, no value
                                         END_OBJECT, QUOTED_NAME, UNQUOTED_NAME, // no value
                                         END_ARRAY /* unexpected */ -> {
                                             if (caretPos == posBefore) {
                                                 return Collections.emptyList();
                                             }
                                    }
                                }
                            } else {
                                // throw REPEATED_COMPONENT_EXCEPTION.create(componentType);
                            }
                        } else {
                            if ((isQuoted ? caretPos + 1 : caretPos) == jsonReader.getPosition()) {
                                final @NotNull List<@NotNull String> suggestedComponents = suggestComponents("", componentTypeStr, alreadyAddedComponents);
                                final @NotNull List<@NotNull String> result = new ArrayList<>();

                                if (isQuoted || inputField.length() > caretPos) {
                                    for (String componentName : suggestedComponents) {
                                        result.add(inputField.substring(0, posBefore) + componentName + inputField.substring(caretPos + 1));
                                    }
                                } else {
                                    for (String componentName : suggestedComponents) {
                                        result.add(inputField.substring(0, posBefore) + componentName);
                                    }
                                }

                                return result;
                            }
                        }
                    }

                    posBefore = jsonReader.getPosition();
                }

                // before:
                // caretPos: 0, jsonReader.getPosition(): 2, jsonReader.structurePeek(): END_OBJECT, posBefore: 0

                // in:
                // aretPos: 1, jsonReader.getPosition(): 2, jsonReader.structurePeek(): END_OBJECT, posBefore: 0

                // after:
                // caretPos: 2, jsonReader.getPosition(): 2, jsonReader.structurePeek(): END_OBJECT, posBefore: 0


                BackTools.LOGGER.info("caretPos: " + caretPos + ", jsonReader.getPosition(): " + jsonReader.getPosition() + ", jsonReader.structurePeek(): " + jsonReader.structurePeek() + ", posBefore: " + posBefore + ", posAfter: " + jsonReader.getPosition());

                final @NotNull List<@NotNull String> result = new ArrayList<>();
//                final boolean anyComponentsAdded = !alreadyAddedComponents.isEmpty();

                // Handle positive components
//                addComponentsToResult(
//                    result,
//                    suggestComponents("", "", alreadyAddedComponents),
//                    anyComponentsAdded,
//                    caretPos,
//                    ""
//                );
//
//                // Handle negative components
//                addComponentsToResult(
//                    result,
//                    suggestComponents("!", "", alreadyAddedComponents),
//                    anyComponentsAdded,
//                    caretPos,
//                    ":{}"
//                );

                BackTools.LOGGER.info("isClosed? " + isClosed + ", length: " + inputField.replaceAll("\\s+$", "").length() + ", caretPos: " + caretPos);
                if (!isClosed && inputField.replaceAll("\\s+$", "").length() == caretPos) { // ignore tailing whitespace
                    BackTools.LOGGER.info("3?");
                    result.add(inputField.substring(0, caretPos) + CLOSED_CURLY_BRACKET);
                }

                return result;
            } else {
                return List.of(OPEN_CURLY_BRACKET + inputField);
            }
        } catch (final @NotNull IOException ignored) {
            BackTools.LOGGER.info("Error parsing JSON: ", ignored);
        }

        return Collections.emptyList();
    }

    private void addComponentsToResult (
        final @NotNull List <@NotNull String> result,
        final @NotNull List<@NotNull String> componentTypesToAdd,
        final boolean insertComma,
        final int pos,
        final @NotNull String suffix
    ){
        boolean hasRemainingInput = inputField.length() > pos;
        String comma = insertComma ? String.valueOf(COMMA) : "";
        String remaining = hasRemainingInput ? inputField.substring(pos) : "";

        BackTools.LOGGER.info("adding components to result: " + componentTypesToAdd.size() + ", insertComma: " + insertComma + ", suffix: " + suffix + ", remaining: " + remaining + ", pos: " + pos);

        for (String componentName : componentTypesToAdd) {
            result.add(
                inputField.substring(0, pos) +
                comma + componentName + suffix +
                remaining
            );
        }
    }

    protected List<String> suggestComponents(final @NotNull String prefix, final @NotNull String componentTypePart,
                                                           final @NotNull Set<ComponentType<?>> alreadyAdded) {
        final @NotNull List<@NotNull String> result = new ArrayList<>();

        CommandSource.forEachMatching(Registries.DATA_COMPONENT_TYPE.getEntrySet(), componentTypePart, (entry) -> (entry.getKey()).getValue(),
            entry -> {
                ComponentType<?> componentType = entry.getValue();

                if (!alreadyAdded.contains(componentType) && componentType.getCodec() != null) {
                    final @NotNull Identifier identifier = entry.getKey().getValue();

                    if (identifier.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                        result.add(prefix + identifier.getPath());
                    }

                    result.add('"' + prefix + identifier + '"'); // always quote namespaced identifiers since the colon would get mistaken as value separator in JSON otherwise
                }
            });

        return result;
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

    private <T> @Nullable T readComponentValue(final @NotNull StringJsonReader jsonReader, final @NotNull ComponentType<T> type) throws JsonParseException {
        final @NotNull JsonElement jsonElement = JsonParser.parseReader(jsonReader);
        final @Nullable Codec<T> valueCodec = type.getCodec();

        if (valueCodec != null) {
            final DataResult<Pair<T, JsonElement>> dataResult = valueCodec.decode(BackTools.getConfigHandler().getDynamicJSONOps(), jsonElement);

            if (dataResult.isSuccess()) {
                return dataResult.getOrThrow().getFirst();
            } else {
                BackTools.LOGGER.debug(dataResult.error().get().message());
            }
        }

        return null;
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

                    BackTools.LOGGER.info("combindedWidth: " + combindedWidth + ", maxWidth: " + maxWidth + ", left: " + left  +" (" + ((double)renderedLeftLength / combindedWidth) + "), right: " + right + " (" + ((double)renderedRightLength / combindedWidth) + ")" );

                    if ((double) renderedLeftLength / combindedWidth >= 0.8) {
                        BackTools.LOGGER.info("Left string is too long: " + left);
                        left = left.substring(Math.min(left.length(), 1 + (ellipsis.length() + 1)));
                        left = ellipsis + left;
                    }

                    if ((double) renderedRightLength / combindedWidth >= 0.2) {
                        BackTools.LOGGER.info("right string is too long: " + right);
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
}
