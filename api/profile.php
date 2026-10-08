<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';

$user = google_user();

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $profile = read_json('username.json', []);
    $record = $profile[$user['email']] ?? null;
    if (!$record) {
        $record = ensure_user($user['email'], $user['name']);
        $profile = read_json('username.json', []);
        $record = $profile[$user['email']] ?? $record;
    }
    json_response(['ok' => true, 'email' => $user['email'], 'username' => $record['username'], 'stage' => $record['stage'] ?? ['easy'=>1,'normal'=>1,'hard'=>1], 'score' => $record['score'] ?? ['easy'=>0,'normal'=>0,'hard'=>0]]);
}

require_method('POST');
$data = request_json();
$username = clean_username((string)($data['username'] ?? ''));

if (!valid_username($username)) {
    json_response(['ok' => false, 'error' => 'Username must be 3 to 20 characters and use only letters, numbers, or underscores.'], 422);
}

$ids = read_json('Id.json', []);
$key = strtolower($user['email']);
$requestedKey = username_key($username);

foreach ($ids as $email => $entry) {
    if (strtolower((string)$email) !== $key && isset($entry['username']) && username_key((string)$entry['username']) === $requestedKey) {
        json_response(['ok' => false, 'error' => 'That username is already taken.'], 409);
    }
}

$old = $ids[$key]['username'] ?? null;
update_json('Id.json', function(array $data) use ($key, $user, $username): array {
    $data[$key] = ['email' => $user['email'], 'username' => $username];
    return $data;
}, []);

update_json('username.json', function(array $data) use ($key, $user, $username, $old): array {
    $record = $data[$key] ?? [
        'email' => $user['email'],
        'username' => $old ?: $username,
        'stage' => ['easy'=>1,'normal'=>1,'hard'=>1],
        'score' => ['easy'=>0,'normal'=>0,'hard'=>0]
    ];
    $record['username'] = $username;
    $data[$key] = $record;
    return $data;
}, []);

update_json('scores.json', function(array $data) use ($key, $username): array {
    foreach (['easy','normal','hard'] as $difficulty) {
        $rows = is_array($data[$difficulty] ?? null) ? $data[$difficulty] : [];
        foreach ($rows as $i => $row) {
            if (strtolower((string)($row['email'] ?? '')) === $key) $data[$difficulty][$i]['username'] = $username;
        }
    }
    return $data;
}, ['easy'=>[],'normal'=>[],'hard'=>[]]);

release_cache_lock($cacheLock);\njson_response(['ok' => true, 'username' => $username]);
?>