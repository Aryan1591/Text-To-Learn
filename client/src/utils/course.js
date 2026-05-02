export function getLesson(course, moduleIndex, lessonIndex) {
  const module = course?.modules?.[Number(moduleIndex)];
  const lesson = module?.lessons?.[Number(lessonIndex)];
  return { module, lesson };
}

export function firstLessonPath(course) {
  if (!course?.id) return '/';
  return `/courses/${course.id}/module/0/lesson/0`;
}

