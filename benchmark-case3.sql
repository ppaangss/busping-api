-- ============================================================
-- 케이스 3: 정류장 50개 (1명당 1개) → API 50번 호출
-- 예상 Before: ~67,150ms / After: ~1,343ms
-- ============================================================

DELETE FROM favorites;
DELETE FROM favorite_folders;
DELETE FROM users WHERE email LIKE 'bench%';

INSERT INTO users (id, email, password, latitude, longitude, last_location_updated_at)
SELECT
    90000 + seq,
    CONCAT('bench', seq, '@test.com'),
    '$2a$10$dummyhashvalue000000000000000000000000000000000000000',
    37.5665,
    126.9780,
    DATE_ADD(NOW(), INTERVAL 9 HOUR)
FROM (
    SELECT a.N + b.N * 10 + 1 AS seq
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
         UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
         UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
) nums WHERE seq <= 50;

INSERT INTO favorite_folders (user_id, name)
SELECT id, '기본폴더' FROM users WHERE email LIKE 'bench%';

INSERT INTO favorites (folder_id, station_id, station_name, city_code, latitude, longitude, route_id, route_name)
SELECT
    ff.id,
    CONCAT('GGB21100', LPAD(ROW_NUMBER() OVER (ORDER BY u.id), 4, '0')),
    CONCAT('테스트정류장', ROW_NUMBER() OVER (ORDER BY u.id)),
    '21000',
    37.5665,
    126.9780,
    'GGB210000006',
    '테스트노선'
FROM favorite_folders ff
JOIN users u ON ff.user_id = u.id
WHERE u.email LIKE 'bench%';

SELECT COUNT(DISTINCT station_id) AS unique_stations FROM favorites; -- 기대값: 50
