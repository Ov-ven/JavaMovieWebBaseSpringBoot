-- 秒杀库存扣减 Lua 脚本
-- KEYS[1]: 秒杀库存 Key，如 seckill:stock:movie:1
-- 返回值: 1=扣减成功, 0=库存不足(售罄)

local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil then
    return 0
end
if stock > 0 then
    redis.call('decr', KEYS[1])
    return 1
else
    return 0
end
