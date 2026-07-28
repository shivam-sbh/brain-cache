package com.braincache.scheduler;

import com.braincache.config.AppProperties;
import com.braincache.config.ReviewProperties;
import com.braincache.domain.Card;
import com.braincache.repository.CardRepository;
import com.braincache.service.EmailService;
import com.braincache.service.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final ReviewProperties review;
    private final AppProperties app;

    public DailyReviewJob(CardRepository cards, EmailService email, ReviewProperties review, AppProperties app) {
        this.cards = cards;
        this.email = email;
        this.review = review;
        this.app = app;
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
        int count = userCards.size();
        List<Integer> ladder = review.intervalsDays();
        int lastRung = ladder.size() - 1;

        StringBuilder rows = new StringBuilder();
        int i = 0;
        for (Card c : userCards) {
            i++;
            int rung = Math.max(0, Math.min(c.getIntervalIndex(), lastRung));
            int days = ladder.get(rung);
            rows.append("<tr><td style=\"padding-bottom:10px;\">")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#14171c\" style=\"background-color:#14171c;border:1px solid #23272f;border-radius:8px;\">")
                    .append("<tr>")
                    .append("<td width=\"6\" bgcolor=\"#6d63ff\" style=\"background-color:#6d63ff;width:6px;font-size:0;line-height:0;\">&nbsp;</td>")
                    .append("<td width=\"46\" valign=\"top\" style=\"padding:15px 0 15px 14px;font-family:'Courier New', Courier, monospace;font-size:12px;font-weight:bold;color:#626973;\">")
                    .append(String.format("%02d", i)).append("</td>")
                    .append("<td valign=\"top\" style=\"padding:15px 14px 15px 0;font-family:'Courier New', Courier, monospace;font-size:14px;line-height:21px;color:#e7e9ec;word-break:break-word;\">")
                    .append(EmailTemplate.escape(c.getFront())).append("</td>")
                    .append("<td width=\"78\" valign=\"top\" align=\"right\" style=\"padding:15px 14px 15px 0;font-family:'Courier New', Courier, monospace;font-size:11px;color:#626973;white-space:nowrap;\">")
                    .append("d").append(days).append("</td>")
                    .append("</tr></table></td></tr>");
        }

        String ladderText = ladder.stream().map(String::valueOf).collect(Collectors.joining(" &#8594; "));
        String heroSubtitle = "Clear them today and each one steps up the ladder&nbsp;— " + ladderText
                + ". Skip, and it drops back to&nbsp;day&nbsp;1.";
        String preheader = count + " card" + (count == 1 ? "" : "s")
                + " due — skip today and the ladder resets to day 1.";
        String dateStamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return EmailTemplate.wrap(
                "REVIEW JOB", dateStamp, preheader,
                String.valueOf(count), "card" + (count == 1 ? "" : "s") + " due for revision", heroSubtitle,
                "QUEUE", rows.toString(),
                "run review", app.appUrl());
    }
}
