-- ============================================================
-- 케이스 1: 정류장 1개 (50명 전부 동일) → API 1번 호출
-- 예상 Before: ~1,343ms / After: ~1,343ms (차이 없음)
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
    'GGB211000001',
    '테스트정류장1',
    '21000',
    37.5665,
    126.9780,
    'GGB210000006',
    '테스트노선'
FROM favorite_folders ff
JOIN users u ON ff.user_id = u.id
WHERE u.email LIKE 'bench%';

SELECT COUNT(DISTINCT station_id) AS unique_stations FROM favorites; -- 기대값: 1
