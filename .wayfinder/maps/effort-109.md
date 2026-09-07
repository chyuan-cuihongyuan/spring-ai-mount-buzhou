# Wayfinder Map — Buzhou 会话特征抽取（effort #109，B 会话第 21 轮）

> B 会话第 21 轮。主题池「特征抽取」：会话行为特征（轮数/工具调用/错误率/
> 活跃度）散在各计数器里——下游（路由/风控/降级决策）要自己拼。借鉴 Feast
> feature store（特征一次定义多处消费）。

## Destination

SessionFeatureStore（core/session，LRU 1024 封顶）+ SessionFeaturesHook
（beforeTurn/afterTool/onModelError 四点自动累积：turns/toolCalls/toolErrors/
modelErrors/lastActiveAt）+ features(sessionId)/snapshot() 查询面。挂 hook
即累积，零侵入。

## Notes

- 号段：B=奇数 spec（本轮 161）。
- 特征是「会话行为侧写」——与成本台账（spec 65 计费面）正交。
- LRU 逐出最久未活跃（内存纪律——TurnHeartbeat 1024 先例）。

## Decisions so far

- 派生比率（错误率等）查询时算，不存储（存原始计数——比率零陈旧）。

## Not yet specified

- token 维度（usage 面接线）；外部特征仓导出。

## Out of scope

- 沿用 #7–#108；在线学习；跨实例特征聚合。

## Tickets

- [x] [T519 SessionFeatureStore + 采集 hook](../tickets/T519-features.md)（impl-293）
- [x] [T520 特征回归（四点累积/查询面/LRU 逐出/隔离）](../tickets/T520-features-tests.md)（impl-293）
