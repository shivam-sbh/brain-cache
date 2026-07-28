package com.braincache.scheduler;

import com.braincache.config.AppProperties;
import com.braincache.domain.User;
import com.braincache.repository.UserRepository;
import com.braincache.service.DsaCatalog.Problem;
import com.braincache.service.DsaRecommendationService;
import com.braincache.service.EmailService;
import com.braincache.service.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final AppProperties app;

    public RecommendationJob(UserRepository users, DsaRecommendationService recommender, EmailService email, AppProperties app) {
        this.users = users;
        this.recommender = recommender;
        this.email = email;
        this.app = app;
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
        int count = recs.size();

        StringBuilder rows = new StringBuilder();
        int i = 0;
        for (Problem p : recs) {
            i++;
            rows.append("<tr><td style=\"padding-bottom:10px;\">")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#14171c\" style=\"background-color:#14171c;border:1px solid #23272f;border-radius:8px;\">")
                    .append("<tr>")
                    .append("<td width=\"6\" bgcolor=\"#6d63ff\" style=\"background-color:#6d63ff;width:6px;font-size:0;line-height:0;\">&nbsp;</td>")
                    .append("<td width=\"46\" valign=\"top\" style=\"padding:15px 0 15px 14px;font-family:'Courier New', Courier, monospace;font-size:12px;font-weight:bold;color:#626973;\">")
                    .append(String.format("%02d", i)).append("</td>")
                    .append("<td valign=\"top\" style=\"padding:15px 14px 15px 0;\">")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">")
                    .append("<tr><td style=\"font-family:Helvetica, Arial, sans-serif;font-size:15px;font-weight:bold;line-height:20px;\">")
                    .append("<a href=\"").append(EmailTemplate.escape(p.url())).append("\" style=\"color:#e7e9ec;text-decoration:none;\">")
                    .append(EmailTemplate.escape(p.title())).append("</a></td></tr>")
                    .append("<tr><td style=\"font-family:'Courier New', Courier, monospace;font-size:11px;line-height:17px;color:#626973;padding-top:4px;\">")
                    .append("Asked at ").append(p.numCompanies()).append(" companies &middot; ")
                    .append(EmailTemplate.escape(companiesLine(p))).append("</td></tr>")
                    .append("</table></td>")
                    .append("</tr></table></td></tr>");
        }

        String preheader = count + " problem" + (count == 1 ? "" : "s")
                + " to solve today — asked-at-company data included.";
        String heroSubtitle = "Solve, then mark done to add it to your revisions.";
        String dateStamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return EmailTemplate.wrap(
                "RECOMMENDATION JOB", dateStamp, preheader,
                String.valueOf(count), "DSA problem" + (count == 1 ? "" : "s") + " to solve", heroSubtitle,
                "PROBLEMS", rows.toString(),
                "open brain-cache", app.appUrl());
    }

    // Show every company, no cap — lists can be 75+ and that's fine.
    private String companiesLine(Problem p) {
        List<String> all = p.companyList();
        return all.isEmpty() ? "—" : String.join(", ", all);
    }
}
