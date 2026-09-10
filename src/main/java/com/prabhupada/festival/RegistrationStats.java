package com.prabhupada.festival;

import java.util.Collection;

/**
 * What an organizer needs at a glance: how many parties registered, and how many
 * people that adds up to — the headcount prasādam is cooked for.
 */
public record RegistrationStats(int registrations, int attending, int largestParty) {

    static RegistrationStats of(Collection<Registration> roster) {
        int attending = roster.stream().mapToInt(Registration::guestCount).sum();
        int largest = roster.stream().mapToInt(Registration::guestCount).max().orElse(0);
        return new RegistrationStats(roster.size(), attending, largest);
    }
}
