package com.braincache.service;

import com.braincache.config.RecommendationProperties;
import com.braincache.domain.Card;
import com.braincache.domain.CardType;
import com.braincache.repository.CardRepository;
import com.braincache.service.DsaCatalog.Problem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks the DSA problems to recommend a user on a given weekday. gRPC/email-agnostic.
 *
 * A problem is "done" once the user has a DSA Card for its URL — that set is what we
 * skip. For each difficulty we pull the day's configured count of top-unseen problems
 * (the catalog is already ordered most-asked first). Not-yet-done problems keep
 * resurfacing until the user marks them done.
 */
@Service
public class DsaRecommendationService {

    private final DsaCatalog catalog;
    private final CardRepository cards;
    private final RecommendationProperties props;

    public DsaRecommendationService(DsaCatalog catalog, CardRepository cards, RecommendationProperties props) {
        this.catalog = catalog;
        this.cards = cards;
        this.props = props;
    }

    /** dayIndex: 0 = Monday .. 6 = Sunday. Returns the problems to email today. */
    @Transactional(readOnly = true)
    public List<Problem> recommendFor(String userEmail, int dayIndex) {
        Set<String> done = cards.findByUserEmailAndType(userEmail, CardType.DSA).stream()
                .map(Card::getUrl)
                .collect(Collectors.toSet());

        List<Problem> out = new ArrayList<>();
        out.addAll(pick("hard", props.countFor(props.hard(), dayIndex), done));
        out.addAll(pick("medium", props.countFor(props.medium(), dayIndex), done));
        out.addAll(pick("easy", props.countFor(props.easy(), dayIndex), done));
        return out;
    }

    private List<Problem> pick(String difficulty, int count, Set<String> done) {
        if (count <= 0) {
            return List.of();
        }
        List<Problem> picked = new ArrayList<>(count);
        for (Problem p : catalog.byDifficulty(difficulty)) {
            if (done.contains(p.url())) {
                continue;
            }
            picked.add(p);
            if (picked.size() == count) {
                break;
            }
        }
        return picked;
    }
}
