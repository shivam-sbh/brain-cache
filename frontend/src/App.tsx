import { useCallback, useEffect, useState } from "react";
import { ConnectError } from "@connectrpc/connect";
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  IconButton,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
} from "@mui/material";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import CancelRoundedIcon from "@mui/icons-material/CancelRounded";
import AddRoundedIcon from "@mui/icons-material/AddRounded";
import LogoutRoundedIcon from "@mui/icons-material/LogoutRounded";
import PsychologyRoundedIcon from "@mui/icons-material/PsychologyRounded";
import { authClient, cardClient, getToken, setToken, clearToken } from "./client";
import type { Card as CardMsg } from "./gen/brain_cache_pb";

function Wordmark({ size = 22 }: { size?: number }) {
  return (
    <Typography
      component="span"
      sx={{ fontFamily: '"Google Sans", sans-serif', fontWeight: 500, fontSize: size, letterSpacing: -0.5 }}
    >
      <span className="g-blue">B</span>
      <span className="g-red">r</span>
      <span className="g-yellow">a</span>
      <span className="g-blue">i</span>
      <span className="g-green">n</span>
      <span style={{ color: "#5f6368" }}> Cache</span>
    </Typography>
  );
}

export function App() {
  const [token, setTok] = useState<string | null>(getToken());
  const [email, setEmailState] = useState<string | null>(null);

  if (!token) {
    return (
      <Auth
        onAuthed={(t, e) => {
          setToken(t);
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
        clearToken();
        setTok(null);
      }}
    />
  );
}

function useErr() {
  const [error, setError] = useState<string | null>(null);
  const capture = (err: unknown) =>
    setError(err instanceof ConnectError ? err.rawMessage : String(err));
  const node = (
    <Snackbar
      open={!!error}
      autoHideDuration={5000}
      onClose={() => setError(null)}
      anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
    >
      <Alert severity="error" variant="filled" onClose={() => setError(null)}>
        {error}
      </Alert>
    </Snackbar>
  );
  return { capture, node };
}

function Auth({ onAuthed }: { onAuthed: (token: string, email: string) => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState<"login" | "register">("login");
  const [busy, setBusy] = useState(false);
  const { capture, node } = useErr();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      const call = mode === "login" ? authClient.login : authClient.register;
      const res = await call({ email, password });
      onAuthed(res.token, res.email);
    } catch (err) {
      capture(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        background: "radial-gradient(1200px 600px at 50% -10%, #e8f0fe 0%, #ffffff 60%)",
        px: 2,
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: "100%",
          maxWidth: 420,
          p: { xs: 3, sm: 5 },
          border: "1px solid #dadce0",
          borderRadius: 4,
        }}
      >
        <Stack spacing={1} alignItems="center" mb={3}>
          <Avatar sx={{ bgcolor: "#e8f0fe", color: "#1a73e8", width: 56, height: 56 }}>
            <PsychologyRoundedIcon fontSize="large" />
          </Avatar>
          <Wordmark size={26} />
          <Typography variant="h5" sx={{ mt: 1 }}>
            {mode === "login" ? "Sign in" : "Create account"}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            to keep your knowledge cached
          </Typography>
        </Stack>

        <Box component="form" onSubmit={submit}>
          <Stack spacing={2.5}>
            <TextField
              label="Email"
              type="email"
              fullWidth
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <TextField
              label="Password"
              type="password"
              fullWidth
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Button
                variant="text"
                onClick={() => setMode(mode === "login" ? "register" : "login")}
              >
                {mode === "login" ? "Create account" : "Have an account?"}
              </Button>
              <Button type="submit" variant="contained" disabled={busy}>
                {busy ? <CircularProgress size={20} color="inherit" /> : mode === "login" ? "Sign in" : "Sign up"}
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Paper>
      {node}
    </Box>
  );
}

function Dashboard({ email, onLogout }: { email: string | null; onLogout: () => void }) {
  const [cards, setCards] = useState<CardMsg[]>([]);
  const [loading, setLoading] = useState(true);
  const [front, setFront] = useState("");
  const [back, setBack] = useState("");
  const { capture, node } = useErr();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await cardClient.listDueCards({});
      setCards(res.cards);
    } catch (err) {
      capture(err);
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!front.trim()) return;
    try {
      await cardClient.createCard({ front, back });
      setFront("");
      setBack("");
      await load();
    } catch (err) {
      capture(err);
    }
  };

  const review = async (card: CardMsg, passed: boolean) => {
    try {
      await cardClient.reviewCard({ cardId: card.id, passed });
      setCards((cs) => cs.filter((c) => c.id !== card.id));
    } catch (err) {
      capture(err);
    }
  };

  const initial = (email ?? "?").charAt(0).toUpperCase();

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "#f8f9fa" }}>
      <AppBar position="sticky">
        <Toolbar>
          <Stack direction="row" spacing={1.5} alignItems="center" sx={{ flexGrow: 1 }}>
            <PsychologyRoundedIcon sx={{ color: "#1a73e8" }} />
            <Wordmark />
          </Stack>
          <Tooltip title={email ?? ""}>
            <Avatar sx={{ bgcolor: "#1a73e8", width: 32, height: 32, fontSize: 16, mr: 1 }}>
              {initial}
            </Avatar>
          </Tooltip>
          <Tooltip title="Log out">
            <IconButton onClick={onLogout} size="small">
              <LogoutRoundedIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={0} sx={{ p: 3, mb: 4, border: "1px solid #dadce0" }}>
          <Typography variant="h6" mb={2}>
            New card
          </Typography>
          <Box component="form" onSubmit={create}>
            <Stack spacing={2}>
              <TextField
                label="Front — prompt"
                fullWidth
                value={front}
                onChange={(e) => setFront(e.target.value)}
              />
              <TextField
                label="Back — answer"
                fullWidth
                multiline
                minRows={2}
                value={back}
                onChange={(e) => setBack(e.target.value)}
              />
              <Box>
                <Button type="submit" variant="contained" startIcon={<AddRoundedIcon />}>
                  Add card
                </Button>
              </Box>
            </Stack>
          </Box>
        </Paper>

        <Stack direction="row" alignItems="center" spacing={1.5} mb={2}>
          <Typography variant="h5">Due now</Typography>
          <Chip label={cards.length} color="primary" size="small" />
        </Stack>

        {loading ? (
          <Box sx={{ display: "grid", placeItems: "center", py: 6 }}>
            <CircularProgress />
          </Box>
        ) : cards.length === 0 ? (
          <Paper
            elevation={0}
            sx={{ p: 6, textAlign: "center", border: "1px dashed #dadce0", color: "text.secondary" }}
          >
            <PsychologyRoundedIcon sx={{ fontSize: 48, opacity: 0.4 }} />
            <Typography mt={1}>All caught up. Nothing due right now.</Typography>
          </Paper>
        ) : (
          <Stack spacing={1.5}>
            {cards.map((card) => (
              <Card
                key={String(card.id)}
                elevation={0}
                sx={{
                  border: "1px solid #dadce0",
                  transition: "box-shadow .2s, transform .2s",
                  "&:hover": { boxShadow: "0 1px 3px rgba(60,64,67,.3), 0 4px 8px rgba(60,64,67,.15)" },
                }}
              >
                <CardContent>
                  <Stack direction="row" spacing={1} mb={1}>
                    <Chip
                      label={card.type}
                      size="small"
                      color={card.type === "DSA" ? "secondary" : "default"}
                      variant="outlined"
                    />
                    <Chip label={`rung ${card.intervalIndex}`} size="small" variant="outlined" />
                  </Stack>
                  <Typography variant="subtitle1" fontWeight={500}>
                    {card.front}
                  </Typography>
                  {card.back && (
                    <>
                      <Divider sx={{ my: 1 }} />
                      <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: "pre-wrap" }}>
                        {card.back}
                      </Typography>
                    </>
                  )}
                </CardContent>
                <CardActions sx={{ px: 2, pb: 2 }}>
                  <Button
                    variant="contained"
                    color="success"
                    startIcon={<CheckCircleRoundedIcon />}
                    onClick={() => review(card, true)}
                  >
                    Pass
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    startIcon={<CancelRoundedIcon />}
                    onClick={() => review(card, false)}
                  >
                    Fail
                  </Button>
                </CardActions>
              </Card>
            ))}
          </Stack>
        )}
      </Container>
      {node}
    </Box>
  );
}
