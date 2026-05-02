import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import CourseCard from '../components/CourseCard.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';
import PromptForm from '../components/PromptForm.jsx';

export default function HomePage() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api.listCourses()
      .then(setCourses)
      .catch((err) => setError(err.message))
      .finally(() => setInitialLoading(false));
  }, []);

  async function handleGenerate(payload) {
    setLoading(true);
    setError('');
    try {
      const course = await api.generateCourse(payload);
      navigate(`/course/${course.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page-grid">
      <section className="hero-panel">
        <p className="eyebrow">AI-powered curriculum builder</p>
        <h2>Turn any topic into a guided learning path.</h2>
        <p>
          Generate modules, lessons, objectives, quizzes, Hinglish explanations, videos, and downloadable lesson notes from one prompt.
        </p>
        <PromptForm onGenerate={handleGenerate} loading={loading} />
        <ErrorMessage message={error} />
      </section>

      <section id="recent" className="recent-panel">
        <div className="section-heading">
          <p className="eyebrow">Library</p>
          <h2>Recent courses</h2>
        </div>
        {initialLoading ? (
          <LoadingSpinner label="Fetching recent courses..." />
        ) : courses.length ? (
          <div className="course-list">
            {courses.map((course) => <CourseCard key={course.id} course={course} />)}
          </div>
        ) : (
          <p className="empty-state">No courses yet. Generate the first one and we’ll put it right here.</p>
        )}
      </section>
    </div>
  );
}

