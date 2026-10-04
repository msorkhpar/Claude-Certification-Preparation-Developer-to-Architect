# Dispatch app

## General
- TypeScript strict mode. No default exports.
- Run `npm test` before finishing a task.
- My notes are in /home/dev/notes/dispatch.

## Components
- Write function components that use hooks. Type props with an interface.

## API handlers
- Use async/await. Wrap failures in `AppError` with an HTTP status.

## Database access
- Follow the repository pattern. No SQL outside a repository.

## Tests
- Use `describe` and `it`. Keep the test file next to the file it tests.
- Test files are named `<name>.spec.ts` or `<name>.spec.tsx` and sit beside the file they test.

## Reviews
- Before a pull request, run git diff and check types, error handling, tests and secrets.
