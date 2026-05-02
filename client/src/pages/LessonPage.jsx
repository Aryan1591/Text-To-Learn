import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { api } from '../api/client.js';
import ErrorMessage from '../components/ErrorMessage.jsx';
import LessonPDFExporter from '../components/LessonPDFExporter.jsx';
import LessonRenderer from '../components/LessonRenderer.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';
import { getLesson } from '../utils/course.js';

export default function LessonPage() {
  const { courseId, moduleIndex, lessonIndex } = useParams();
  const [course, setCourse] = useState(null);
  const [error, setError] = useState('');
  const lessonRef = useRef(null);

  useEffect(() => {
    api.getCourse(courseId)
      .then(setCourse)
      .catch((err) => setError(err.message));
  }, [courseId]);

  if (error) return <ErrorMessage message={error} />;
  if (!course) return <LoadingSpinner label="Preparing lesson..." />;

  const { module, lesson } = getLesson(course, moduleIndex, lessonIndex);
  if (!lesson) return <ErrorMessage message="Lesson not found." />;

  return (
    <section className="lesson-page">
      <div className="lesson-toolbar">
        <Link className="ghost-button" to={`/course/${course.id}`}>
          <ArrowLeft size={16} /> Back to course
        </Link>
        <LessonPDFExporter targetRef={lessonRef} fileName={`${lesson.title}.pdf`} />
      </div>

      <div className="lesson-shell" ref={lessonRef}>
        <p className="eyebrow">{module?.title}</p>
        <h2>{lesson.title}</h2>
        <LessonRenderer lesson={lesson} />
      </div>
    </section>
  );
}

