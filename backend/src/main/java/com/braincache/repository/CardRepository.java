package com.braincache.repository;

import com.braincache.domain.Card;
import com.braincache.domain.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Derived-query repo, same magic as UserRepository: method name IS the query.
 *
 * - findByUserEmailAndNextReviewBefore -> due cards for one user (the ListDueCards RPC).
 * - findByNextReviewBefore             -> every due card, any user (the daily email job
 *   groups these by email in Java — fine at personal scale).
 * - findByIdAndUserEmail               -> ownership-checked fetch for ReviewCard, so a
 *   user can't review someone else's card.
 * - findByUserEmailAndType             -> a user's DSA cards; their URLs are the "already
 *   done" set the recommender skips.
 * - existsByUserEmailAndUrl            -> guard against marking the same problem done twice.
 */
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByUserEmailAndNextReviewBefore(String userEmail, Instant now);

    List<Card> findByNextReviewBefore(Instant now);

    Optional<Card> findByIdAndUserEmail(Long id, String userEmail);

    List<Card> findByUserEmailAndType(String userEmail, CardType type);

    // Full history for the caller, newest first — backs the ListHistory RPC / history rail.
    List<Card> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    boolean existsByUserEmailAndUrl(String userEmail, String url);
}
