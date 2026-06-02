import HeadingBlock from './blocks/HeadingBlock.jsx';
import ParagraphBlock from './blocks/ParagraphBlock.jsx';
import CodeBlock from './blocks/CodeBlock.jsx';
import VideoBlock from './blocks/VideoBlock.jsx';
import MCQBlock from './blocks/MCQBlock.jsx';

export default function LessonRenderer({ lesson }) {
  if (!lesson) return null;
  const seenMcq = new Set();
  const seenText = new Set();
  let videoSeen = false;
  const sanitizedContent = (lesson.content || []).filter((block) => {
    if (!block || !block.type) return false;
    const type = block.type.toLowerCase();
    if (type === 'mcq') {
      const key = (block.question || '').trim().toLowerCase();
      if (!key || seenMcq.has(key)) return false;
      seenMcq.add(key);
      return true;
    }
    if (type === 'heading' || type === 'paragraph') {
      const key = `${type}|${(block.text || '').trim().toLowerCase()}`;
      if (!block.text || seenText.has(key)) return false;
      seenText.add(key);
      return true;
    }
    if (type === 'video') {
      if (videoSeen) return false;
      videoSeen = true;
      return true;
    }
    return true;
  });

  return (
    <article className="lesson-document">
      <section className="objectives">
        <h3>Objectives</h3>
        <ul>
          {lesson.objectives?.map((objective) => (
            <li key={objective}>{objective}</li>
          ))}
        </ul>
      </section>

      {sanitizedContent.map((block, index) => {
        const key = `${block.type}-${index}`;
        switch (block.type) {
          case 'heading':
            return <HeadingBlock key={key} block={block} />;
          case 'paragraph':
            return <ParagraphBlock key={key} block={block} />;
          case 'code':
            return <CodeBlock key={key} block={block} />;
          case 'video':
            return <VideoBlock key={key} block={block} />;
          case 'mcq':
            return <MCQBlock key={key} block={block} />;
          default:
            return <ParagraphBlock key={key} block={{ text: block.text || JSON.stringify(block) }} />;
        }
      })}
    </article>
  );
}
