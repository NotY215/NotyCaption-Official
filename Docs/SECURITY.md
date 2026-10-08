# Security

## Reporting

Do not publish sensitive security issues containing credentials, access tokens, or private user data in public issues.

Security contact: NotY215@outlook.com

## Rules

- Never commit OAuth secrets.
- Never commit access tokens.
- Never expose private Drive credentials.
- Never put the Google OAuth client secret in browser JavaScript.
- Validate Google OAuth access tokens on the server before accepting account data.
- Use email as the stable account identifier, not the username.
- Treat usernames as case-insensitive and reject duplicates.
- Keep cache files outside the public API surface.
- Deny direct HTTP access to cache and server-only configuration files.
- Validate username, difficulty, stage and score values server-side.
- Escape leaderboard usernames before rendering them as HTML.
- Use file locking for concurrent cache updates.

## OAuth configuration

The browser loads only the public OAuth configuration from /api/config.php. The server reads client.json directly. client.json is ignored by Git and direct web access is denied by .htaccess.

A Google OAuth client ID is public browser configuration. A client secret must never be shipped to the browser. If a real secret was previously exposed, rotate it in Google Cloud.

## Guest mode

Guest progress is intentionally browser-only. Guest scores are not accepted by the server leaderboard.

> Placeholder: Add a dedicated private vulnerability reporting process if one is established.
