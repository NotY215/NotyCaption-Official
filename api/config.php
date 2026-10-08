<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
require_method('GET');

$path = __DIR__ . '/../client.json';
if (!is_file($path)) json_response(['ok' => false, 'error' => 'OAuth configuration unavailable'], 503);

$data = json_decode((string)file_get_contents($path), true);
$web = is_array($data['web'] ?? null) ? $data['web'] : [];
if (empty($web['client_id'])) json_response(['ok' => false, 'error' => 'OAuth client ID missing'], 503);

json_response([
    'ok' => true,
    'client_id' => $web['client_id'],
    'project_id' => $web['project_id'] ?? null,
    'auth_uri' => $web['auth_uri'] ?? 'https://accounts.google.com/o/oauth2/auth',
    'token_uri' => $web['token_uri'] ?? 'https://oauth2.googleapis.com/token',
    'redirect_uris' => [$web['redirect_uris'][0] ?? null],\n    'redirect_uri' => $web['redirect_uris'][0] ?? null,
    'auth_provider_x509_cert_url' => $web['auth_provider_x509_cert_url'] ?? null,\n    'scopes' => $web['scopes'] ?? [
        'https://www.googleapis.com/auth/drive.file',
        'https://www.googleapis.com/auth/userinfo.profile',
        'https://www.googleapis.com/auth/userinfo.email'
    ]
]);
?>