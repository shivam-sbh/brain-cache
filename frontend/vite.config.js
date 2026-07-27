import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
// Dev server on :5173. The gRPC-Web endpoint (Envoy) is reached directly via
// VITE_API_BASE (default http://localhost:8081), not proxied here.
export default defineConfig({
    plugins: [react()],
});
