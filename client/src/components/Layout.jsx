import { Link, Outlet, useLocation } from 'react-router-dom';
import { BookOpenText, GraduationCap, LogIn, LogOut, Sparkles } from 'lucide-react';
import { useAuth0 } from '@auth0/auth0-react';

function AuthActions({ authConfigured }) {
  if (!authConfigured) {
    return <span className="auth-pill">Guest mode</span>;
  }

  return <ConfiguredAuthActions />;
}

function ConfiguredAuthActions() {
  const { isAuthenticated, loginWithRedirect, logout, user } = useAuth0();
  if (!isAuthenticated) {
    return (
      <button className="ghost-button" onClick={() => loginWithRedirect()}>
        <LogIn size={16} /> Login
      </button>
    );
  }

  return (
    <button className="ghost-button" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
      <LogOut size={16} /> {user?.name || 'Logout'}
    </button>
  );
}

export default function Layout({ authConfigured }) {
  const location = useLocation();

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
          <a href="#recent">
            <BookOpenText size={18} /> Recent courses
          </a>
        </nav>

        <div className="sidebar-card">
          <p>Hackathon-ready stack</p>
          <span>React + Spring Boot + MongoDB + Auth0</span>
        </div>
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
