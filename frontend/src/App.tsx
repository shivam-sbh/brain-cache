import { useCallback, useEffect, useState } from "react";
import { ConnectError } from "@connectrpc/connect";
import { authClient, cardClient, getToken, setToken, clearToken } from "./client";
import type { Card } from "./gen/brain_cache_pb";

export function App() {
  const [token, setTok] = useState<string | null>(getToken());

  if (!token) {
    return <Auth onAuthed={(t) => { setToken(t); setTok(t); }} />;
  }
  return <Dashboard onLogout={() => { clearToken(); setTok(null); }} />;
}

function Auth({ onAuthed }: { onAuthed: (token: string) => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const call = mode === "login" ? authClient.login : authClient.register;
      const res = await call({ email, password });
      onAuthed(res.token);
    } catch (err) {
      setError(err instanceof ConnectError ? err.rawMessage : String(err));
    }
  };

  return (
    <>
      <h1>Brain Cache</h1>
      <form onSubmit={submit}>
        <input placeholder="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input placeholder="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">{mode === "login" ? "Log in" : "Register"}</button>
      </form>
      <p className="muted">
        {mode === "login" ? "No account?" : "Have an account?"}{" "}
        <a href="#" onClick={(e) => { e.preventDefault(); setMode(mode === "login" ? "register" : "login"); }}>
          {mode === "login" ? "Register" : "Log in"}
        </a>
      </p>
      {error && <p className="error">{error}</p>}
    </>
  );
}

function Dashboard({ onLogout }: { onLogout: () => void }) {
  const [cards, setCards] = useState<Card[]>([]);
  const [front, setFront] = useState("");
  const [back, setBack] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const res = await cardClient.listDueCards({});
      setCards(res.cards);
    } catch (err) {
      setError(err instanceof ConnectError ? err.rawMessage : String(err));
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await cardClient.createCard({ front, back });
      setFront(""); setBack("");
      await load();
    } catch (err) {
      setError(err instanceof ConnectError ? err.rawMessage : String(err));
    }
  };

  const review = async (card: Card, passed: boolean) => {
    try {
      await cardClient.reviewCard({ cardId: card.id, passed });
      await load();
    } catch (err) {
      setError(err instanceof ConnectError ? err.rawMessage : String(err));
    }
  };

  return (
    <>
      <h1>Brain Cache</h1>
      <button onClick={onLogout}>Log out</button>

      <h2>New card</h2>
      <form onSubmit={create}>
        <input placeholder="front (prompt)" value={front} onChange={(e) => setFront(e.target.value)} />
        <textarea placeholder="back (answer)" value={back} onChange={(e) => setBack(e.target.value)} />
        <button type="submit">Add card</button>
      </form>

      <h2>Due now ({cards.length})</h2>
      {cards.length === 0 && <p className="muted">Nothing due. Nice.</p>}
      {cards.map((card) => (
        <div className="card" key={String(card.id)}>
          <strong>{card.front}</strong>
          <div className="muted">{card.back}</div>
          <div className="muted">rung {card.intervalIndex} · {card.type}</div>
          <button onClick={() => review(card, true)}>Pass</button>{" "}
          <button onClick={() => review(card, false)}>Fail</button>
        </div>
      ))}

      {error && <p className="error">{error}</p>}
    </>
  );
}
