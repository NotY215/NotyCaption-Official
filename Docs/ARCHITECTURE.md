# Architecture

NotyCaption Pro combines browser-side processing workflows with a small PHP game/account backend.

## Caption processing flow

Website -> Google Drive -> Google Colab -> Google Drive result -> Website

1. Audio is uploaded directly to Google Drive.
2. A processing notebook is generated.
3. The notebook is opened in Google Colab.
4. Colab processes the audio.
5. The result is uploaded to Google Drive.
6. The website polls Drive for the matching operation.
7. The result is displayed automatically.
8. The Colab tab is closed after completion.

## Game/account flow

Browser -> Google OAuth -> PHP API -> protected cache

- Google OAuth identifies the logged-in account.
- The PHP API verifies the access token with Google's userinfo endpoint.
- Email is the stable account key.
- Username is presentation data and can be changed without losing scores.
- Logged-in stage and score data are stored in the protected cache.
- Guest stage and score data remain in browser storage only.

## Game storage

- cache/scores.json: Easy, Normal and Hard leaderboard rows.
- cache/username.json: per-email username, stage and score data.
- cache/Id.json: email to username mapping.

Direct HTTP access to the cache directory is denied.

## Main components

- index.html: public entry page
- app.html: authenticated application interface
- game.html: Arrow Dash game shell
- game-infinite.js: infinite procedural stages, persistence and leaderboard UI
- profile.js: account username management
- drive.js: Google Drive operations
- ipynb.js: notebook generation
- colab.js: Colab lifecycle and result tracking
- audioManager.js: audio upload management
- api/*.php: authenticated game/account API
- api/game-api.ts: shared TypeScript API contracts
- caption.ipynb: Whisper workflow
- enhance.ipynb: vocal enhancement workflow

## Infinite stages

Every stage is generated from a deterministic seed derived from the difficulty and stage number. The result is reproducible for a given account and stage while still producing a new layout for every stage number. There is no final stage.

Each finish line contains a large finish door. Clearing it advances the infinite stage counter.

## Operation metadata

Caption results are identified using Google Drive appProperties:

- notycaption_operation_id
- notycaption_operation_type
- notycaption_status

The website does not rely on cross-origin sessionStorage communication with Colab.
