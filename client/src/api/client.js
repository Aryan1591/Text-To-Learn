const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

async function request(path, options = {}, getToken) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (getToken) {
    try {
      const token = await getToken();
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Guest generation still works if auth is not configured.
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new Error(payload.error || `Request failed with ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export const api = {
  generateCourse: (body, getToken) => request('/api/courses/generate', {
    method: 'POST',
    body: JSON.stringify(body),
  }, getToken),
  listCourses: () => request('/api/courses'),
  listMyCourses: (getToken) => request('/api/courses/my', {}, getToken),
  getCourse: (id) => request(`/api/courses/${id}`),
  getVideo: (query) => request(`/api/youtube?query=${encodeURIComponent(query)}`),
};
