<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

require_method('GET');

$type = strtolower(trim((string)($_GET['type'] ?? '')));
$files = [
    'caption' => __DIR__ . '/../caption.ipynb',
    'enhance' => __DIR__ . '/../enhance.ipynb'
];

if (!isset($files[$type]) || !is_file($files[$type])) {
    json_response(['ok' => false, 'error' => 'Notebook template not found'], 404);
}

header('Content-Type: application/x-ipynb+json; charset=utf-8');
header('Content-Disposition: inline; filename="notycaption-template.ipynb"');
header('X-Content-Type-Options: nosniff');
header('Cache-Control: no-store');

readfile($files[$type]);
exit;
?>