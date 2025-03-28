package com.daniking.backtools;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class Utils {
    // no public constructor
    private Utils () {}

    // Why Java? Why is there no "SequencedMap.of()"?
    public static <K, V> @NotNull SequencedMap<K, V> sequencedMapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs should be in pairs.");
        }

        SequencedMap<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return Collections.unmodifiableSequencedMap(map);
    }
}
