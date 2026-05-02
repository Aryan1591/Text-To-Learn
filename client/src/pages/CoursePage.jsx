import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, BookOpenCheck } from 'lucide-react';
import { api } from '../api/client.js';
import ErrorMessage from '../components/ErrorMessage.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';

export default function CoursePage() {
  const { courseId } = useParams();
  const [course, setCourse] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getCourse(courseId)
      .then(setCourse)
      .catch((err) => setError(err.message));
  }, [courseId]);

  if (error) return <ErrorMessage message={error} />;
  if (!course) return <LoadingSpinner label="Opening course..." />;

  return (
    <section className="course-overview">
      <div className="course-hero">
        <p className="eyebrow">Generated syllabus</p>
        <h2>{course.title}</h2>
        <p>{course.description}</p>
        <div className="tag-row">
          {course.tags?.map((tag) => <span key={tag}>{tag}</span>)}
        </div>
      </div>

      <div className="module-stack">
        {course.modules?.map((module, moduleIndex) => (
          <article className="module-card" key={module.id}>
            <div className="module-title">
              <BookOpenCheck />
              <div>
                <span>Module {moduleIndex + 1}</span>
                <h3>{module.title}</h3>
                <p>{module.summary}</p>
              </div>
            </div>
            <div className="lesson-list">
              {module.lessons?.map((lesson, lessonIndex) => (
                <Link key={lesson.id} to={`/courses/${course.id}/module/${moduleIndex}/lesson/${lessonIndex}`}>
                  {lesson.title}
                  <ArrowRight size={16} />
                </Link>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

