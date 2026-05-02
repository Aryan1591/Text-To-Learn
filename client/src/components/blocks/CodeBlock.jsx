export default function CodeBlock({ block }) {
  return (
    <div className="code-block">
      <span>{block.language || 'code'}</span>
      <pre><code>{block.text}</code></pre>
    </div>
  );
}

