package com.daniking.backtools.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

@Environment(EnvType.CLIENT)
public class LinkedHashMapFactory {
    // no public constructor
    private LinkedHashMapFactory() {}

    // Why Java? Why is there no "SequencedMap.of()"?
    public static <K, V> @NotNull LinkedHashMap<K, V> linkedHashMapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs should be in pairs.");
        }

        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }
}
