package com.divistay.split;

import com.divistay.domain.DomainException;
import com.divistay.domain.SplitType;

import java.util.List;

/** Everyone pays the same; leftover agorot go to the first members. */
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    public List<Long> split(SplitContext context) {
        if (context.members().isEmpty()) {
            throw new DomainException("Add at least one member.");
        }
        return SplitStrategy.divideEvenly(context.total(), context.members().size());
    }
}
