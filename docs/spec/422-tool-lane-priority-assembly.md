# Spec 422 — 工具泳道优先级装配（effort #422）

> wayfinder map：`.wayfinder/maps/effort-422.md`（T735–T736）。D 会话第 23 轮。

## Problem Statement

PriorityLane（spec 411）与 ToolLaneRegistry/LaneLimitingToolCallback
（spec 173）都是零接线库原语：泳道是平面公平 Semaphore——全部调用者
FIFO 等价排队；宿主要「交互工具优先于批处理工具拿许可」只能手写代码，
yml 无声明面。

## Solution

yml 声明即装配（Envoy priority levels 接线，406 装配方同法）：

- `buzhou.tool-lanes.lanes.<泳道名>.permits / acquire-timeout`（默认
  permits=1、timeout=30s——排队优先于拒绝）。
- `buzhou.tool-lanes.tools.<工具名>.lane / priority`（默认 priority=5
  中位；0-9 有界）。
- 装配：lanes 非空才出 RuntimeConfig（Binder 预绑 Condition）；customizer
  经 wrapToolCallbacks 名匹配 → `PriorityLaneToolCallback`（共享
  `ToolLaneRegistry.priorityLane` 命名单例）；未命中零包装。
- `PriorityLaneToolCallback`：acquire(priority, timeout) → finally release；
  超时/中断抛 IllegalStateException（与 LaneLimitingToolCallback 同词汇）；
  异常路径也归还许可。

## User Stories

1. 作为应用作者，我想 yml 声明泳道容量与 per-tool 优先级，so 交互工具
   拥挤时先于批处理工具拿许可，无需写代码。
2. 作为运维，我想 tools 引用拼错的泳道名在启动时红，so 配置错误不拖到
   首次调用才暴露。

## Implementation Decisions

- ToolLaneRegistry 增 `priorityLane(name, permits)`（与 `lane()` 分仓：
  类型不同不共享 namespace——混用同名不会 CCE）。
- fail-fast：bean 方法内校验 tools 的 lane 引用都在 lanes 声明内。
- 共享语义：customizer 每会话装配重复执行，registry 命名单例保证许可
  跨会话共享（permits 只在首建生效——同 `lane()` 语义）。

## Testing Decisions

- 插队：1 许可泳道被持 → 低(5)先排队、高(1)后排队 → release 高先完成
  （R12 教训：逐次 release+完成屏障）；超时结构化异常；异常路径许可归还。
- registry：priorityLane 同名同实例、异名异实例。
- yml 装配：声明 lanes 出 bean、未配置无 bean、未知泳道引用启动失败；
  E2E 起 runtime 包装不炸。

## Out of Scope

- 抢占持有者；动态调优先级；跨实例共享（BackendLanePermit 域）；等待
  观测面装配。

## Further Notes

- 新公共类型 `PriorityLaneToolCallback`、`BuzhouToolLaneProperties` 随轮
  regenerate 快照。
