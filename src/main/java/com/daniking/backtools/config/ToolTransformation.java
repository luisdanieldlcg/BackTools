package com.daniking.backtools.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class ToolTransformation {
    private final static ToolTransformation EMPTY = new ToolTransformation(null,
        0F, 0F, 0F,
        0F, 0F, 0F,
        1F, 1F, 1F, false);

    private final @Nullable ComponentChanges componentChanges;
    private final float rotationX;
    private final float rotationY;
    private final float rotationZ;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleX;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleY;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ;
    private final boolean isNegative;

    public ToolTransformation(@Nullable ComponentChanges componentChanges,
                              float rotationX, float rotationY, float rotationZ,
                              float offsetX, float offsetY, float offsetZ,
                              @Range(from = 0, to = Integer.MAX_VALUE) float scaleX, @Range(from = 0, to = Integer.MAX_VALUE) float scaleY, @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ, boolean isNegative) {
        this.componentChanges = componentChanges;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.isNegative = isNegative;
    }

    public static ToolTransformation empty() {
        return EMPTY;
    }

    /**
     * @return true, if the ComponentChanges hold by this Object are null / empty,
     * or if all ComponentTypes of this object's ComponentChanges map to the same optional value in the parameter.
     * <p>
     * This effectively means, in the non-trivial case, that all changes in this object must be the same as the parameter one.
     * But the parameter one may contain additional ComponentChanges, that are ignored.
     */
    public boolean matches(final @Nullable ComponentChanges otherComponentChanges) {
        if (this.componentChanges == otherComponentChanges) {
            return true;
        } else if (this.componentChanges == null || this.componentChanges.isEmpty()) {
            return true;
        } else if (otherComponentChanges == null || otherComponentChanges.isEmpty()) {
            return false;
        } else { // both != null
            for (final @NotNull Map.Entry<@NotNull ComponentType<?>, @NotNull Optional<?>> entry : this.componentChanges.entrySet()) {
                if (!entry.getValue().equals(otherComponentChanges.get(entry.getKey()))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true, if the ComponentChanges hold by this Object are null / empty,
     * or if all ComponentTypes of this object's ComponentChanges map to the same optional value in the parameter.
     * <p>
     * This effectively means, in the non-trivial case, that all changes in this object must be the same as the parameter one.
     * But the parameter one may contain additional ComponentChanges, that are ignored.
     */
    public boolean matches(final @NotNull ToolTransformation otherToolTransformation) {
        return matches(otherToolTransformation.componentChanges);
    }

    public float rotationX() {
        return rotationX;
    }

    public float rotationY() {
        return rotationY;
    }

    public float rotationZ() {
        return rotationZ;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public float offsetZ() {
        return offsetZ;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleX() {
        return scaleX;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleY() {
        return scaleY;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ() {
        return scaleZ;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public static ToolTransformation deserialize (final @Nullable ComponentChanges changes, final boolean isNegative, final @NotNull Map<@NotNull String, @NotNull Map<@NotNull String, @NotNull Float>> serializedTransformations) {
        float rotationX = 0f;
        float rotationY = 0f;
        float rotationZ = 0f;
        float offsetX = 0f;
        float offsetY = 0f;
        float offsetZ = 0f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleX = 1f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleY = 1f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ = 1f;

        @Nullable Float temp;
        if (serializedTransformations.get(BackToolsConfig.ROTATION_KEY) instanceof Map<String, Float> rotationMap) {
            if ((temp = rotationMap.get(BackToolsConfig.X_KEY)) != null) {
                rotationX = temp;
            }
            if ((temp = rotationMap.get(BackToolsConfig.Y_KEY)) != null) {
                rotationY = temp;
            }
            if ((temp = rotationMap.get(BackToolsConfig.Z_KEY)) != null) {
                rotationZ = temp;
            }
        }

        if (serializedTransformations.get(BackToolsConfig.OFFSET_KEY) instanceof Map<String, Float> rotationMap) {
            if ((temp = rotationMap.get(BackToolsConfig.X_KEY)) != null) {
                offsetX = temp;
            }
            if ((temp = rotationMap.get(BackToolsConfig.Y_KEY)) != null) {
                offsetY = temp;
            }
            if ((temp = rotationMap.get(BackToolsConfig.Z_KEY)) != null) {
                offsetZ = temp;
            }
        }

        if (serializedTransformations.get(BackToolsConfig.SCALE_KEY) instanceof Map<String, Float> rotationMap) {
            if ((temp = rotationMap.get(BackToolsConfig.X_KEY)) != null) {
                offsetX = Math.max(0, Math.min(temp, Integer.MAX_VALUE));
            }
            if ((temp = rotationMap.get(BackToolsConfig.Y_KEY)) != null) {
                offsetY = Math.max(0, Math.min(temp, Integer.MAX_VALUE));
            }
            if ((temp = rotationMap.get(BackToolsConfig.Z_KEY)) != null) {
                offsetZ = Math.max(0, Math.min(temp, Integer.MAX_VALUE));
            }
        }

        return new ToolTransformation(
            changes,
            rotationX, rotationY, rotationZ,
            offsetX, offsetY, offsetZ,
            scaleX, scaleY, scaleZ, isNegative);
    }
}
