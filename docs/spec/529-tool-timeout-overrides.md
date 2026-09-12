# Spec 529 — per-tool 超时预算覆盖（effort #529）

> wayfinder map：`.wayfinder/maps/effort-529.md`（T811–T812）。E 会话第 30 轮。

## Problem Statement

单工具超时全局一刀切：调大保护慢工具但拖死快工具的故障检测，调小反之。
31 的 per-tool glob 覆盖模式在超时维度空白。

## Solution

`core.exec.ToolTimeoutOverrides`（+嵌套 Holder，ToolResultLimiterHolder
同型）：glob 键 → 覆盖毫秒（-1 = 用全局）；manager
effectiveToolTimeoutMillis 先查覆盖（命中替换全局）再与 Deadline 剩余
取 min（Deadline 恒天花板——308 传播语义不变）。默认空表零变化。

## User Stories

1. 作为宿主，我想给爬虫工具 5 分钟、给查表工具 5 秒， so 故障检测不
   被慢工具的宽预算拖累。

## Implementation Decisions

- 覆盖替换全局（非叠加）；Deadline 剩余恒天花板（308 传播语义不变）。
- Holder 模式（R7 输入限幅同型）；默认空表。

## Testing Decisions

- glob 命中替换/未命中全局/-1 语义/负值 fail-fast/Holder 往返与 null
  归 disabled。

## Out of Scope

- yml 装配面；动态热调。

## Further Notes

- 新公共类型 `ToolTimeoutOverrides`（嵌套 Holder）随轮 regenerate 快照
  + api-surface.md 加行。
