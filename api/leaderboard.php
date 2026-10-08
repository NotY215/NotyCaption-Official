<?php
declare(strict_types=1);
require_once __DIR__ . '/bootstrap.php';
require_method('GET');

$difficulty = strtolower((string)($_GET['difficulty'] ?? 'easy'));
if (!in_array($difficulty, ['easy','normal','hard'], true)) $difficulty = 'easy';

$data = read_json('scores.json', ['easy'=>[],'normal'=>[],'hard'=>[]]);
$rows = is_array($data[$difficulty] ?? null) ? $data[$difficulty] : [];
$out = [];
$rank = 1;

foreach ($rows as $row) {
    if (count($out) >= 100) break;
    $out[] = [
        'rank' => $rank++,
        'username' => (string)($row['username'] ?? 'Player'),
        'score' => (int)($row['score'] ?? 0),
        'stage' => (int)($row['stage'] ?? 1)
    ];
}

json_response(['ok'=>true,'difficulty'=>$difficulty,'leaderboard'=>$out]);
?>