package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.menu.ItemLikeTicker;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ItemTagControllerElement extends AbstractDropdownControllerElement<AItemLike, String> {
    protected @Nullable AItemLike currentItemLike = null;
    protected SequencedMap<String, @NotNull AItemLike> matchingItems = new LinkedHashMap<>();

    public ItemTagControllerElement(final @NotNull ItemTagController control, final @NotNull YACLScreen screen, final @NotNull Dimension<@NotNull Integer> dim) {
        super(control, screen, dim);
    }

    @Override
    protected void drawValueText(final @NotNull DrawContext graphics, final int mouseX, final int mouseY, final float delta) {
        ItemLikeTicker.getInstance().tryToTick();

        Dimension<Integer> oldDimension = this.getDimension();
        this.setDimension(this.getDimension().withWidth(this.getDimension().width() - this.getDecorationPadding()));
        super.drawValueText(graphics, mouseX, mouseY, delta);
        this.setDimension(oldDimension);

        if (this.currentItemLike != null) {
            if (currentItemLike.isInvalid()) {
                graphics.drawTextWithShadow(textRenderer, Text.literal("?"), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() / 2, this.getTextY(), Formatting.DARK_GRAY.getColorValue());
            } else {
                graphics.drawItemWithoutEntity(new ItemStack(currentItemLike.getDisplayItem()), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
            }
        }
    }

    private @NotNull SequencedMap<@NotNull String, @NotNull AItemLike> getMatchingItemIdentifiers(final @NotNull String value) {
        final @NotNull SequencedMap<@NotNull String, @NotNull AItemLike> result = new LinkedHashMap<>();

        // register the tickables in the ticker and map the identifiers to string.
        for (final @NotNull AItemLike aItemLike : BackTools.getConfigHandler().readAllFittingItems(value)) { // todo handle invalid tags here if the configHandler couldn't read the tag (since it may be still perfectly fine in another context!) <-- also render some sort of info (hover) text "Could not find in current context, make sure to load the depending datapack or join the server that defines this tag."
            switch (aItemLike) {
                case AItemLike.TagItemLike tagItemLike -> {
                    ItemLikeTicker.getInstance().startTicking(tagItemLike);

                    result.put(tagItemLike.toString(), tagItemLike);
                }
                case AItemLike.DirectItemLike directItemLike -> result.put(aItemLike.toString(), directItemLike);
                default -> {
                }
            }
        }

        return result;
    }

    @Override
    public @NotNull List<@NotNull String> computeMatchingValues() {
        matchingItems = getMatchingItemIdentifiers(this.inputField);

        if (matchingItems.isEmpty()) {
            this.currentItemLike = null;
        } else {
            this.currentItemLike = matchingItems.firstEntry().getValue();
        }

        return List.copyOf(matchingItems.keySet());
    }

    @Override
    protected void renderDropdownEntry(DrawContext graphics, Dimension<Integer> entryDimension, String identifier) {
        super.renderDropdownEntry(graphics, entryDimension, identifier);

        graphics.drawItemWithoutEntity(
            new ItemStack(this.matchingItems.get(identifier).getDisplayItem()),
            entryDimension.xLimit() - 2, entryDimension.y() + 1);
    }

    @Override
    public @NotNull String getString(final @NotNull String identifier) {
        return identifier;
    }

    @Override
    protected int getDecorationPadding() {
        return 16;
    }

    @Override
    protected int getDropdownEntryPadding() {
        return 4;
    }

    @Override
    protected int getControlWidth() {
        return super.getControlWidth() + this.getDecorationPadding();
    }

    @Override
    protected Text getValueText() {
        if (inputField.isEmpty() || control == null) {
            return super.getValueText();
        } else if (inputFieldFocused) {
            return Text.literal(inputField);
        } else {
            final AItemLike pendingValue = ((ItemTagController)control).option().pendingValue();

            if (pendingValue instanceof AItemLike.DirectItemLike) {
                return pendingValue.getDisplayItem().getName();
            } else {
                return Text.literal(pendingValue.toString());
            }
        }
    }
}
