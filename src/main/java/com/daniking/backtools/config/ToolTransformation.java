package com.daniking.backtools.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("ClassExplicitlyExtendsObject") // we need to explicitly extend Object to inherit doc for equals (IntelliJ IDEA 2024.3.5 (Community Edition))
@Environment(EnvType.CLIENT)
public final class ToolTransformation extends Object {
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
    private final boolean isSymmetric;
    private final boolean isNegative;

    public ToolTransformation(@Nullable ComponentChanges componentChanges,
                              float rotationX, float rotationY, float rotationZ,
                              float offsetX, float offsetY, float offsetZ,
                              @Range(from = 0, to = Integer.MAX_VALUE) float scaleX, @Range(from = 0, to = Integer.MAX_VALUE) float scaleY, @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ,
                              boolean isSymmetric, boolean isNegative) {
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
        this.isSymmetric = isSymmetric;
        this.isNegative = isNegative;
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

    public boolean isSymmetric() {
        return isSymmetric;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public static @NotNull ToolTransformation deserialize (final @Nullable ComponentChanges changes, final boolean isNegative,
                                                  final @NotNull Map<@NotNull String, ? extends @NotNull Object> serializedTransformations) {
        float rotationX = 0f;
        float rotationY = 0f;
        float rotationZ = 0f;
        float offsetX = 0f;
        float offsetY = 0f;
        float offsetZ = 0f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleX = 1f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleY = 1f;
        @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ = 1f;
        boolean isSymmetric = true;

        if (serializedTransformations.get(BackToolsConfig.ROTATION_KEY) instanceof Map<?, ?> rotationMap) {
            // using number here, since the type is dependent on the implementation of the config file codec.
            // gson will return double here, even though we put float in
            // just to be safe for any format changes in the future we just take number for now;
            // may want to use instance of float <-- yes in lower case, whenever that's a stable future in java
            if (rotationMap.get(BackToolsConfig.X_KEY) instanceof Number temp) {
                rotationX = temp.floatValue();
            }
            if (rotationMap.get(BackToolsConfig.Y_KEY)  instanceof Number temp) {
                rotationY = temp.floatValue();
            }
            if (rotationMap.get(BackToolsConfig.Z_KEY) instanceof Number temp) {
                rotationZ = temp.floatValue();
            }
        }

        if (serializedTransformations.get(BackToolsConfig.OFFSET_KEY) instanceof Map<?, ?> rotationMap) {
            if (rotationMap.get(BackToolsConfig.X_KEY) instanceof Number temp) {
                offsetX = temp.floatValue();
            }
            if (rotationMap.get(BackToolsConfig.Y_KEY) instanceof Number temp) {
                offsetY = temp.floatValue();
            }
            if (rotationMap.get(BackToolsConfig.Z_KEY) instanceof Number temp) {
                offsetZ = temp.floatValue();
            }
        }

        if (serializedTransformations.get(BackToolsConfig.SCALE_KEY) instanceof Map<?, ?> scaleMap) {
            if (scaleMap.get(BackToolsConfig.X_KEY) instanceof Number temp) {
                scaleX = Math.max(0, Math.min(temp.floatValue(), Integer.MAX_VALUE));
            }
            if (scaleMap.get(BackToolsConfig.Y_KEY) instanceof Number temp) {
                scaleY = Math.max(0, Math.min(temp.floatValue(), Integer.MAX_VALUE));
            }
            if (scaleMap.get(BackToolsConfig.Z_KEY) instanceof Number temp) {
                scaleZ = Math.max(0, Math.min(temp.floatValue(), Integer.MAX_VALUE));
            }
        }

        if (serializedTransformations.get(BackToolsConfig.IS_SYMMETRIC) instanceof Boolean temp) {
            isSymmetric = temp;
        }

        return new ToolTransformation(
            changes,
            rotationX, rotationY, rotationZ,
            offsetX, offsetY, offsetZ,
            scaleX, scaleY, scaleZ,
            isSymmetric, isNegative);
    }

    @Override
    public @NotNull String toString() {
        return "ToolTransformation[" +
            "componentChanges: " + componentChanges + ", " +
            "rotationX = " +  rotationX + ", " +
            "rotationY = " + rotationY + ", " +
            "rotationZ = " + rotationZ + ", " +
            "offsetX = " + offsetX + ", " +
            "offsetY = " + offsetY + ", " +
            "offsetZ = " + offsetZ + ", " +
            "scaleX = " + scaleX + ", " +
            "scaleY = " + scaleY + ", " +
            "scaleZ = " + scaleZ + ", " +
            "isSymmetric = " + isSymmetric + ", " +
            "isNegative = " + isNegative +
            ']';
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentChanges,
            rotationX, rotationY, rotationZ,
            offsetX, offsetY, offsetZ,
            scaleX, scaleY, scaleZ,
            isSymmetric, isNegative);
    }

    /**
     * {@inheritDoc}
     *
     * @see #matches(ComponentChanges)
     * @see #matches(ToolTransformation)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof ToolTransformation other) {
            return Objects.equals(this.componentChanges, other.componentChanges) &&
                this.rotationX == other.rotationX && this.rotationY == other.rotationY && this.rotationZ == other.rotationZ &&
                this.offsetX == other.offsetX && this.offsetY == other.offsetY && this.offsetZ == other.offsetZ &&
                this.scaleX == other.scaleX && this.scaleY == other.scaleY && this.scaleZ == other.scaleZ &&
                this.isSymmetric == other.isSymmetric && this.isNegative == other.isNegative;
        } else {
            return false;
        }
    }
}
