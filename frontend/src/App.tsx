import { useCallback, useEffect, useRef, useState } from "react";
import { ConnectError } from "@connectrpc/connect";
import {
  LogOut,
  Search,
  Check,
  X,
  Plus,
  Terminal,
  ExternalLink,
  Loader2,
  ChevronRight,
  ArrowLeft,
  CalendarDays,
  Clock,
  Layers,
  Zap,
  CornerDownRight,
} from "lucide-react";
import {
  authClient,
  cardClient,
  getToken,
  setToken,
  getEmail,
  setEmail,
  clearSession,
} from "./client";
import type { Card as CardMsg, DsaProblem } from "./gen/brain_cache_pb";

/* ── wordmark ──────────────────────────────────────────────────────────── */
function Wordmark({ big = false }: { big?: boolean }) {
  return (
    <span className={`wordmark${big ? " big" : ""}`}>
      <span className="glyph">◈</span>
      <span className="mark-text">
        brain<span className="dim">cache</span>
      </span>
    </span>
  );
}

/* ── date helpers (local-day grouping for the history rail) ───────────────── */
const dayKey = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}`;
};
const todayKey = () => dayKey(new Date().toISOString());
function labelForDay(key: string) {
  const t = todayKey();
  if (key === t) return "Today";
  const d = new Date(key + "T00:00:00");
  const yest = new Date();
  yest.setDate(yest.getDate() - 1);
  if (key === dayKey(yest.toISOString())) return "Yesterday";
  return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}
const timeOf = (iso: string) =>
  new Date(iso).toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });

/* ── toast hook (error + success) ──────────────────────────────────────── */
type Toast = { kind: "error" | "ok"; msg: string };
function useToast() {
  const [toast, setToast] = useState<Toast | null>(null);
  const timer = useRef<number | undefined>(undefined);
  const show = useCallback((t: Toast) => {
    setToast(t);
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), 4000);
  }, []);
  const fail = useCallback(
    (err: unknown) =>
      show({ kind: "error", msg: err instanceof ConnectError ? err.rawMessage : String(err) }),
    [show],
  );
  const ok = useCallback((msg: string) => show({ kind: "ok", msg }), [show]);
  const node = toast ? <div className={`toast ${toast.kind}`}>{toast.msg}</div> : null;
  return { fail, ok, node };
}

/* ── root gate ─────────────────────────────────────────────────────────── */
export function App() {
  const [token, setTok] = useState<string | null>(getToken());
  const [email, setEmailState] = useState<string | null>(getEmail());

  if (!token) {
    return (
      <Auth
        onAuthed={(t, e) => {
          setToken(t);
          setEmail(e);
          setTok(t);
          setEmailState(e);
        }}
      />
    );
  }
  return (
    <Dashboard
      email={email}
      onLogout={() => {
        clearSession();
        setTok(null);
        setEmailState(null);
      }}
    />
  );
}

/* ── auth ──────────────────────────────────────────────────────────────── */
function Auth({ onAuthed }: { onAuthed: (token: string, email: string) => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState<"login" | "register">("login");
  const [busy, setBusy] = useState(false);
  const { fail, node } = useToast();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await authClient[mode]({ email, password });
      onAuthed(res.token, res.email);
    } catch (err) {
      fail(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="center">
          <Wordmark />
        </div>
        <div className="lead">
          <h1>{mode === "login" ? "Sign in" : "Create account"}</h1>
          <p>Cache what you learn. Ship what you cache.</p>
        </div>
        <form onSubmit={submit} className="stack">
          <div className="field">
            <label>Email</label>
            <input
              className="input"
              type="email"
              autoFocus
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Password</label>
            <input
              className="input"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button className="btn block" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : mode === "login" ? "Sign in" : "Sign up"}
          </button>
          <button
            type="button"
            className="btn subtle"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "Need an account? Create one" : "Have an account? Sign in"}
          </button>
        </form>
      </div>
      {node}
    </div>
  );
}

/* ── dashboard ─────────────────────────────────────────────────────────── */
function Dashboard({ email, onLogout }: { email: string | null; onLogout: () => void }) {
  const [cards, setCards] = useState<CardMsg[]>([]);
  const [history, setHistory] = useState<CardMsg[]>([]);
  const [loading, setLoading] = useState(true);
  const [front, setFront] = useState("");
  const [back, setBack] = useState("");
  const [activeDay, setActiveDay] = useState<string | null>(null);
  const { fail, ok, node } = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [due, all] = await Promise.all([
        cardClient.listDueCards({}),
        cardClient.listHistory({}),
      ]);
      setCards(due.cards);
      setHistory(all.cards);
    } catch (err) {
      fail(err);
    } finally {
      setLoading(false);
    }
  }, [fail]);

  useEffect(() => {
    void load();
  }, [load]);

  // Group the full history into ordered days (newest first).
  const days = (() => {
    const map = new Map<string, CardMsg[]>();
    for (const c of history) {
      if (!c.createdAt) continue;
      const k = dayKey(c.createdAt);
      (map.get(k) ?? map.set(k, []).get(k)!).push(c);
    }
    return [...map.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1));
  })();
  const activeCards = activeDay ? history.filter((c) => c.createdAt && dayKey(c.createdAt) === activeDay) : [];

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!front.trim()) return;
    try {
      await cardClient.createCard({ front, back });
      setFront("");
      setBack("");
      ok("Cached.");
      await load();
    } catch (err) {
      fail(err);
    }
  };

  const review = async (card: CardMsg, passed: boolean) => {
    try {
      await cardClient.reviewCard({ cardId: card.id, passed });
      setCards((cs) => cs.filter((c) => c.id !== card.id));
    } catch (err) {
      fail(err);
    }
  };

  const initial = (email ?? "?").charAt(0).toUpperCase();

  return (
    <div className="shell">
      <div className="aurora" aria-hidden />
      <header className="topbar">
        <Wordmark big />
        <div className="spacer" />
        <div className="avatar" title={email ?? ""}>
          {initial}
        </div>
        <button className="btn logout" onClick={onLogout}>
          <LogOut size={16} strokeWidth={2.2} />
          Logout
        </button>
      </header>

      <div className="workspace">
        {/* sliding stage: pane 0 = today, pane 1 = a chosen history day */}
        <main className="stage">
          <div className={`stage-track${activeDay ? " slid" : ""}`}>
            {/* ── pane: today ── */}
            <section className="pane">
              <div className="page-head">
                <h1 className="big-today">Today</h1>
                <div className="rule" />
                <p className="page-sub">Cache what you learn. Ship what you cache.</p>
              </div>

              <section className="panel">
                <div className="panel-head">
                  <Terminal size={16} strokeWidth={2.2} />
                  <h2>Cache a Note</h2>
                </div>
                <form onSubmit={create} className="stack">
                  <div className="field">
                    <label>Heading</label>
                    <input
                      className="input mono"
                      placeholder="Short title — this is what shows in the email."
                      value={front}
                      onChange={(e) => setFront(e.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label>What I learned!</label>
                    <textarea
                      className="textarea tall mono"
                      placeholder="Write as much as you want — paste 1000 lines if you like. Full text lives here; the email shows only the heading."
                      value={back}
                      onChange={(e) => setBack(e.target.value)}
                    />
                  </div>
                  <div className="row">
                    <button className="btn" type="submit">
                      <Plus size={16} strokeWidth={2.4} />
                      Cache it
                    </button>
                  </div>
                </form>
              </section>

              <SolvePanel
                onDone={async () => {
                  ok("Solve cached — queued for recall.");
                  await load();
                }}
                onError={fail}
              />

              <div className="page-head">
                <h1 className="queue-title">Recall Queue</h1>
                <div className="rule" />
                <p className="page-sub">
                  Cards due for review.
                  {!loading && cards.length > 0 && ` · ${cards.length} due`}
                </p>
              </div>

              {loading ? (
                <div className="empty">
                  <Loader2 className="spin" size={20} />
                </div>
              ) : cards.length === 0 ? (
                <div className="queue-empty">
                  <Zap size={24} strokeWidth={1.8} />
                  <span className="qe-title">Cache warm</span>
                  <span className="qe-sub">Nothing to recall right now.</span>
                </div>
              ) : (
                <div className="queue-list">
                  {cards.map((c) => (
                    <CardItem key={String(c.id)} card={c} onReview={review} />
                  ))}
                </div>
              )}
            </section>

            {/* ── pane: a history day ── */}
            <section className="pane">
              {activeDay && (
                <DayView day={activeDay} cards={activeCards} onBack={() => setActiveDay(null)} />
              )}
            </section>
          </div>
        </main>

        {/* history rail (right) — animated timeline */}
        <aside className="history-rail">
          <div className="rail-head">
            <span className="rail-icon">
              <Layers size={18} strokeWidth={2.2} />
            </span>
            <span className="rail-titles">
              <span className="rail-eyebrow">◈ Timeline</span>
              <span className="rail-title">Cache Entries</span>
              <span className="rail-sub">
                {days.length === 0
                  ? "nothing cached yet"
                  : `${history.length} ${history.length === 1 ? "entry" : "entries"} · ${
                      days.length
                    } ${days.length === 1 ? "day" : "days"}`}
              </span>
            </span>
          </div>
          {days.length === 0 ? (
            <div className="rail-empty">
              <CalendarDays size={22} strokeWidth={1.6} />
              <span>No entries yet.</span>
            </div>
          ) : (
            <div className="timeline">
              <span className="tl-spine" aria-hidden />
              {days.map(([key, list], i) => {
                const dsa = list.filter((c) => c.type === "DSA").length;
                const notes = list.length - dsa;
                const active = key === activeDay;
                return (
                  <button
                    key={key}
                    className={`tl-item${active ? " active" : ""}`}
                    style={{ "--i": i } as React.CSSProperties}
                    onClick={() => setActiveDay(active ? null : key)}
                  >
                    <span className="tl-node">
                      <span className="tl-ring" />
                    </span>
                    <span className="tl-card">
                      <span className="tl-top">
                        <span className="tl-label">{labelForDay(key)}</span>
                        <span className="tl-count">{list.length}</span>
                      </span>
                      <span className="tl-pips">
                        {dsa > 0 && (
                          <span className="pip dsa">
                            <span className="pip-dot" />
                            {dsa} DSA
                          </span>
                        )}
                        {notes > 0 && (
                          <span className="pip note">
                            <span className="pip-dot" />
                            {notes} note{notes === 1 ? "" : "s"}
                          </span>
                        )}
                      </span>
                      <ChevronRight className="tl-chev" size={15} />
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </aside>
      </div>

      {node}
    </div>
  );
}

/* ── card ──────────────────────────────────────────────────────────────── */
const CO_LIMIT = 8;
function CardItem({ card, onReview }: { card: CardMsg; onReview: (c: CardMsg, p: boolean) => void }) {
  const isDsa = card.type === "DSA";
  const learned = isDsa ? card.comment : card.back;
  const extra = card.companies.length - CO_LIMIT;
  return (
    <div className="card">
      {/* 1 — heading */}
      <div className="row between">
        <div className="title mono">
          {isDsa && card.url ? (
            <a href={card.url} target="_blank" rel="noreferrer">
              {card.front}
              <ExternalLink size={14} strokeWidth={2.2} />
            </a>
          ) : (
            card.front
          )}
        </div>
        <div className="row">
          <span className={`chip ${isDsa ? "accent" : ""}`}>{card.type}</span>
          <span className="chip">rung {card.intervalIndex}</span>
        </div>
      </div>

      {/* 2 — what I learned */}
      {learned && (
        <div className="learned">
          <div className="learned-label">What I learned</div>
          <div className="learned-body mono">{learned}</div>
        </div>
      )}

      {/* 3 — DSA meta, always below */}
      {isDsa && (card.numCompanies > 0 || card.companies.length > 0) && (
        <div className="dsa-meta">
          {card.numCompanies > 0 && (
            <div className="dsa-meta-label">Asked at {card.numCompanies} companies</div>
          )}
          {card.companies.length > 0 && (
            <div className="companies">
              {card.companies.slice(0, CO_LIMIT).map((co) => (
                <span key={co} className="co">
                  {co}
                </span>
              ))}
              {extra > 0 && <span className="co more">+{extra} more</span>}
            </div>
          )}
        </div>
      )}

      <div className="card-actions">
        <button className="btn pass" onClick={() => onReview(card, true)} title="Recalled it">
          <Check size={16} strokeWidth={2.6} />
          Hit
        </button>
        <button className="btn fail" onClick={() => onReview(card, false)} title="Forgot it">
          <X size={16} strokeWidth={2.6} />
          Miss
        </button>
      </div>
    </div>
  );
}

/* ── history day view (slides in from the right) ───────────────────────── */
function DayView({
  day,
  cards,
  onBack,
}: {
  day: string;
  cards: CardMsg[];
  onBack: () => void;
}) {
  const dsa = cards.filter((c) => c.type === "DSA").length;
  const notes = cards.length - dsa;
  return (
    <div className="dayview">
      <button className="btn back" onClick={onBack}>
        <ArrowLeft size={16} strokeWidth={2.4} />
        Back to today
      </button>
      <div className="page-head">
        <h1 className="big-today">{labelForDay(day)}</h1>
        <div className="rule" />
        <p className="page-sub">
          {cards.length} {cards.length === 1 ? "entry" : "entries"}
          {dsa > 0 && ` · ${dsa} DSA`}
          {notes > 0 && ` · ${notes} note${notes === 1 ? "" : "s"}`}
        </p>
      </div>
      {cards.length === 0 ? (
        <div className="empty">Nothing recorded this day.</div>
      ) : (
        cards.map((c) => <HistoryCard key={String(c.id)} card={c} />)
      )}
    </div>
  );
}

function HistoryCard({ card }: { card: CardMsg }) {
  const isDsa = card.type === "DSA";
  const learned = isDsa ? card.comment : card.back;
  const extra = card.companies.length - CO_LIMIT;
  return (
    <div className="card">
      <div className="row between">
        <div className="title mono">
          {isDsa && card.url ? (
            <a href={card.url} target="_blank" rel="noreferrer">
              {card.front}
              <ExternalLink size={14} strokeWidth={2.2} />
            </a>
          ) : (
            card.front
          )}
        </div>
        <div className="row">
          <span className={`chip ${isDsa ? "accent" : ""}`}>{card.type}</span>
          {card.createdAt && (
            <span className="chip">
              <Clock size={12} strokeWidth={2.2} />
              {timeOf(card.createdAt)}
            </span>
          )}
        </div>
      </div>

      {learned && (
        <div className="learned">
          <div className="learned-label">What I learned</div>
          <div className="learned-body mono">{learned}</div>
        </div>
      )}

      {isDsa && (card.numCompanies > 0 || card.companies.length > 0) && (
        <div className="dsa-meta">
          {card.numCompanies > 0 && (
            <div className="dsa-meta-label">Asked at {card.numCompanies} companies</div>
          )}
          {card.companies.length > 0 && (
            <div className="companies">
              {card.companies.slice(0, CO_LIMIT).map((co) => (
                <span key={co} className="co">
                  {co}
                </span>
              ))}
              {extra > 0 && <span className="co more">+{extra} more</span>}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/* ── solve-a-problem (inline panel: search picker → comment → submit) ───── */
function SolvePanel({
  onDone,
  onError,
}: {
  onDone: () => void;
  onError: (err: unknown) => void;
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<DsaProblem[]>([]);
  const [searching, setSearching] = useState(false);
  const [picked, setPicked] = useState<DsaProblem | null>(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);

  // Debounced catalog search; skip while a problem is already picked.
  useEffect(() => {
    if (picked) return;
    const q = query.trim();
    if (q.length < 2) {
      setResults([]);
      return;
    }
    setSearching(true);
    const h = window.setTimeout(async () => {
      try {
        const res = await cardClient.searchDsaProblems({ query: q, limit: 25 });
        setResults(res.problems);
      } catch (err) {
        onError(err);
      } finally {
        setSearching(false);
      }
    }, 220);
    return () => window.clearTimeout(h);
  }, [query, picked, onError]);

  const submit = async () => {
    if (!picked) return;
    setBusy(true);
    try {
      await cardClient.markDsaProblemDone({ url: picked.url, comment: comment.trim() });
      setPicked(null);
      setComment("");
      setQuery("");
      setResults([]);
      onDone();
    } catch (err) {
      onError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="panel">
      <div className="panel-head">
        <Search size={16} strokeWidth={2.2} />
        <h2>Cache a Solve</h2>
      </div>

      {!picked ? (
        <div className="stack">
          <div className="field">
            <label>Search LeetCode problems by name</label>
            <div className="search-box">
              <Search className="search-ico" size={16} strokeWidth={2.2} />
              <input
                className="input mono"
                placeholder="e.g. trapping rain water"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
              <Loader2
                className={`spin search-spin${searching ? "" : " is-hidden"}`}
                size={16}
              />
            </div>
          </div>
          {results.length > 0 ? (
            <div className="results">
              {results.map((p, i) => (
                <button key={p.url} className="result" onClick={() => setPicked(p)}>
                  <span className="result-idx">{String(i + 1).padStart(2, "0")}</span>
                  <span className="result-main">
                    <span className="name mono">{p.title}</span>
                    <span className="result-co">
                      <Zap size={11} strokeWidth={2.3} />
                      {p.numCompanies} companies
                    </span>
                  </span>
                  <CornerDownRight className="result-pick" size={15} />
                </button>
              ))}
            </div>
          ) : query.trim().length >= 2 && !searching ? (
            <div className="hint">No matches — try another name.</div>
          ) : !searching ? (
            <div className="hint">Type at least 2 characters to search the catalog.</div>
          ) : null}
        </div>
      ) : (
        <div className="stack">
          <div className="picked">
            <span className="picked-check">
              <Check size={14} strokeWidth={3} />
            </span>
            <span className="name mono">{picked.title}</span>
            <span className="chip accent">{picked.numCompanies} companies</span>
            <button
              className="btn subtle"
              onClick={() => {
                setPicked(null);
                setComment("");
              }}
            >
              change
            </button>
          </div>
          <div className="field">
            <label>What I learned — optional</label>
            <textarea
              className="textarea mono"
              autoFocus
              placeholder="Approach, gotchas, complexity…"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </div>
          <button className="btn block" onClick={submit} disabled={busy}>
            {busy ? <Loader2 className="spin" size={16} /> : <Check size={16} strokeWidth={2.4} />}
            Cache this solve
          </button>
        </div>
      )}
    </section>
  );
}
