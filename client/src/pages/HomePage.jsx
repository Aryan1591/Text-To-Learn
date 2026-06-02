import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth0 } from '@auth0/auth0-react';
import { api } from '../api/client.js';
import CourseCard from '../components/CourseCard.jsx';
import ErrorMessage from '../components/ErrorMessage.jsx';
import LoadingSpinner from '../components/LoadingSpinner.jsx';
import PromptForm from '../components/PromptForm.jsx';

function HomeContent({
  isAuthenticated,
  hideLibraryWhenLoggedOut,
  getAccessTokenSilently,
  heading,
  loadingLabel,
  emptyLabel,
}) {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    async function loadCourses() {
      if (hideLibraryWhenLoggedOut && !isAuthenticated) {
        if (active) {
          setCourses([]);
          setInitialLoading(false);
          setError('');
        }
        return;
      }

      setInitialLoading(true);
      setError('');
      try {
        const result = isAuthenticated
          ? await api.listMyCourses(getAccessTokenSilently)
          : await api.listCourses();
        if (active) {
          setCourses(result);
        }
      } catch (err) {
        if (active) {
          setError(err.message);
        }
      } finally {
        if (active) {
          setInitialLoading(false);
        }
      }
    }

    loadCourses();
    return () => {
      active = false;
    };
  }, [isAuthenticated, getAccessTokenSilently]);

  async function handleGenerate(payload) {
    setLoading(true);
    setError('');
    try {
      const getToken = isAuthenticated ? getAccessTokenSilently : undefined;
      const course = await api.generateCourse(payload, getToken);
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
          <h2>{heading}</h2>
        </div>
        {hideLibraryWhenLoggedOut && !isAuthenticated ? (
          <p className="empty-state">Login to view your saved courses.</p>
        ) : initialLoading ? (
          <LoadingSpinner label={loadingLabel} />
        ) : courses.length ? (
          <div className="course-list">
            {courses.map((course) => <CourseCard key={course.id} course={course} />)}
          </div>
        ) : (
          <p className="empty-state">{emptyLabel}</p>
        )}
      </section>
    </div>
  );
}

function AuthenticatedHomePage() {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();

  return (
    <HomeContent
      isAuthenticated={isAuthenticated}
      hideLibraryWhenLoggedOut
      getAccessTokenSilently={getAccessTokenSilently}
      heading={isAuthenticated ? 'Your courses' : 'Your courses'}
      loadingLabel="Fetching your courses..."
      emptyLabel="No courses in your account yet. Generate your first one."
    />
  );
}

function GuestHomePage() {
  return (
    <HomeContent
      isAuthenticated={false}
      hideLibraryWhenLoggedOut={false}
      getAccessTokenSilently={undefined}
      heading="Recent public courses"
      loadingLabel="Fetching recent courses..."
      emptyLabel="No courses yet. Generate the first one."
    />
  );
}

export default function HomePage({ authConfigured }) {
  return authConfigured ? <AuthenticatedHomePage /> : <GuestHomePage />;
}
