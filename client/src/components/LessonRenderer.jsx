import HeadingBlock from './blocks/HeadingBlock.jsx';
import ParagraphBlock from './blocks/ParagraphBlock.jsx';
import CodeBlock from './blocks/CodeBlock.jsx';
import VideoBlock from './blocks/VideoBlock.jsx';
import MCQBlock from './blocks/MCQBlock.jsx';

export default function LessonRenderer({ lesson }) {
  if (!lesson) return null;

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

      {lesson.content?.map((block, index) => {
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

