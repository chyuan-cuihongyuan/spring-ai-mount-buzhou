# 727 — Redis 键布局健康面

> 来源：G 会话第 28 轮 = effort #727（705 审计的接线，548 同型）/ [T1054](../../.wayfinder/tickets/T1054-redis-key-health.md) / [T1055](../../.wayfinder/tickets/T1055-redis-key-health-verify.md) / impl 627。

## Problem

705 键布局审计是静态原语——actuator/312 告警引擎读不到；「当前布局有几处结构碰撞」要宿主手动跑代码。

## Solution

`RedisKeyLayoutHealth`（implements BuzhouHealth）：mechanism=redis-key-layout；恒 UP（findings 是数据需关注非进程故障——548 fsck 同口径）；details 聚合 findings 总数/shapeCollisions/colonSuffixTricks 分族计数+reservedSegments 数量。装配随 BuzhouRedisStoreAutoConfiguration（type=redis 条件继承，@ConditionalOnMissingBean 兼容）。

## Out of Scope

DOWN 语义（布局碰撞不阻断进程职能——数据需关注）；修复动作（键形状迁移归大版本）。
