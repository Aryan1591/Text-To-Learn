import { useState } from 'react';

export default function MCQBlock({ block }) {
  const [selected, setSelected] = useState(null);
  const answered = selected !== null;

  return (
    <section className="mcq-block">
      <h4>{block.question}</h4>
      <div className="mcq-options">
        {block.options?.map((option, index) => (
          <button
            key={option}
            className={answered && index === block.answer ? 'correct' : answered && index === selected ? 'incorrect' : ''}
            onClick={() => setSelected(index)}
          >
            {option}
          </button>
        ))}
      </div>
      {answered && <p>{block.explanation}</p>}
    </section>
  );
}

