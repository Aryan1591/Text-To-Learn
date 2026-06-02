import { Link, Outlet, useLocation } from 'react-router-dom';
import { BookOpenText, GraduationCap, LogIn, LogOut, Sparkles } from 'lucide-react';
import { useAuth0 } from '@auth0/auth0-react';
import { useState } from 'react';

function AuthActions({ authConfigured }) {
  if (!authConfigured) {
    return <span className="auth-pill">Guest mode</span>;
  }

  return <ConfiguredAuthActions />;
}

function ConfiguredAuthActions() {
  const { isAuthenticated, isLoading, loginWithRedirect, logout } = useAuth0();
  const [authError, setAuthError] = useState('');
  const [redirecting, setRedirecting] = useState(false);

  async function handleLogin() {
    setAuthError('');
    setRedirecting(true);
    try {
      await loginWithRedirect({
        appState: { returnTo: window.location.pathname },
        authorizationParams: { prompt: 'login' },
      });
    } catch (error) {
      setAuthError(error?.message || 'Unable to start login. Please check Auth0 settings.');
      setRedirecting(false);
    }
  }

  if (isLoading) {
    return <span className="auth-pill">Auth loading...</span>;
  }

  if (!isAuthenticated) {
    return (
      <div>
        <button type="button" className="ghost-button" onClick={handleLogin} disabled={redirecting}>
          <LogIn size={16} /> {redirecting ? 'Redirecting...' : 'Login'}
        </button>
        {authError ? <small className="auth-error">{authError}</small> : null}
      </div>
    );
  }

  return (
    <button className="ghost-button" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
      <LogOut size={16} /> Logout
    </button>
  );
}

export default function Layout({ authConfigured }) {
  const location = useLocation();
  function scrollToRecent() {
    const el = document.getElementById('recent');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="brand" to="/">
          <span className="brand-mark"><GraduationCap size={26} /></span>
          <span>
            <strong>Text-to-Learn</strong>
            <small>AI course studio</small>
          </span>
        </Link>

        <nav className="nav-stack">
          <Link className={location.pathname === '/' ? 'active' : ''} to="/">
            <Sparkles size={18} /> Generate
          </Link>
          <button type="button" className="nav-link-button" onClick={scrollToRecent}>
            <BookOpenText size={18} /> Recent courses
          </button>
        </nav>
      </aside>

      <main className="content-area">
        <header className="topbar">
          <div>
            <span className="eyebrow">Prompt in. Course out.</span>
            <h1>Build a syllabus in seconds</h1>
          </div>
          <AuthActions authConfigured={authConfigured} />
        </header>
        <Outlet />
      </main>
    </div>
  );
}
