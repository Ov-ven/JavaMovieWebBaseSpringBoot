-- 批量插入 500 个测试用户（密码统一为 123456 的 MD5）
-- 执行：mysql -u root -p123456 movie_db < src/main/resources/test-users.sql

INSERT INTO `user` (username, password, email, balance, is_vip, status)
SELECT
    CONCAT('testuser_', seq),
    'e10adc3949ba59abbe56e057f20f883e',
    CONCAT('test_', seq, '@test.com'),
    1000.00,
    0,
    1
FROM (
    SELECT @rownum := @rownum + 1 AS seq
    FROM information_schema.columns a, information_schema.columns b, (SELECT @rownum := 0) r
    LIMIT 500
) nums;
