# effort #727 — Redis 键审计健康面接线（705 扩散）

- 会话：G 会话 700 系第 28 轮 ｜ spec [727](../../../docs/spec/727-redis-key-layout-health.md) ｜ 票 [T1054](../tickets/T1054-redis-key-health.md)/[T1055](../tickets/T1055-redis-key-health-verify.md) ｜ impl627
- 借鉴：—（705 接线，548 同型）

## 勘察（排重）

- 705 audit 是静态原语无健康面——actuator/312 读不到；mechanism=redis-key-layout 零命中。

## 决定

`RedisKeyLayoutHealth`（store-redis，implements BuzhouHealth）：mechanism=redis-key-layout 恒 UP（findings 是数据——548 同口径）；details 聚合 findings/shapeCollisions/colonSuffixTricks/reservedSegments；装配随 BuzhouRedisStoreAutoConfiguration（type=redis 条件继承）。

## 测试

恒 UP+details 三族计数（6 保留段→9 findings：6+2+1）/定制前缀同成立。
