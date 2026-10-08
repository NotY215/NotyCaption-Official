<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
require_method('POST');

$user = google_user();
$data = request_json();
$difficulty = strtolower((string)($data['difficulty'] ?? ''));
$score = max(0, min(2147483647, (int)($data['score'] ?? 0)));
$stage = max(1, min(2147483647, (int)($data['stage'] ?? 1)));

if (!in_array($difficulty, ['easy','normal','hard'], true)) {
    json_response(['ok' => false, 'error' => 'Invalid difficulty'], 422);
}

$profile = read_json('username.json', []);
$record = $profile[$user['email']] ?? ensure_user($user['email'], $user['name']);
$username = $record['username'] ?? ensure_user($user['email'], $user['name'])['username'];

update_json('username.json', function(array $data) use ($user, $username, $difficulty, $score, $stage): array {
    $key = $user['email'];
    $record = $data[$key] ?? ['email'=>$key,'username'=>$username,'stage'=>['easy'=>1,'normal'=>1,'hard'=>1],'score'=>['easy'=>0,'normal'=>0,'hard'=>0]];
    $record['username'] = $username;
    $record['stage'][$difficulty] = max((int)($record['stage'][$difficulty] ?? 1), $stage);
    $record['score'][$difficulty] = max((int)($record['score'][$difficulty] ?? 0), $score);
    $data[$key] = $record;
    return $data;
}, []);

update_json('scores.json', function(array $data) use ($user, $username, $difficulty, $score, $stage): array {
    $rows = is_array($data[$difficulty] ?? null) ? $data[$difficulty] : [];
    $found = false;
    foreach ($rows as $i => $row) {
        if (strtolower((string)($row['email'] ?? '')) === $user['email']) {
            $rows[$i]['username'] = $username;
            $rows[$i]['score'] = max((int)($row['score'] ?? 0), $score);
            $rows[$i]['stage'] = max((int)($row['stage'] ?? 1), $stage);
            $found = true;
            break;
        }
    }
    if (!$found) $rows[] = ['email'=>$user['email'],'username'=>$username,'score'=>$score,'stage'=>$stage];
    usort($rows, fn($a,$b) => (int)($b['score'] ?? 0) <=> (int)($a['score'] ?? 0));
    $data[$difficulty] = $rows;
    return $data;
}, ['easy'=>[],'normal'=>[],'hard'=>[]]);

json_response(['ok'=>true,'difficulty'=>$difficulty,'score'=>$score,'stage'=>$stage,'username'=>$username]);
?>