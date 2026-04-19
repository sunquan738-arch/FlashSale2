-- Password migration helper (incremental, non-destructive)
-- 1) Check legacy plain-text accounts
SELECT id, username, password
FROM `user`
WHERE password NOT LIKE 'pbkdf2_sha256$%';

-- 2) Example: migrate built-in demo accounts with known default password 123456
--    (only update rows still in plain-text)
UPDATE `user`
SET `password` = CASE username
    WHEN 'alice' THEN 'pbkdf2_sha256$120000$2QmMzyRI0jkOhwA2pv2LiQ==$pVjCPEe8wnhAutN+l7cbE4eAM/bezexiJJ7ayU2yobk='
    WHEN 'bob' THEN 'pbkdf2_sha256$120000$qdvFzfwp/qPDUo9AY+yptw==$BOkJElfl5uxwSx1REQHEGuhzQ3Zhe/1XToq6H8rjX2I='
    WHEN 'charlie' THEN 'pbkdf2_sha256$120000$1+eVDumseAiDBRU2aXfNcA==$W8nwzJVOAkSg7zKKCYuMOIlbee4WrgtUlACtUq8W/Jo='
    WHEN 'david' THEN 'pbkdf2_sha256$120000$0CUD+wz0U5HfZiY5WumN+g==$ndGdI981C7KwOIS6Tbfr8BB8r6hladXxliCcX9ZJvI4='
    WHEN 'eva' THEN 'pbkdf2_sha256$120000$fG1siFxzlyMtVvzFWWY0Ow==$FbOOBu8x84eMzl/n1oAVHKAvjGjXPokRJVk4yxWB/5E='
    ELSE `password`
END,
`update_time` = NOW(3)
WHERE username IN ('alice','bob','charlie','david','eva')
  AND password NOT LIKE 'pbkdf2_sha256$%';
