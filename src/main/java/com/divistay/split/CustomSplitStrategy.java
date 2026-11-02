package com.divistay.split;

import com.divistay.domain.DomainException;
import com.divistay.domain.Money;
import com.divistay.domain.SplitType;

import java.util.ArrayList;
import java.util.List;

/** The organizer sets each amount; they must add up to the total exactly. */
public class CustomSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.CUSTOM;
    }

    @Override
    public List<Long> split(SplitContext context) {
        if (context.members().isEmpty()) {
            throw new DomainException("Add at least one member.");
        }
        List<Long> shares = new ArrayList<>();
        long sum = 0;
        for (MemberSpec m : context.members()) {
            if (m.customAmount() == null || m.customAmount() <= 0) {
                throw new DomainException("Enter a positive amount for " + (m.name().isBlank() ? m.email() : m.name()) + ".");
            }
            shares.add(m.customAmount());
            sum += m.customAmount();
        }
        if (sum != context.total()) {
            long diff = context.total() - sum;
            throw new DomainException("The amounts add up to " + Money.format(sum) + " but the booking costs "
                    + Money.format(context.total()) + " ("
                    + (diff > 0 ? Money.format(diff) + " missing" : Money.format(-diff) + " too much") + ").");
        }
        return List.copyOf(shares);
    }
}
