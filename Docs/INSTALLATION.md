# Installation

## Requirements

- Modern web browser
- Google account
- Google Drive access
- Google Colab access
- Configured Google OAuth application
- PHP hosting with writable application storage

## Web deployment

Deploy the repository while preserving its file structure.

Current configured website:

https://notycaptiongen.free.nf

The server must be able to execute the files under api/ and write the cache directory.

## OAuth

Keep the real Google OAuth client JSON as client.json on the server. Do not commit it to Git.

The browser does not fetch client.json directly. config.js requests /api/config.php, which exposes only the public OAuth configuration needed by the browser.

## Runtime cache

The server creates and updates:

- cache/scores.json
- cache/username.json
- cache/Id.json

Direct web access to cache is denied.

## Username system

The first authenticated request creates a unique username. Users can change it later from the account menu. Usernames are 3 to 20 characters and are case-insensitive for uniqueness.

## Game persistence

Logged-in users save their stage and best score per difficulty on the server. Guests save the same information only in browser storage.

Never commit OAuth secrets, private keys, refresh tokens, access tokens, or production cache files.
