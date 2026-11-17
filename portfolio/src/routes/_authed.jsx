import { createFileRoute, Outlet } from "@tanstack/react-router";
import { AuthGateError, requireSignedIn } from "@/components/auth/AuthGate";

// Signed-in pages. The messages inbox (/admin) draws its own dark layout;
// the account page (/account) uses the site header and footer.
export const Route = createFileRoute("/_authed")({
  ssr: false,
  beforeLoad: requireSignedIn,
  errorComponent: AuthGateError,
  component: Outlet,
});
