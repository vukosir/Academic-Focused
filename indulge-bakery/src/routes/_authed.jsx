import { createFileRoute, Outlet } from "@tanstack/react-router";
import { AuthGateError, requireSignedIn } from "@/components/auth/AuthGate";
import { BagProvider } from "@/components/site/BagProvider";

// Signed-in pages. The owner's inbox (/admin) draws its own dark layout;
// My orders (/dashboard) uses the site header and footer.
export const Route = createFileRoute("/_authed")({
  ssr: false,
  beforeLoad: requireSignedIn,
  errorComponent: AuthGateError,
  component: AuthedLayout,
});

function AuthedLayout() {
  return (
    <BagProvider>
      <Outlet />
    </BagProvider>
  );
}
