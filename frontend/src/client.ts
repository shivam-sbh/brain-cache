import { createClient, type Interceptor } from "@connectrpc/connect";
import { createGrpcWebTransport } from "@connectrpc/connect-web";
import { AuthService, CardService } from "./gen/brain_cache_pb";

// JWT lives in localStorage. The backend interceptor reads "authorization: Bearer <jwt>".
const TOKEN_KEY = "bc.token";
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

// Attach the bearer token to every request, if we have one.
const authInterceptor: Interceptor = (next) => async (req) => {
  const token = getToken();
  if (token) {
    req.header.set("authorization", `Bearer ${token}`);
  }
  return next(req);
};

// Browsers can't speak native gRPC; this is gRPC-Web, terminated by Envoy which
// forwards to the Spring gRPC server on :9090.
const transport = createGrpcWebTransport({
  baseUrl: import.meta.env.VITE_API_BASE ?? "http://localhost:8081",
  interceptors: [authInterceptor],
});

export const authClient = createClient(AuthService, transport);
export const cardClient = createClient(CardService, transport);
