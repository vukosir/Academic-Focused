import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// The wdth build carries both the weight and the width axis, which the display type uses.
import '@fontsource-variable/bricolage-grotesque/wdth.css';
import '@fontsource-variable/hanken-grotesk';
import './styles/tokens.css';
import './styles/base.css';
import './styles/scenes.css';
import './styles/layout.css';
import './styles/components.css';
import App from './App';
import { AppErrorBoundary } from './components/AppErrorBoundary';
import { AppStateProvider } from './state/AppState';

const root = document.getElementById('root');
if (!root) throw new Error('Missing #root element in index.html');

createRoot(root).render(
  <StrictMode>
    <AppErrorBoundary>
      <AppStateProvider>
        <App />
      </AppStateProvider>
    </AppErrorBoundary>
  </StrictMode>,
);
