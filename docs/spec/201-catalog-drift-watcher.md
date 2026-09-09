# Spec 201 — 工具目录漂移看门狗（effort #219）

> wayfinder map：`.wayfinder/maps/effort-219.md`（T573–T574）。组合 spec 175（指纹）
> 成自动化闭环——变化即事件，不用人工拍快照。

## Problem Statement

目录指纹（175）给了对账原语，但没人盯着：MCP 热更新、装配变更、动态注册
发生后，运维要记得「拍快照比对」——靠人记得的防线等于没有。看门狗模式
（基线 + 重拍 + 变化即报）是监控系统的标准解。

## Solution

`CatalogDriftWatcher`（core/exec）：

- **基线**：首次 `check(List<ToolDefinition>)` 拍基线（不发事件——装配期
  多工具集变化是常态）。
- **重拍**：后续 check 重拍指纹 diff 基线——非空即：
  - 发 `tool.catalog.drifted` 事件（payload：added/removed/changed 三分类 +
    新旧 summaryHex）；
  - 基线推进到新版（再变再报，不重放旧闻）；
  - 计数 `buzhou.catalog.drifted`。
- 无变化静默零事件；check 由宿主定时/`tools/list_changed` 触发（本类无调度）。

## User Stories

1. 作为运维，工具面任何增删改都有事件——热更新被谁改了即时可见。
2. 作为策略，drifted 事件可接告警/审计——「意外工具混入」从对账报告升级为
  实时告警。
3. 作为宿主，装配期首拍不误报——只有「运行中的变化」才是事件。

## Implementation Decisions

- 内部复用 ToolCatalogFingerprint（175）零新哈希逻辑；事件经 Consumer 注入
  （与 SpawnGate emitter 同款）。

## Testing Decimals

- 首拍建基线零事件；增/删/改各发事件带三分类；无变静默；连续两变两事件
  （基线推进）；事件 payload 含新旧摘要。

## Out of Scope

- 基线持久化；自动回滚；调度器。

## Further Notes

- 漂移闭环：指纹原语（175）→ 看门狗（本轮）→ 告警接线（留档）。
