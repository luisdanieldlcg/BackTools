package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.utils.Either;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import net.minecraft.component.ComponentChanges;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentController extends AbstractDropdownController<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> {

    public ComponentController(Option<@NotNull Either<@NotNull JsonElement, @Nullable ComponentChanges>> option) {
        super(option);
    }

    @Override
    public @NotNull String getString() {
        return this.option.pendingValue().fold(
            JsonElement::toString,
            componentChanges -> ComponentChanges.CODEC.
                encodeStart(BackTools.getConfigHandler().getDynamicJSONOps(), componentChanges).
                result().
                map(JsonElement::toString).
                orElse("ERROR")
        );
    }

    @Override
    public void setFromString(final @NotNull String string) {
        try {
            final @NotNull JsonElement element = JsonParser.parseString(string);

            if (BackTools.getConfigHandler().canAccessDynamicRegistries()) {
                ComponentChanges.CODEC.decode(BackTools.getConfigHandler().getDynamicJSONOps(), element).
                    ifSuccess(succPair -> this.option.requestSet(Either.right(succPair.getFirst()))
                    ).ifError(errPair -> this.option.requestSet(Either.left(element)));
            } else {
                this.option.requestSet(Either.left(element));
            }
        } catch (JsonSyntaxException ignored) {
        }
    }

    @Override
    public boolean isValueValid(final @NotNull String value) { // todo better help for Component validation here
//        Registries.DATA_COMPONENT_TYPE
        try {
            JsonParser.parseString(value);

            return true;
        } catch (final @NotNull JsonSyntaxException ignored) {
            return false;
        }
    }

    @Override
    public AbstractWidget provideWidget(final @NotNull YACLScreen screen,
                                        final @NotNull Dimension<@NotNull Integer> widgetDimension) {
        return new ComponentControllerElement(this, screen, widgetDimension);
    }
}
