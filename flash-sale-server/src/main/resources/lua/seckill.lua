-- KEYS[1] = stockKey      e.g. seckill:stock:{activityId}
-- KEYS[2] = orderUserSet  e.g. seckill:ordered:{activityId}
-- ARGV[1] = userId

local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return 2
end

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1
end

if stock <= 0 then
    return 2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 0
