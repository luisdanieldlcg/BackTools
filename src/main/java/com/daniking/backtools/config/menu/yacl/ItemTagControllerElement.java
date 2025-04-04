package com.daniking.backtools.config.menu.yacl;

import com.daniking.backtools.BackTools;
import com.daniking.backtools.config.AItemLike;
import com.daniking.backtools.config.menu.Ticker;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class ItemTagControllerElement extends AbstractDropdownControllerElement<AItemLike, String> {
    private final ItemTagController itemTagController;
    protected @Nullable AItemLike currentItemLike = null;
    protected SequencedMap<String, @NotNull AItemLike> matchingItems = new LinkedHashMap<>();
    private final @NotNull Ticker ticker = new Ticker(Duration.ofSeconds(2));

    public ItemTagControllerElement(final @NotNull ItemTagController control, final @NotNull YACLScreen screen, final @NotNull Dimension<@NotNull Integer> dim) {
        super(control, screen, dim);
        this.itemTagController = control;
    }

    @Override
    protected void drawValueText(final @NotNull DrawContext graphics, final int mouseX, final int mouseY, final float delta) {
        ticker.tryToTick();

        Dimension<Integer> oldDimension = this.getDimension();
        this.setDimension(this.getDimension().withWidth(this.getDimension().width() - this.getDecorationPadding()));
        super.drawValueText(graphics, mouseX, mouseY, delta);
        this.setDimension(oldDimension);

        if (this.currentItemLike != null) {
            if (currentItemLike.isInvalid()) {
                graphics.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(""), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2, 14737632);
            } else {
                graphics.drawItemWithoutEntity(new ItemStack(currentItemLike.getDisplayItem()), this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
            }
        }
    }

    private @NotNull SequencedMap<@NotNull String, @NotNull AItemLike> getMatchingItemIdentifiers(String value) {
        final @NotNull SequencedMap<@NotNull String, @NotNull AItemLike> result = new LinkedHashMap<>();

        // register the tickables in the ticker and map the identifiers to string.
        for (final @NotNull AItemLike aItemLike : BackTools.getConfigHandler().readAllFittingItems(value)) { // todo handle invalid tags here if the configHandler couldn't read the tag (since it may be still perfectly fine in another context!) <-- also render some sort of info (hover) text "Could not find in current context, make sure to load the depending datapack or join the server that defines this tag."
            switch (aItemLike) {
                case AItemLike.TagItemLike tagItemLike -> {
                    ticker.startTicking(tagItemLike);

                    result.put(aItemLike.toString(), tagItemLike);
                }
                case AItemLike.DirectItemLike directItemLike -> result.put(aItemLike.toString(), directItemLike);
                default -> {
                }
            }
        }

        return result;
    }

    public @NotNull List<@NotNull String> computeMatchingValues() {
        matchingItems = getMatchingItemIdentifiers(this.inputField);

        if (matchingItems.isEmpty()) {
            this.currentItemLike = null;
        } else {
            this.currentItemLike = matchingItems.firstEntry().getValue();
        }

        return List.copyOf(matchingItems.keySet());
    }

    protected void renderDropdownEntry(DrawContext graphics, Dimension<Integer> entryDimension, String identifier) {
        super.renderDropdownEntry(graphics, entryDimension, identifier);

        graphics.drawItemWithoutEntity(
            new ItemStack(this.matchingItems.get(identifier).getDisplayItem()),
            entryDimension.xLimit() - 2, entryDimension.y() + 1);
    }

    @Override
    public String getString(String identifier) {
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
        if (inputField.isEmpty() || itemTagController == null) {
            return super.getValueText();
        } else if (inputFieldFocused) {
            return Text.literal(inputField);
        } else {
            if (itemTagController.option().pendingValue() instanceof AItemLike.TagItemLike) {
                return Text.literal(itemTagController.option().pendingValue().toString());
            } else {
                return itemTagController.option().pendingValue().getDisplayItem().getName();
            }
        }
    }
}
