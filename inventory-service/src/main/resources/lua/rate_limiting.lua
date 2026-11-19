-- KEYS[1] = window ZSET   rl:{name:scope:id}:w
-- KEYS[2] = cooldown key  rl:{name:scope:id}:c
-- ARGV[1] = window ms | ARGV[2] = limit | ARGV[3] = cooldown ms | ARGV[4] = unique id
-- Returns {allowed (1/0), retryAfterMs, remaining}

local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local window   = tonumber(ARGV[1])
local limit    = tonumber(ARGV[2])
local cooldown = tonumber(ARGV[3])

-- 1. Active cooldown? (PTTL: -2 = key missing, -1 = no expiry)
local ttl = redis.call('PTTL', KEYS[2])
if ttl > 0 then
    return {0, ttl, 0}
end

-- 2. Remove requests older than the window
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - window)

-- 3. Over the limit?
local count = redis.call('ZCARD', KEYS[1])
if count >= limit then
    if cooldown > 0 then
        redis.call('SET', KEYS[2], '1', 'PX', cooldown)
        redis.call('DEL', KEYS[1])                 -- fresh start after cooldown
        return {0, cooldown, 0}
    end
    -- No cooldown: retry when the oldest entry leaves the window
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
    local retry = window
    if oldest[2] then retry = tonumber(oldest[2]) + window - now end
    if retry < 1 then retry = 1 end
    return {0, retry, 0}
end

-- 4. Accept this request
redis.call('ZADD', KEYS[1], now, ARGV[4])
redis.call('PEXPIRE', KEYS[1], window)             -- key self-cleans when idle

return {1, 0, limit - count - 1}