import { useEffect, useRef, useState } from "react";

/**
 * Scroll reveal: content fades and rises in once, the first time it enters the
 * viewport. A fail-safe shows the content after a moment even if the observer
 * never fires, so nothing can stay invisible.
 */
export default function Reveal({ children, delay = 0, className }) {
  const ref = useRef(null);
  const [shown, setShown] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return undefined;

    const show = () => setShown(true);

    if (typeof IntersectionObserver === "undefined") {
      show();
      return undefined;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          show();
          observer.disconnect();
        }
      },
      { rootMargin: "0px 0px -40px 0px", threshold: 0.01 }
    );
    observer.observe(element);

    const failSafe = window.setTimeout(show, 1600);

    return () => {
      observer.disconnect();
      window.clearTimeout(failSafe);
    };
  }, []);

  const ease = "cubic-bezier(0.2, 0.7, 0.2, 1)";

  return (
    <div
      ref={ref}
      className={className}
      style={{
        opacity: shown ? 1 : 0,
        transform: shown ? "translateY(0)" : "translateY(26px)",
        transition: `opacity 0.8s ${ease} ${delay}s, transform 0.8s ${ease} ${delay}s`,
        willChange: "opacity, transform",
      }}
    >
      {children}
    </div>
  );
}
