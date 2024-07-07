package com.daniking.backtools;

import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TransformationSetting {
    private final static TransformationSetting EMPTY = new TransformationSetting(null, 0, 0, 0);

    private final float x, y, z;
    private final @Nullable ComponentMap components;

    public TransformationSetting(@Nullable ComponentMap componentsToMatch, float x, float y, float z) {
        this.components = componentsToMatch;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static TransformationSetting empty() {
        return EMPTY;
    }

    public boolean doComponentsMatch(@Nullable ComponentMap toCheck) {
        if (components == null) {
            return true;
        }
        if (toCheck == null) {
            return false;
        }

        for (DataComponentType<?> key : components.getTypes()) {
            if (!Objects.equals(components.get(key), toCheck.get(key))) {
                return false;
            }
        }

        return true;

    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
