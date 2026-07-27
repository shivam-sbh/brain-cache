package com.braincache.service;

import com.braincache.config.ReviewProperties;
import com.braincache.domain.Card;
import com.braincache.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Spaced-repetition logic — gRPC-agnostic, like AuthService. Knows the interval
 * ladder from ReviewProperties and nothing about proto types.
 *
 * The whole algorithm is the ladder: pass climbs one rung (capped at the top),
 * fail drops to rung 0. nextReview = now + intervalsDays[index]. No ease factor,
 * no grades — deliberately simple.
 */
@Service
public class CardService {

    private final CardRepository cards;
    private final List<Integer> intervalsDays;

    public CardService(CardRepository cards, ReviewProperties review) {
        this.cards = cards;
        this.intervalsDays = review.intervalsDays();
    }

    @Transactional
    public Card create(String userEmail, String front, String back) {
        Instant next = Instant.now().plus(intervalsDays.get(0), ChronoUnit.DAYS);
        return cards.save(new Card(userEmail, front, back, next));
    }

    @Transactional(readOnly = true)
    public List<Card> listDue(String userEmail) {
        return cards.findByUserEmailAndNextReviewBefore(userEmail, Instant.now());
    }

    /**
     * Apply a review outcome. passed -> index+1 (capped at last rung); failed -> 0.
     * Reschedules nextReview off the new rung. Ownership enforced via id+email lookup.
     */
    @Transactional
    public Card review(String userEmail, long cardId, boolean passed) {
        Card card = cards.findByIdAndUserEmail(cardId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("card not found"));

        int lastRung = intervalsDays.size() - 1;
        int nextIndex = passed ? Math.min(card.getIntervalIndex() + 1, lastRung) : 0;

        card.setIntervalIndex(nextIndex);
        card.setNextReview(Instant.now().plus(intervalsDays.get(nextIndex), ChronoUnit.DAYS));
        return cards.save(card);
    }
}
