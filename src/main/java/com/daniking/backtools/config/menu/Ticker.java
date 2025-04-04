package com.daniking.backtools.config.menu;

import com.daniking.backtools.config.AItemLike;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Ticker {
    private final @NotNull Set<AItemLike.TagItemLike> set = Collections.newSetFromMap(new WeakHashMap<>());
    private final @NotNull Duration duration;
    // I know we could be way faster using long encoded millis, but in my experience the risk of the timeunit getting messed up is not worth it
    private @NotNull Instant lastTick = Instant.now();

    public Ticker(final @NotNull Duration duration) {
        this.duration = duration;
    }

    /// Everything registered here is stored in a set with weak references to the objects.
    /// This effectively means everything just has to get registered, but not unregistered
    public void startTicking(final @NotNull AItemLike.TagItemLike tagItemLike) {
        set.add(tagItemLike);
    }

    public void tryToTick() {
        if (duration.compareTo(Duration.between(lastTick, Instant.now())) < 0) {
            set.forEach(AItemLike.TagItemLike::tick);
            lastTick = Instant.now();
        }
    }
}
