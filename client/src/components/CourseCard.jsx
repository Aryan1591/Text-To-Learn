import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { firstLessonPath } from '../utils/course.js';

export default function CourseCard({ course }) {
  return (
    <article className="course-card">
      <div>
        <p className="tagline">{course.tags?.slice(0, 2).join(' / ')}</p>
        <h3>{course.title}</h3>
        <p>{course.description}</p>
      </div>
      <div className="course-card-actions">
        <span>{course.modules?.length || 0} modules</span>
        <Link to={`/course/${course.id}`}>Overview</Link>
        <Link className="round-link" to={firstLessonPath(course)}><ArrowRight size={18} /></Link>
      </div>
    </article>
  );
}

