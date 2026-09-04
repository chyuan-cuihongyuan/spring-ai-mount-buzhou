# Spec 346 — 会话面板端点（effort #346）

> wayfinder map：`.wayfinder346/MAP.md`（T683–T684）。C 会话第 47 轮，
> 面板三部曲之三（343 配置 / 345 告警 / 346 会话）。

## Problem Statement

运维面板缺会话面：「现在多少活跃会话、准入地板被谁抬着（冻结/维护）、
cordon 是否生效」无端点可答——spawn 拒绝排障要跨日志与多个 API 拼图。

## Solution

`/actuator/buzhou-sessions`（BuzhouSessionsEndpoint，只读）：

- **activeSessions**：SessionIndexStore status=ACTIVE 分页计数（页 500、
  至多 100 页 = 50k 封顶，超限 `truncated: true` 诚实降级）；无索引
  bean → 段缺席（`available: false`）诚实。
- **spawnFloor**：`effective`（当前地板）+ `sources`（342 多源视图——
  谁抬着：default=冻结 / maintenance=cordon）。
- **maintenance**：MaintenanceCordon `view()`（cordoned/reason/until/
  cordonedCount）。
- 挂 343/345 同 actuator 条件配置类。

## User Stories

1. 作为运维，我想一屏看到活跃会话数与地板状态，所以 spawn 被拒时
   当场定位是容量、冻结还是维护。
2. 作为 SRE，我想看到地板各源（谁抬着），所以 335/342 的正交状态
   可分别核对。
3. 作为使用者，无索引部署段缺席诚实，所以 不臆造。

## Implementation Decisions

- 计数只读扫（短页即止）；端点构造容忍缺席 bean（ObjectProvider）。

## Testing Decisions

- 端点：索引在场计数准+地板抬升可见+cordon 视图 / 无索引段缺席 /
  截断标记（小窗多页）。
- 装配：actuator 下有端点。

## Out of Scope

- 会话明细（隐私）；per-app 分组；写操作。

## Further Notes

- 新公共类型 `BuzhouSessionsEndpoint` 随轮 regenerate 快照。
