package com.braincache.service;

import com.braincache.config.RecommendationProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory catalog of the one-time DSA CSV export (hard.csv / medium.csv / easy.csv).
 * Loaded once at startup from RecommendationProperties.dataDir. The CSVs are already
 * sorted by NumCompanies descending, so each difficulty list is kept in file order —
 * the recommender just walks it top-down skipping done problems.
 *
 * Static reference data; if a file is missing we log and carry on with an empty list
 * so the app still boots.
 */
@Component
public class DsaCatalog {

    private static final Logger log = LoggerFactory.getLogger(DsaCatalog.class);

    /** One problem row. companies kept as the raw semicolon-joined string. */
    public record Problem(String title, String url, int numCompanies, String companies) {
    }

    private final Path dataDir;

    // difficulty key ("hard"/"medium"/"easy") -> ordered problems.
    private final Map<String, List<Problem>> byDifficulty = new HashMap<>();
    // url -> problem, for resolving a title when the user marks one done.
    private final Map<String, Problem> byUrl = new HashMap<>();

    public DsaCatalog(RecommendationProperties props) {
        this.dataDir = Path.of(props.dataDir());
    }

    @PostConstruct
    void load() {
        for (String diff : List.of("hard", "medium", "easy")) {
            List<Problem> problems = readCsv(dataDir.resolve(diff + ".csv"));
            byDifficulty.put(diff, problems);
            for (Problem p : problems) {
                byUrl.put(p.url(), p);
            }
            log.info("DSA catalog: loaded {} {} problems", problems.size(), diff);
        }
    }

    public List<Problem> byDifficulty(String difficulty) {
        return byDifficulty.getOrDefault(difficulty, List.of());
    }

    public Optional<Problem> byUrl(String url) {
        return Optional.ofNullable(byUrl.get(url));
    }

    // Minimal CSV read for our own well-formed export: Rank,Title,URL,NumCompanies,Companies.
    // Title/URL/NumCompanies never contain commas; Companies is the last field so we split
    // with a limit and keep the remainder intact.
    private List<Problem> readCsv(Path file) {
        List<Problem> out = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            log.warn("DSA catalog: file not found: {}", file.toAbsolutePath());
            return out;
        }
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            r.readLine(); // header
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split(",", 5);
                if (f.length < 5) {
                    continue;
                }
                int num;
                try {
                    num = Integer.parseInt(f[3].trim());
                } catch (NumberFormatException e) {
                    num = 0;
                }
                out.add(new Problem(f[1], f[2], num, f[4]));
            }
        } catch (Exception e) {
            log.warn("DSA catalog: failed reading {}: {}", file, e.getMessage());
        }
        return out;
    }
}
