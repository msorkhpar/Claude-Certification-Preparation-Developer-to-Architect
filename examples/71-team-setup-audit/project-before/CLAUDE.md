# Orders app

## General
- TypeScript strict mode. No default exports.

## React components
- Functional components with hooks. Props are typed with an interface.

## API handlers
- Use async/await. Wrap failures in AppError with an HTTP status.

## Database models
- Follow the repository pattern. No SQL in handlers.

## Tests
- Test files sit next to the code they test, for example Button.test.tsx beside Button.tsx. Use describe and it, and no snapshots.
