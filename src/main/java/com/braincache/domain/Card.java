package com.braincache.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A review item == one thing to remember. JPA entity, same idea as User: annotations
 * map class -> `cards` table.
 *
 * Two flavours (see CardType): a MANUAL flashcard (front = prompt, back = answer the
 * user wrote) or a DSA problem the user marked done (front = problem title, back =
 * LeetCode URL, plus an optional comment). Both ride the same interval ladder, so the
 * one Revision job feeds on all of them.
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

    // Kind of item. Stored as its name (STRING), not ordinal, so reordering the enum
    // never corrupts existing rows.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType type = CardType.MANUAL;

    @Column(nullable = false)
    private String front;

    @Column(nullable = false)
    private String back;

    // Only set for DSA cards: the LeetCode problem URL. Also the dedup key that stops a
    // done problem being recommended again.
    @Column
    private String url;

    // Optional user note captured when marking a DSA problem done.
    @Column(columnDefinition = "text")
    private String comment;

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

    /**
     * Factory for a DSA card: a recommended problem the user just marked done. Front is
     * the problem title, back is the URL; both feed the Revision email like any card.
     */
    public static Card dsa(String userEmail, String title, String url, String comment, Instant nextReview) {
        Card c = new Card(userEmail, title, url, nextReview);
        c.type = CardType.DSA;
        c.url = url;
        c.comment = comment;
        return c;
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public CardType getType() {
        return type;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    public String getUrl() {
        return url;
    }

    public String getComment() {
        return comment;
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
