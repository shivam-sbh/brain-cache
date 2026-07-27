package com.braincache.scheduler;

import com.braincache.domain.User;
import com.braincache.repository.UserRepository;
import com.braincache.service.DsaCatalog.Problem;
import com.braincache.service.DsaRecommendationService;
import com.braincache.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily cron: recommend each user the day's DSA problems ("Brain-Cache: Recommendation").
 * Difficulty + count come from the weekly schedule (RecommendationProperties); the pick
 * itself lives in DsaRecommendationService.
 *
 * Unlike the Revision job (which emails only when cards are due), this walks all users
 * and emails whoever has something recommended today. A user with everything done, or a
 * day configured to zero, simply gets no mail.
 *
 * The email only surfaces problems — marking one done is the MarkDsaProblemDone RPC,
 * which is what moves it into the Revision stream.
 */
@Component
public class RecommendationJob {

    private static final Logger log = LoggerFactory.getLogger(RecommendationJob.class);

    private final UserRepository users;
    private final DsaRecommendationService recommender;
    private final EmailService email;

    public RecommendationJob(UserRepository users, DsaRecommendationService recommender, EmailService email) {
        this.users = users;
        this.recommender = recommender;
        this.email = email;
    }

    @Scheduled(cron = "${braincache.recommendation.cron}")
    public void sendRecommendations() {
        int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1; // 0 = Monday
        int sent = 0;
        for (User user : users.findAll()) {
            List<Problem> recs = recommender.recommendFor(user.getEmail(), dayIndex);
            if (recs.isEmpty()) {
                continue;
            }
            try {
                email.send(user.getEmail(), subject(recs.size()), body(recs));
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send recommendation email to {}: {}", user.getEmail(), e.getMessage());
            }
        }
        log.info("Daily recommendation: emailed {} users (day index {})", sent, dayIndex);
    }

    private String subject(int count) {
        return "Brain-Cache: Recommendation — " + count + " problem" + (count == 1 ? "" : "s");
    }

    private String body(List<Problem> recs) {
        StringBuilder sb = new StringBuilder("Today's DSA problems to solve:\n\n");
        int i = 1;
        for (Problem p : recs) {
            sb.append(i++).append(". ").append(p.title())
                    .append("  (asked at ").append(p.numCompanies()).append(" companies)\n")
                    .append("   ").append(p.url()).append('\n');
        }
        sb.append("\nSolve, then mark done (MarkDsaProblemDone) to add it to your revisions.");
        return sb.toString();
    }
}
