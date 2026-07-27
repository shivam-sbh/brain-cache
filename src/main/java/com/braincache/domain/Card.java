package com.braincache.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A flashcard == one thing to remember (front = prompt, back = answer). JPA entity,
 * same idea as User: annotations map class -> `cards` table.
 *
 * Owned by a user via userEmail (the JWT subject) — kept as a plain column for now,
 * no FK to User. The (user_email, next_review) index makes the "due cards for this
 * user" query cheap, which the daily email job hammers.
 *
 * intervalIndex points into ReviewProperties.intervalsDays; nextReview is when the
 * card should resurface. Mutable review state, so unlike User this exposes setters
 * the service uses to advance the ladder.
 */
@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_cards_user_next_review", columnList = "user_email, next_review")
})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String front;

    @Column(nullable = false)
    private String back;

    // Position in the config interval ladder. 0 == freshly created / just failed.
    @Column(name = "interval_index", nullable = false)
    private int intervalIndex;

    @Column(name = "next_review", nullable = false)
    private Instant nextReview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Card() {
    }

    public Card(String userEmail, String front, String back, Instant nextReview) {
        this.userEmail = userEmail;
        this.front = front;
        this.back = back;
        this.intervalIndex = 0;
        this.nextReview = nextReview;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    public int getIntervalIndex() {
        return intervalIndex;
    }

    public void setIntervalIndex(int intervalIndex) {
        this.intervalIndex = intervalIndex;
    }

    public Instant getNextReview() {
        return nextReview;
    }

    public void setNextReview(Instant nextReview) {
        this.nextReview = nextReview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
