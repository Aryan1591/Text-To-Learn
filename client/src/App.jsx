import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import HomePage from './pages/HomePage.jsx';
import CoursePage from './pages/CoursePage.jsx';
import LessonPage from './pages/LessonPage.jsx';

export default function App({ authConfigured }) {
  return (
    <Routes>
      <Route element={<Layout authConfigured={authConfigured} />}>
        <Route index element={<HomePage authConfigured={authConfigured} />} />
        <Route path="/course/:courseId" element={<CoursePage />} />
        <Route path="/courses/:courseId/module/:moduleIndex/lesson/:lessonIndex" element={<LessonPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
