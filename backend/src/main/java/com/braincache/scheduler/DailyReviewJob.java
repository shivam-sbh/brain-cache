package com.braincache.scheduler;

import com.braincache.domain.Card;
import com.braincache.repository.CardRepository;
import com.braincache.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily cron: find every due card, group by owner, email each user their review list.
 * Cron time comes from braincache.review.email-cron (default 07:00).
 *
 * The email only reminds — it does NOT advance the ladder. The user still calls the
 * ReviewCard RPC to record pass/fail. (Resolving reviews from the email itself is a
 * later step.)
 *
 * Go parallel: a ticker goroutine that on each tick queries the DB and fans out mail.
 */
@Component
public class DailyReviewJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReviewJob.class);

    private final CardRepository cards;
    private final EmailService email;

    public DailyReviewJob(CardRepository cards, EmailService email) {
        this.cards = cards;
        this.email = email;
    }

    @Scheduled(cron = "${braincache.review.email-cron}")
    @Transactional(readOnly = true)
    public void sendDailyReviews() {
        List<Card> due = cards.findByNextReviewBefore(Instant.now());
        if (due.isEmpty()) {
            log.info("Daily review: no cards due");
            return;
        }

        Map<String, List<Card>> byUser = due.stream()
                .collect(Collectors.groupingBy(Card::getUserEmail));

        log.info("Daily review: {} cards due across {} users", due.size(), byUser.size());

        byUser.forEach((userEmail, userCards) -> {
            try {
                email.send(userEmail, subject(userCards.size()), body(userCards));
            } catch (Exception e) {
                // One user's mail failure must not abort the rest.
                log.warn("Failed to send review email to {}: {}", userEmail, e.getMessage());
            }
        });
    }

    private String subject(int count) {
        return "Brain-Cache: Revision — " + count + " card" + (count == 1 ? "" : "s") + " due";
    }

    private String body(List<Card> userCards) {
        StringBuilder sb = new StringBuilder("You have cards due for review:\n\n");
        int i = 1;
        for (Card c : userCards) {
            sb.append(i++).append(". ").append(c.getFront()).append('\n');
        }
        sb.append("\nOpen Brain Cache to review them.");
        return sb.toString();
    }
}
