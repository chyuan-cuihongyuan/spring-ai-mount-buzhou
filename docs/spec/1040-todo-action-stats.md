# 1040 — todo 动作分布读面

> 来源：J 会话第 40 轮 = effort #1040（[T1533](../../.wayfinder/tickets/T1533-todo-action-stats-shape.md) / [T1534](../../.wayfinder/tickets/T1534-todo-action-stats-verify.md) / impl 792）。与 R30/R26 同谱系：动作分发分布显形（操作分桶——清单/更新/删除/清空四动作 + other）。

## Problem Statement

TodoTool（spec 06 推演 #5，四动作 todo 工具）call 按 action 分发，但零计数：四动作各自使用分布不可见——「clear 被反复调用」=模型反复清单重建（任务管理异常信号）；「upsert 远多于 list」=清单驱动工作流偏斜。spec 13 全局调用计数只有总量。

## 目标

- `TodoTool` 增量（buzhou-tools todo 包，实例级）：白名单五桶计数 `list` / `upsert` / `remove` / `clear` / `other`（ConcurrentHashMap<String,AtomicLong>，固定五键——基数安全有界）。
- 嵌套 record `TodoActionStats(Map<String, Long> byAction)` + `total()` 派生 + `actionStats()` 只读快照（Map.copyOf 不可变）。
- 计数点：call 解析出 action 后、分发前（白名单内计入对应桶，未知动作归 other）。

## 兼容性

纯增量读面：分发/返回值语义逐位不变；无新配置项。

## Out of Scope

- 按 sessionId 分桶（基数纪律；会话数无界）。
- TodoStalenessAudit 陈旧审计（已有独立面）。
