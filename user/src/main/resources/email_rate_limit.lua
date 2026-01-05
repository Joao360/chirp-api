local rateLimitKey = KEYS[1]
local attemptCountKey = KEYS[2]

if redis.call('EXISTS', rateLimitKey) == 1 then
    local ttl = redis.call('TTL', rateLimitKey)
    return { -1, ttl > 0 and ttl or 60 }  -- Rate limit active, return -1 and remaining TTL or default 60 seconds
end

local currentCount = redis.call('GET', attemptCountKey)
currentCount = currentCount and tonumber(currentCount) or 0

local newCount = redis.call('INCR', attemptCountKey)

local backoffSeconds = { 60, 300, 3600 } -- 1 minute, 5 minutes, 1 hour
local backoffIndex = max.min(currentCount, 2) + 1

redis.call('SETEX', rateLimitKey, backoffSeconds[backoffIndex], '1') -- Set rate limit key with backoff TTL
redis.call('EXPIRE', attemptCountKey, 86400) -- Set attempt count key to expire in 24 hours

return { newCount, 0 }  -- Return new attempt count and 0 indicating no active rate limit