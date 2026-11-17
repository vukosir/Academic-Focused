import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

const STORAGE_KEY = "indulge.order-bag.v1";
const BagContext = createContext(null);

export function BagProvider({ children }) {
  const [items, setItems] = useState([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) setItems(parsed);
      }
    } catch {
      /* unreadable bag: start empty */
    }
    setReady(true);
  }, []);

  useEffect(() => {
    if (!ready) return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      /* storage full or blocked: the bag still works for this session */
    }
  }, [items, ready]);

  const add = useCallback((item) => {
    setItems((prev) => {
      const key = `${item.slug}|${item.option}|${item.palette}`;
      const existing = prev.find((entry) => entry.key === key);
      if (existing) {
        return prev.map((entry) => (entry.key === key ? { ...entry, quantity: Math.min(20, entry.quantity + (item.quantity || 1)) } : entry));
      }
      if (prev.length >= 10) return prev;
      return [...prev, { ...item, key, quantity: item.quantity || 1 }];
    });
  }, []);

  const remove = useCallback((key) => {
    setItems((prev) => prev.filter((entry) => entry.key !== key));
  }, []);

  const setQuantity = useCallback((key, quantity) => {
    setItems((prev) =>
      prev.map((entry) => (entry.key === key ? { ...entry, quantity: Math.min(20, Math.max(1, quantity)) } : entry)).filter((entry) => entry.quantity > 0)
    );
  }, []);

  const clear = useCallback(() => setItems([]), []);

  const count = useMemo(() => items.reduce((total, entry) => total + entry.quantity, 0), [items]);

  const value = useMemo(() => ({ items, count, add, remove, setQuantity, clear, ready }), [items, count, add, remove, setQuantity, clear, ready]);

  return <BagContext.Provider value={value}>{children}</BagContext.Provider>;
}

export function useBag() {
  const context = useContext(BagContext);
  if (!context) throw new Error("useBag must be used inside BagProvider");
  return context;
}
