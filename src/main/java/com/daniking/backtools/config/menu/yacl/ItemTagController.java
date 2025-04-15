package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.ConfigHandler;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.utils.ItemRegistryHelper;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ItemTagController extends AbstractDropdownController<AItemLike> {
    public ItemTagController(Option<AItemLike> option) {
        super(option);
    }

    public String getString() {
        return this.option.pendingValue().toString();
    }

    public void setFromString(final @NotNull String value) {
        try {
            this.option.requestSet(BackTools.getConfigHandler().readAItemLike(value));
        } catch (CommandSyntaxException ignored) {
        }
    }

    public Text formatValue() {
        return Text.literal(this.getString());
    }

    public boolean isValueValid(final @NotNull String value) {
        // all tags are "valid", since they might depend on Context
        return ItemRegistryHelper.isRegisteredItem(value) || ConfigHandler.couldBeTag(value);
    }

    protected String getValidValue(final @NotNull String value, final int offset) {
        return BackTools.getConfigHandler().readAllFittingItems(value).stream().skip(offset).findFirst().map(AItemLike::toString).orElseGet(this::getString);
    }

    public @NotNull AbstractWidget provideWidget(final @NotNull YACLScreen screen, final @NotNull Dimension<Integer> widgetDimension) {
        return new ItemTagControllerElement(this, screen, widgetDimension);
    }
}
