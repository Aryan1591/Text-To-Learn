export default function LoadingSpinner({ label = 'Loading...' }) {
  return (
    <div className="loading-box">
      <span className="spinner" />
      <p>{label}</p>
    </div>
  );
}

