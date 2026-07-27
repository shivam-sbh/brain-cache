import { Code, ConnectError, createClient, type Interceptor } from "@connectrpc/connect";
import { createGrpcWebTransport } from "@connectrpc/connect-web";
import { AuthService, CardService } from "./gen/brain_cache_pb";

// JWT + email live in localStorage. The backend interceptor reads
// "authorization: Bearer <jwt>". Email is cached so the dashboard survives a
// page reload (the token alone doesn't tell the UI who we are).
const TOKEN_KEY = "bc.token";
const EMAIL_KEY = "bc.email";
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t);
export const getEmail = () => localStorage.getItem(EMAIL_KEY);
export const setEmail = (e: string) => localStorage.setItem(EMAIL_KEY, e);
export const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EMAIL_KEY);
};

// Attach the bearer token to every request, if we have one.
const authInterceptor: Interceptor = (next) => async (req) => {
  const token = getToken();
  if (token) {
    req.header.set("authorization", `Bearer ${token}`);
  }
  return next(req);
};

// If the server rejects us as unauthenticated (expired/invalid JWT), the token
// is dead — drop the session and bounce back to the sign-in screen instead of
// leaving the user stuck on a dashboard that can never load.
const expiryInterceptor: Interceptor = (next) => async (req) => {
  try {
    return await next(req);
  } catch (err) {
    if (err instanceof ConnectError && err.code === Code.Unauthenticated) {
      clearSession();
      location.reload();
    }
    throw err;
  }
};

// Browsers can't speak native gRPC; this is gRPC-Web, terminated by Envoy which
// forwards to the Spring gRPC server on :9090.
const transport = createGrpcWebTransport({
  baseUrl: import.meta.env.VITE_API_BASE ?? "http://localhost:8081",
  interceptors: [authInterceptor, expiryInterceptor],
});

export const authClient = createClient(AuthService, transport);
export const cardClient = createClient(CardService, transport);
