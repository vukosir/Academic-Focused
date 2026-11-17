import { createFileRoute, Outlet } from "@tanstack/react-router";
import { BagProvider } from "@/components/site/BagProvider";
import SiteHeader from "@/components/site/SiteHeader";
import SiteFooter from "@/components/site/SiteFooter";

export const Route = createFileRoute("/_site")({
  component: SiteLayout,
});

function SiteLayout() {
  return (
    <BagProvider>
      <div className="flex min-h-screen flex-col bg-background">
        <SiteHeader />
        <main id="main" className="flex-1">
          <Outlet />
        </main>
        <SiteFooter />
      </div>
    </BagProvider>
  );
}
