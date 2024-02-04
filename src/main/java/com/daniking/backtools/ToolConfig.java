package com.daniking.backtools;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ToolConfig {
    private final static ToolConfig EMPTY = new ToolConfig(null, 0, 0, 0);

    private final float x, y, z;
    private final @Nullable NbtCompound nbt;

    public ToolConfig(@Nullable NbtCompound nbtToMatch, float x, float y, float z) {
        this.nbt = nbtToMatch;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ToolConfig empty() {
        return EMPTY;
    }

    public boolean doesNBTMatch(@Nullable NbtCompound toCheck) {
        if (nbt == null) {
            return true;
        }
        if (toCheck == null) {
            return false;
        }

        for (String key : nbt.getKeys()) {
            if (!Objects.equals(nbt.get(key), toCheck.get(key))) {
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
