<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: strict-origin-when-cross-origin');
header('Cache-Control: no-store');

const CACHE_DIR = __DIR__ . '/../cache';

function json_response(array $data, int $status = 200): never {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function require_method(string $method): void {
    if ($_SERVER['REQUEST_METHOD'] !== $method) {
        header('Allow: ' . $method);
        json_response(['ok' => false, 'error' => 'Method not allowed'], 405);
    }
}

function request_json(): array {
    $raw = file_get_contents('php://input') ?: '';
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

function bearer_token(): ?string {
    $headers = [];
    if (function_exists('getallheaders')) {
        $headers = getallheaders();
    }

    $candidates = [
        $_SERVER['HTTP_AUTHORIZATION'] ?? '',
        $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '',
        $headers['Authorization'] ?? '',
        $headers['authorization'] ?? '',
        $_COOKIE['notycaption_access_token'] ?? ''
    ];

    foreach ($candidates as $value) {
        $value = trim((string)$value);
        if ($value === '') continue;
        if (preg_match('/^Bearer\\s+(.+)$/i', $value, $m)) {
            return trim($m[1]);
        }
        if (!str_contains($value, ' ') && strlen($value) >= 20) {
            return $value;
        }
    }

    return null;
}

function google_user(): array {
    $token = bearer_token();
    if (!$token || strlen($token) < 20 || strlen($token) > 4096) {
        json_response(['ok' => false, 'error' => 'Authentication required'], 401);
    }

    $context = stream_context_create([
        'http' => [
            'method' => 'GET',
            'header' => "Authorization: Bearer {$token}\r\nAccept: application/json\r\n",
            'timeout' => 8,
            'ignore_errors' => true
        ]
    ]);

    $raw = @file_get_contents('https://www.googleapis.com/oauth2/v2/userinfo', false, $context);
    $user = json_decode($raw ?: '', true);

    if (!is_array($user) || empty($user['email']) || empty($user['id'])) {
        json_response(['ok' => false, 'error' => 'Invalid or expired Google session'], 401);
    }

    return [
        'id' => (string)$user['id'],
        'email' => strtolower(trim((string)$user['email'])),
        'name' => trim((string)($user['name'] ?? 'User'))
    ];
}

function ensure_cache(): void {
    if (!is_dir(CACHE_DIR)) {
        @mkdir(CACHE_DIR, 0755, true);
    }

    foreach (['scores.json', 'username.json', 'Id.json'] as $name) {
        $path = CACHE_DIR . '/' . $name;
        if (!file_exists($path)) {
            $initial = $name === 'scores.json'
                ? ['easy' => [], 'normal' => [], 'hard' => []]
                : [];
            @file_put_contents($path, json_encode($initial, JSON_PRETTY_PRINT));
        }
    }
}

function acquire_cache_lock() {
    ensure_cache();
    $fp = @fopen(CACHE_DIR . '/.lock', 'c');
    if (!$fp || !flock($fp, LOCK_EX)) {
        json_response(['ok' => false, 'error' => 'Server storage busy'], 503);
    }
    return $fp;
}

function release_cache_lock($fp): void {
    if ($fp) {
        flock($fp, LOCK_UN);
        fclose($fp);
    }
}

function read_json(string $name, array $fallback): array {
    ensure_cache();
    $path = CACHE_DIR . '/' . $name;
    $fp = @fopen($path, 'c+');
    if (!$fp) return $fallback;

    flock($fp, LOCK_SH);
    rewind($fp);
    $raw = stream_get_contents($fp);
    flock($fp, LOCK_UN);
    fclose($fp);

    $data = json_decode($raw ?: '', true);
    return is_array($data) ? $data : $fallback;
}

function update_json(string $name, callable $mutator, array $fallback): array {
    ensure_cache();
    $path = CACHE_DIR . '/' . $name;
    $fp = @fopen($path, 'c+');
    if (!$fp) {
        json_response(['ok' => false, 'error' => 'Server storage unavailable'], 503);
    }

    flock($fp, LOCK_EX);
    rewind($fp);
    $raw = stream_get_contents($fp);
    $data = json_decode($raw ?: '', true);
    if (!is_array($data)) $data = $fallback;

    $data = $mutator($data);
    $encoded = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

    ftruncate($fp, 0);
    rewind($fp);
    fwrite($fp, $encoded);
    fflush($fp);
    flock($fp, LOCK_UN);
    fclose($fp);

    return $data;
}

function clean_username(string $username): string {
    return trim($username);
}

function username_key(string $username): string {
    return strtolower($username);
}

function valid_username(string $username): bool {
    return (bool)preg_match('/^[A-Za-z0-9_]{3,20}$/', $username);
}

function find_username_for_email(string $email): ?string {
    $ids = read_json('Id.json', []);
    $key = strtolower($email);
    return isset($ids[$key]['username']) ? (string)$ids[$key]['username'] : null;
}

function ensure_user(string $email, string $displayName = 'User'): array {
    $cacheLock = acquire_cache_lock();

    $existing = find_username_for_email($email);
    if ($existing) {
        release_cache_lock($cacheLock);
        return ['username' => $existing];
    }

    $base = preg_replace('/[^A-Za-z0-9_]/', '', $displayName);
    $base = $base !== '' ? substr($base, 0, 16) : 'Player';
    if (strlen($base) < 3) $base = 'Player';

    $ids = read_json('Id.json', []);
    $used = [];
    foreach ($ids as $entry) {
        if (isset($entry['username'])) {
            $used[username_key((string)$entry['username'])] = true;
        }
    }

    $candidate = $base;
    $n = 2;
    while (isset($used[username_key($candidate)])) {
        $suffix = (string)$n++;
        $candidate = substr($base, 0, max(3, 20 - strlen($suffix))) . $suffix;
    }

    update_json('Id.json', function(array $data) use ($email, $candidate): array {
        $data[strtolower($email)] = [
            'email' => strtolower($email),
            'username' => $candidate
        ];
        return $data;
    }, []);

    update_json('username.json', function(array $data) use ($email, $candidate): array {
        $data[strtolower($email)] = [
            'email' => strtolower($email),
            'username' => $candidate,
            'stage' => ['easy' => 1, 'normal' => 1, 'hard' => 1],
            'score' => ['easy' => 0, 'normal' => 0, 'hard' => 0]
        ];
        return $data;
    }, []);

    release_cache_lock($cacheLock);
    return ['username' => $candidate];
}
?>