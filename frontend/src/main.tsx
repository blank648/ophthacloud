import { createRoot } from "react-dom/client";
import { useEffect, useState } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import App from "./App.tsx";
import "./index.css";
import { queryClient } from "@/lib/queryClient";
import { initKeycloak } from "@/lib/auth";

const Root = () => {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    initKeycloak()
      .catch((e) => console.warn("Keycloak unavailable, continuing in demo mode", e))
      .finally(() => setReady(true));
  }, []);

  if (!ready) return null;

  return (
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  );
};

createRoot(document.getElementById("root")!).render(<Root />);
