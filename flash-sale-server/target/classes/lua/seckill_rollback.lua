-- KEYS[1] = stockKey
-- KEYS[2] = orderUserSetKey
-- ARGV[1] = userId

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    redis.call('SREM', KEYS[2], ARGV[1])
    redis.call('INCR', KEYS[1])
end

return 0
