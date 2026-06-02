import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Auth0Provider } from '@auth0/auth0-react';
import App from './App.jsx';
import AppErrorBoundary from './components/AppErrorBoundary.jsx';
import './index.css';

const domain = import.meta.env.VITE_AUTH0_DOMAIN;
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID;
const audience = import.meta.env.VITE_AUTH0_AUDIENCE;
const authConfigured = domain && clientId && !domain.includes('your-domain') && !clientId.includes('your-client');

const app = (
  <BrowserRouter>
    <App authConfigured={authConfigured} />
  </BrowserRouter>
);

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {authConfigured ? (
      <Auth0Provider
        domain={domain}
        clientId={clientId}
        cacheLocation="localstorage"
        useRefreshTokens
        authorizationParams={{
          redirect_uri: window.location.origin,
          audience,
        }}
      >
        <AppErrorBoundary>{app}</AppErrorBoundary>
      </Auth0Provider>
    ) : <AppErrorBoundary>{app}</AppErrorBoundary>}
  </React.StrictMode>,
);
