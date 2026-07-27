package com.braincache.domain;

/**
 * What kind of review item a Card is.
 *
 * MANUAL — a flashcard the user created (front/back they wrote).
 * DSA    — a recommended LeetCode problem the user marked done; it then rides the
 *          same spaced-repetition ladder as any card.
 */
public enum CardType {
    MANUAL,
    DSA
}
