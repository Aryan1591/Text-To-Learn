export default function ParagraphBlock({ block }) {
  return <p className={block.text?.startsWith('Hinglish') ? 'hinglish-note' : 'lesson-paragraph'}>{block.text}</p>;
}

