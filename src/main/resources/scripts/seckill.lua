-- 秒杀库存扣减 Lua 脚本（带用户维度防重）
-- KEYS[1]: 秒杀库存 Key，如 seckill:stock:movie:1
-- KEYS[2]: 用户集合 Key，如 seckill:users:movie:1
-- ARGV[1]: 用户 ID
-- 返回值: 1=扣减成功, 0=库存不足(售罄), 2=重复请求

-- 检查用户是否已抢过
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2  -- 重复请求
end

local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0  -- 库存不足
end

-- 扣库存 + 记录用户
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 1  -- 成功
