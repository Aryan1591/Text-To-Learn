# Text-to-Learn: AI-Powered Course Generator

Text-to-Learn turns a free-form learning topic into a structured online course with modules, rich lessons, quizzes, video suggestions, saved courses, and lesson PDF export.

This version uses a Java-friendly backend:

- `server/`: Spring Boot 3, Spring Security, MongoDB, optional Auth0 JWT validation
- `client/`: React + Vite, React Router, Auth0 React SDK, html2canvas + jsPDF

## Features

- Generate a full course from a topic prompt
- 3-6 modules with 3-5 lessons each
- Rich lesson blocks: headings, paragraphs, objectives, code, video queries, MCQs
- Persist courses in MongoDB
- Auth0-ready protected user endpoints
- YouTube video lookup endpoint
- Lesson PDF export
- Responsive syllabus and lesson UI
- Template fallback generator when no AI API key is configured

## Project Structure

```text
text-to-learn-spring/
|-- server/
|   `-- src/main/java/com/texttolearn/api/
|       |-- config/
|       |-- controller/
|       |-- dto/
|       |-- exception/
|       |-- model/
|       |-- repository/
|       |-- security/
|       `-- service/
`-- client/
    `-- src/
        |-- api/
        |-- components/
        |-- context/
        |-- pages/
        `-- utils/
```

## What You Need To Do

1. Create a MongoDB Atlas database and copy the connection string.
2. Create an Auth0 SPA app and API if you want login-protected course ownership.
3. Optional: create YouTube Data API and Gemini/OpenAI keys.
4. Copy `server/.env.example` to `server/.env` and fill values.
5. Copy `client/.env.example` to `client/.env` and fill values.
6. Run backend and frontend locally.

## Run Locally

Backend:

```bash
cd server
mvn spring-boot:run
```

Frontend:

```bash
cd client
npm install
npm run dev
```

Open `http://localhost:5173`.

## Backend Environment

```env
PORT=5000
MONGO_URI=mongodb://localhost:27017/text_to_learn
MONGO_DATABASE=text_to_learn
CLIENT_ORIGIN=http://localhost:5173
AUTH0_ISSUER_URI=https://your-domain.auth0.com/
# Optional fallback if you already use AUTH0_ISSUER in older docs
# AUTH0_ISSUER=https://your-domain.auth0.com/
AUTH0_AUDIENCE=https://text-to-learn-api
GEMINI_API_KEY=
OPENAI_API_KEY=
YOUTUBE_API_KEY=
```

The app runs without AI keys by using the built-in template generator.

## Frontend Environment

```env
VITE_API_URL=http://localhost:5000
VITE_AUTH0_DOMAIN=your-domain.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
VITE_AUTH0_AUDIENCE=https://text-to-learn-api
```

If Auth0 values are placeholders, the UI still works in guest mode for generation.

## API Overview

- `POST /api/courses/generate`: generate and save a course
- `GET /api/courses`: list public/recent generated courses
- `GET /api/courses/{id}`: get a course by id
- `GET /api/courses/my`: list authenticated user's courses
- `DELETE /api/courses/{id}`: delete own course
- `GET /api/youtube?query=...`: resolve a video query

## Deployment

Backend on Render:

- Service type: `Docker`
- Root directory: `server`
- Environment variables:
  - `PORT=5000`
- `MONGO_URI=...`
- `MONGO_DATABASE=text_to_learn`
  - `CLIENT_ORIGIN=https://your-vercel-app.vercel.app`
  - `AUTH0_ISSUER_URI=https://your-domain.auth0.com/`
  - `AUTH0_AUDIENCE=https://text-to-learn-api`
  - `GEMINI_API_KEY=...`
  - `YOUTUBE_API_KEY=...`

Render will read the `server/Dockerfile` and build the Spring Boot app inside a container.

Frontend on Vercel:

- Root directory: `client`
- Build command: `npm run build`
- Output directory: `dist`
- Environment variables:
  - `VITE_API_URL=https://your-render-app.onrender.com`
  - `VITE_AUTH0_DOMAIN=your-domain.auth0.com`
  - `VITE_AUTH0_CLIENT_ID=your-client-id`
  - `VITE_AUTH0_AUDIENCE=https://text-to-learn-api`

Auth0 dashboard:

- Add the Vercel URL to Allowed Callback URLs, Allowed Logout URLs, and Allowed Web Origins.
- Add the frontend domain to any CORS or origin settings you have enabled.
- Keep the Auth0 audience identical in the frontend and backend.

## Hackathon Demo Script

1. Show login or guest landing page.
2. Enter a topic like `Intro to React Hooks for beginners`.
3. Generate a course and open the course overview.
4. Navigate module lessons.
5. Show MCQs, code blocks, video suggestions, and PDF download.
6. Briefly explain backend packages and frontend components.
