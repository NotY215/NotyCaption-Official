# NotyCaption Game Backend

The production runtime uses PHP because the target shared hosting environment executes PHP directly. api/game-api.ts contains the shared TypeScript data contracts for the client/backend design.

## Storage

- cache/scores.json: server leaderboard data for Easy, Normal and Hard
- cache/username.json: per-account username, stage and score
- cache/Id.json: email to username mapping

These files are server-side storage and are denied by .htaccess.

## Authentication

Authenticated requests use the Google OAuth access token in the Authorization: Bearer header. The API validates the token with Google's userinfo endpoint and uses the verified email as the stable account key.

Usernames are presentation data only. Changing a username never changes the account key, so existing scores remain attached to the same email account.

## Guest mode

Guests keep their progress only in browser storage. Guest entries can be displayed locally in the leaderboard with a GUEST marker, but guest scores are never written to the server leaderboard.
