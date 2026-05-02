import { useEffect, useState } from 'react';
import { api } from '../../api/client.js';

export default function VideoBlock({ block }) {
  const [video, setVideo] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.getVideo(block.query)
      .then((result) => active && setVideo(result))
      .catch((err) => active && setError(err.message));
    return () => {
      active = false;
    };
  }, [block.query]);

  return (
    <section className="video-block">
      <p className="tagline">Suggested video</p>
      {video?.embedUrl ? (
        <iframe
          title={video.title}
          src={video.embedUrl}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        />
      ) : (
        <a href={video?.sourceUrl || `https://www.youtube.com/results?search_query=${encodeURIComponent(block.query)}`} target="_blank" rel="noreferrer">
          Search YouTube for "{block.query}"
        </a>
      )}
      {error && <small>{error}</small>}
    </section>
  );
}

