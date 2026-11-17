import { QueryClientProvider } from "@tanstack/react-query";
import { createRouter } from "@tanstack/react-router";
import { createIsomorphicFn, getGlobalStartContext } from "@tanstack/react-start";
import { routeTree } from "./routeTree.gen";
import { DefaultCatchBoundary } from "@/components/DefaultCatchBoundary";
import { NotFound } from "@/components/NotFound";
import { createQueryClient } from "@/lib/query-client";

// The per-request CSP nonce set by the security middleware (src/start.js). The router
// stamps it on every script it renders, so they pass the Content Security Policy.
const getNonce = createIsomorphicFn()
  .server(() => {
    try {
      return getGlobalStartContext()?.nonce;
    } catch {
      return undefined;
    }
  })
  .client(() => undefined);

export function getRouter() {
  const queryClient = createQueryClient();
  return createRouter({
    routeTree,
    context: { queryClient },
    defaultPreload: "intent",
    defaultPreloadStaleTime: 0,
    defaultErrorComponent: DefaultCatchBoundary,
    defaultNotFoundComponent: () => <NotFound />,
    scrollRestoration: true,
    ssr: { nonce: getNonce() },
    Wrap: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
  });
}
