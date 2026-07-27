import { createTheme } from "@mui/material/styles";

// Google-flavoured Material theme: Google Blue primary, Google Sans display font,
// Roboto body, pill buttons, soft surfaces — the Material You / Workspace look.
export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1a73e8", dark: "#1765cc", light: "#4285f4" },
    error: { main: "#d93025" },
    success: { main: "#188038" },
    background: { default: "#ffffff", paper: "#ffffff" },
    text: { primary: "#202124", secondary: "#5f6368" },
    divider: "#dadce0",
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: '"Roboto", "Google Sans", system-ui, sans-serif',
    h1: { fontFamily: '"Google Sans", "Roboto", sans-serif' },
    h2: { fontFamily: '"Google Sans", "Roboto", sans-serif' },
    h3: { fontFamily: '"Google Sans", "Roboto", sans-serif' },
    h4: { fontFamily: '"Google Sans", "Roboto", sans-serif', fontWeight: 500 },
    h5: { fontFamily: '"Google Sans", "Roboto", sans-serif', fontWeight: 500 },
    h6: { fontFamily: '"Google Sans", "Roboto", sans-serif', fontWeight: 500 },
    button: { fontFamily: '"Google Sans", "Roboto", sans-serif', textTransform: "none", fontWeight: 500 },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 100, paddingInline: 24, height: 40 },
      },
    },
    MuiTextField: {
      defaultProps: { variant: "outlined", size: "small" },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: { borderRadius: 16 },
      },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: "default" },
      styleOverrides: {
        root: {
          backgroundColor: "#ffffff",
          borderBottom: "1px solid #dadce0",
        },
      },
    },
  },
});
