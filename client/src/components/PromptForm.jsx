import { useState } from 'react';
import { Wand2 } from 'lucide-react';

export default function PromptForm({ onGenerate, loading }) {
  const [topic, setTopic] = useState('Intro to React Hooks for beginners');
  const [learningLevel, setLearningLevel] = useState('Beginner');
  const [languagePreference, setLanguagePreference] = useState('English + Hinglish support');

  function handleSubmit(event) {
    event.preventDefault();
    onGenerate({ topic, learningLevel, languagePreference });
  }

  return (
    <form className="prompt-card" onSubmit={handleSubmit}>
      <label htmlFor="topic">What do you want to learn?</label>
      <textarea
        id="topic"
        value={topic}
        onChange={(event) => setTopic(event.target.value)}
        minLength={3}
        maxLength={160}
        placeholder="Basics of Copyright Law, Java Streams, Machine Learning..."
        required
      />
      <div className="form-grid">
        <label>
          Level
          <select value={learningLevel} onChange={(event) => setLearningLevel(event.target.value)}>
            <option>Beginner</option>
            <option>Intermediate</option>
            <option>Advanced</option>
            <option>Beginner to intermediate</option>
          </select>
        </label>
        <label>
          Language support
          <select value={languagePreference} onChange={(event) => setLanguagePreference(event.target.value)}>
            <option>English + Hinglish support</option>
            <option>English only</option>
            <option>Hindi explanations</option>
          </select>
        </label>
      </div>
      <button className="primary-button" disabled={loading}>
        <Wand2 size={18} /> {loading ? 'Generating course...' : 'Generate course'}
      </button>
    </form>
  );
}

