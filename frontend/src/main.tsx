import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import "@fontsource/google-sans/400.css";
import "@fontsource/google-sans/500.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import { App } from "./App";
import { theme } from "./theme";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </StrictMode>,
);
