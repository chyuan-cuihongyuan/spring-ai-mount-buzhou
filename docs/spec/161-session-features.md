# Spec 161 — 会话特征抽取（effort #109）

> wayfinder map：`.wayfinder109/MAP.md`（T519–T520）。借鉴：Feast feature
> store——特征一次定义多处消费（路由/风控/降级共享同一侧写）。

## Problem Statement

会话的行为侧写（轮数/工具调用/错误率/活跃度）散落在不同计数器与事件里：
路由想按「工具错误率」降级、风控想按「轮数×错误率」限流——每个消费者自己
从观测面拼一份，口径不一且重复实现。

## Solution

`SessionFeatureStore`（core/session）+ `SessionFeaturesHook`：

- **采集（hook 四点自动）**：beforeTurn → turns++ + lastActive；afterTool →
  toolCalls++（错误反馈文案计 toolErrors++——ToolFeedbackType 结构化标记）；
  onModelError → modelErrors++。
- **存储**：per-session 原始计数 + lastActiveAt；LRU 1024 封顶（逐出最久未
  活跃——内存纪律，TurnHeartbeat 先例）。
- **查询**：`features(sessionId)` → `Features(turns, toolCalls, toolErrors,
  modelErrors, lastActiveAt, toolErrorRate(), modelErrorRate())`（比率查询时
  算——零陈旧）；`snapshot()` 全量。
- 挂 hook 即累积（不挂零成本）；查询面只读。

## User Stories

1. 作为路由策略，我按 features.toolErrorRate() 对高危会话降级——一次定义
   多处消费。
2. 作为风控，我按 turns 与 modelErrors 侧写异常会话——不用自己拼计数器。
3. 作为运维，LRU 保证特征面内存有界（活跃会话才驻留）。

## Implementation Decisions

- 原始计数存储、比率派生（防陈旧口径）；错误识别复用结构化标记。
- per-session 单锁 + 全局 LRU（LinkedHashMap accessOrder 1024）。

## Testing Decisions

- 四点累积正确（turns/toolCalls/toolErrors/modelErrors/lastActive）；
  比率派生（零调用=0 不 NaN）；LRU 逐出最久未活跃；会话隔离；不挂 hook 零变化。

## Out of Scope

- token 维度；外部特征仓；在线学习；跨实例聚合。

## Further Notes

- 观测消费链：计数器（散）→ 特征面（本轮，一次定义）→ 策略消费。
