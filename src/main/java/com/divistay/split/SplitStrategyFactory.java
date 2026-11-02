package com.divistay.split;

import com.divistay.domain.SplitType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Returns the right {@link SplitStrategy} for the organizer's choice (design pattern: Factory).
 * Adding a new split type means adding one class and registering it here.
 */
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies = new EnumMap<>(SplitType.class);

    public SplitStrategyFactory() {
        this(List.of(new EqualSplitStrategy(), new RoomBasedSplitStrategy(), new CustomSplitStrategy()));
    }

    public SplitStrategyFactory(List<SplitStrategy> available) {
        for (SplitStrategy s : available) {
            strategies.put(s.type(), s);
        }
        for (SplitType type : SplitType.values()) {
            if (!strategies.containsKey(type)) {
                throw new IllegalStateException("No strategy registered for " + type);
            }
        }
    }

    public SplitStrategy forType(SplitType type) {
        return strategies.get(type);
    }
}
