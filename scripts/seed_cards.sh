#!/usr/bin/env bash
#
# seed_cards.sh — populate the cards table with distributed demo data so the
# history rail, "Due today" list, and DSA enrichment all have something to show.
#
#   ./scripts/seed_cards.sh [email]
#
# Defaults to the repo owner's account. Cards spread across the last ~2 weeks
# (created_at), a mix of MANUAL notes and DSA cards, with roughly a third due
# now so the Today pane isn't empty. DSA urls are real catalog slugs, so the
# backend enriches them with company names.
#
# Idempotent-ish: pass --wipe to clear this user's cards first.
set -euo pipefail

EMAIL="${1:-sbh.shivam@gmail.com}"
WIPE=false
[[ "${2:-}" == "--wipe" || "${1:-}" == "--wipe" ]] && WIPE=true
[[ "${1:-}" == "--wipe" ]] && EMAIL="${2:-sbh.shivam@gmail.com}"

CONTAINER="${PG_CONTAINER:-braincache-postgres}"
DB="${POSTGRES_DB:-braincache}"
USER="${POSTGRES_USER:-braincache}"

echo "Seeding cards for: $EMAIL  (container=$CONTAINER db=$DB)"

psql() { docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U "$USER" -d "$DB" "$@"; }

if $WIPE; then
  echo "Wiping existing cards for $EMAIL ..."
  psql -c "DELETE FROM cards WHERE user_email = '$EMAIL';"
fi

# ── the seed rows ────────────────────────────────────────────────────────────
# created_at is spread over the last 14 days; next_review is either in the past
# (due now) or a few days out. interval_index varies so cards sit on different
# rungs. DSA rows carry the LeetCode url in both `back` and `url`.
psql <<SQL
BEGIN;

-- MANUAL notes ---------------------------------------------------------------
INSERT INTO cards (user_email, type, front, back, url, comment, interval_index, next_review, created_at) VALUES
('$EMAIL','MANUAL','Postgres MVCC',            E'Each row version tagged with xmin/xmax. Readers see a snapshot; writers never block readers. VACUUM reclaims dead tuples.', NULL, NULL, 2, now() - interval '1 minute',  now() - interval '0 day'   - interval '3 hour'),
('$EMAIL','MANUAL','gRPC vs REST',             E'gRPC = HTTP/2 + protobuf, binary framing, streaming, codegen contracts. REST = text/JSON over HTTP/1.1. gRPC wins on latency + typing, loses on browser reach (needs grpc-web).', NULL, NULL, 1, now() + interval '2 day',       now() - interval '0 day'   - interval '6 hour'),
('$EMAIL','MANUAL','Spring @Transactional',    E'Proxy-based. Self-invocation bypasses the proxy = no transaction. Default rollback on RuntimeException only; checked exceptions commit unless rollbackFor set.', NULL, NULL, 0, now() - interval '1 minute',  now() - interval '1 day'   - interval '2 hour'),
('$EMAIL','MANUAL','JWT expiry',               E'exp is a Unix timestamp claim. Server rejects when now > exp. Refresh tokens rotate; access tokens stay short-lived (~15m).', NULL, NULL, 3, now() + interval '4 day',       now() - interval '1 day'   - interval '7 hour'),
('$EMAIL','MANUAL','Envoy dns_lookup_family',  E'V4_ONLY forces IPv4 resolution. Fixed the dead host.docker.internal IPv6 route to the gRPC backend.', NULL, NULL, 1, now() - interval '1 minute',  now() - interval '2 day'   - interval '4 hour'),
('$EMAIL','MANUAL','Go channels vs Java',      E'Go channels = typed pipes + select. Java has no direct analogue; closest is BlockingQueue + manual goroutine=thread mgmt. CSP vs shared-memory.', NULL, NULL, 2, now() + interval '1 day',       now() - interval '3 day'   - interval '5 hour'),
('$EMAIL','MANUAL','Index-only scan',          E'When all needed columns live in the index, Postgres skips the heap. Covering index (INCLUDE) makes more queries index-only.', NULL, NULL, 0, now() - interval '1 minute',  now() - interval '5 day'   - interval '2 hour'),
('$EMAIL','MANUAL','Idempotency keys',         E'Client sends a unique key; server dedupes retries. Store key->result; return cached result on replay. Critical for at-least-once delivery.', NULL, NULL, 2, now() + interval '3 day',       now() - interval '7 day'   - interval '8 hour'),
('$EMAIL','MANUAL','SMTP STARTTLS',            E'Plaintext connect on 587, then upgrade to TLS via STARTTLS. Mailpit (dev) needs auth+starttls off; SES (prod) needs both on.', NULL, NULL, 1, now() + interval '2 day',       now() - interval '10 day'  - interval '3 hour'),
('$EMAIL','MANUAL','Connection pooling',       E'Reuse open DB connections across requests; skip TCP + auth handshake each time. HikariCP defaults are sane; size ~= cores*2 for CPU-bound.', NULL, NULL, 4, now() + interval '6 day',       now() - interval '13 day'  - interval '6 hour');

-- DSA cards (real catalog urls -> enriched with companies) --------------------
INSERT INTO cards (user_email, type, front, back, url, comment, interval_index, next_review, created_at) VALUES
('$EMAIL','DSA','Two Sum',                                        'https://leetcode.com/problems/two-sum',                                       'https://leetcode.com/problems/two-sum',                                       E'Hash map of value->index in one pass. O(n) time, O(n) space.', 3, now() + interval '3 day',      now() - interval '0 day'  - interval '2 hour'),
('$EMAIL','DSA','LRU Cache',                                      'https://leetcode.com/problems/lru-cache',                                     'https://leetcode.com/problems/lru-cache',                                     E'HashMap + doubly linked list. Map to nodes, list tracks recency. get/put O(1).', 1, now() - interval '1 minute', now() - interval '1 day'  - interval '4 hour'),
('$EMAIL','DSA','Merge Intervals',                               'https://leetcode.com/problems/merge-intervals',                               'https://leetcode.com/problems/merge-intervals',                               E'Sort by start, sweep, merge when overlap (cur.start <= prev.end). O(n log n).', 2, now() + interval '2 day',      now() - interval '2 day'  - interval '3 hour'),
('$EMAIL','DSA','Number of Islands',                             'https://leetcode.com/problems/number-of-islands',                             'https://leetcode.com/problems/number-of-islands',                             E'DFS/BFS flood-fill each unvisited land cell; count components. Sink visited to 0.', 0, now() - interval '1 minute', now() - interval '3 day'  - interval '5 hour'),
('$EMAIL','DSA','Longest Substring Without Repeating Characters', 'https://leetcode.com/problems/longest-substring-without-repeating-characters', 'https://leetcode.com/problems/longest-substring-without-repeating-characters', E'Sliding window + last-seen index map. Shrink left past duplicate. O(n).', 2, now() + interval '4 day',      now() - interval '5 day'  - interval '2 hour'),
('$EMAIL','DSA','Product of Array Except Self',                  'https://leetcode.com/problems/product-of-array-except-self',                  'https://leetcode.com/problems/product-of-array-except-self',                  E'Prefix products left-to-right, then suffix on the return pass. No division, O(n).', 1, now() - interval '1 minute', now() - interval '7 day'  - interval '6 hour'),
('$EMAIL','DSA','Coin Change',                                   'https://leetcode.com/problems/coin-change',                                   'https://leetcode.com/problems/coin-change',                                   E'DP dp[amt] = min coins. dp[a] = min(dp[a-c]+1). Unbounded knapsack flavour.', 3, now() + interval '5 day',      now() - interval '9 day'  - interval '3 hour'),
('$EMAIL','DSA','Course Schedule',                               'https://leetcode.com/problems/course-schedule',                               'https://leetcode.com/problems/course-schedule',                               E'Cycle detection on directed graph. Kahn topo-sort or DFS with colour states.', 2, now() + interval '2 day',      now() - interval '11 day' - interval '4 hour'),
('$EMAIL','DSA','Word Break',                                    'https://leetcode.com/problems/word-break',                                    'https://leetcode.com/problems/word-break',                                    E'DP over prefixes; dp[i] true if some j has dp[j] && s[j..i] in dict. O(n^2).', 1, now() + interval '1 day',      now() - interval '13 day' - interval '5 hour'),
('$EMAIL','DSA','Trapping Rain Water',                           'https://leetcode.com/problems/trapping-rain-water',                           'https://leetcode.com/problems/trapping-rain-water',                           E'Two pointers, track leftMax/rightMax. Water = min(maxes) - height. O(n), O(1).', 4, now() + interval '7 day',      now() - interval '13 day' - interval '9 hour');

COMMIT;
SQL

echo
psql -c "SELECT to_char(created_at,'YYYY-MM-DD') AS day, count(*) FROM cards WHERE user_email='$EMAIL' GROUP BY 1 ORDER BY 1 DESC;"
echo "Done. Reload the app — history rail + Today should be populated."
