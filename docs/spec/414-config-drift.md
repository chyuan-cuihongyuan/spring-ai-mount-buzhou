# Spec 414 — 配置漂移审计（effort #414）

> wayfinder map：`.wayfinder/maps/effort-414.md`（T719–T720）。D 会话第 15 轮。

## Problem Statement

配置变更有热读无审计：320/325/340 各自热重读 yml 子集、343 只给当前快照
——「谁在何时把哪个旋钮从什么拧到什么」无事件无留痕；运行中漂移（配置
中心推送/环境变量变更）静默生效。

## Solution

`core.config.ConfigDriftAuditor`（ArgoCD drift detection 借鉴）：

- **周期快照 diff**：SmartLifecycle 单线程调度（interval 默认 30s）枚举
  `buzhou.*` 全属性（EnumerablePropertySource——343 同法；**末段掩码
  同一判定**——敏感值不进事件流）。
- **基线**：首拍建立（首拍零事件——基线不是漂移）。
- **漂移**：值变更 → 事件 `config.changed` {key, from, to}（掩码后）+
  计数 `buzhou.config.changed`；新增键 from="(unset)"、删除键
  to="(unset)"。
- yml：`buzhou.config-audit.{enabled=false, interval=30s}`；独立调度
  （405 同法——关审计不影响任何机制）。
- 事件经会话事件通道不可用（无会话上下文）——观察面走
  ObservabilityStore？诚实边界：本版事件**经 listener 回调**（宿主接
  webhook/日志）；内置事件流接线为扩散候选。

## User Stories

1. 作为运维，我想运行中配置变更有留痕（键/旧值/新值/时刻），so 「谁
   拧的旋钮」可追。
2. 作为安全负责人，我想敏感值掩码后才入事件，so 审计不泄密。
3. 作为宿主，我想未开启零开销，so 默认关。

## Implementation Decisions

- 轮询对账（两栖：plain boot 与 cloud 均工作）。
- listener 回调暴露变更（Consumer<List<Change>>）——宿主自接落盘/告警。

## Testing Decisions

- 首拍基线零事件；改动 MapPropertySource 值后下一拍发 from/to 正确；
  新增/删除键语义；掩码键（含 token/secret 末段）值不外泄；yml 装配
  默认关。

## Out of Scope

- cloud 事件加速；回滚；propertySource 定位；内置事件流/JSONL。

## Further Notes

- 新公共类型 `ConfigDriftAuditor`（嵌套 `Change`）/
  `BuzhouConfigAuditProperties` 随轮 regenerate 快照。
