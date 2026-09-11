# Wayfinder Map — Buzhou per-tool 超时预算覆盖（effort #529，E 会话第 30 轮）

> E 会话第 30 轮（31 per-tool glob 覆盖 × impl-28 单工具超时扩展轮）。
> 勘察：单工具超时是**全局值**（toolTimeout）——慢工具（爬虫/长查询）
> 与快工具（查表/计算）一刀切：全局值调大保护慢工具但拖死快工具故障
> 检测，调小反之。per-tool glob 覆盖空白。

## Destination

`core.exec.ToolTimeoutOverrides`（+嵌套 Holder——ToolResultLimiterHolder
同型）：glob 键 → 覆盖毫秒（负值 fail-fast；-1 = 用全局）；manager
effectiveToolTimeoutMillis 先查覆盖（命中替换全局）再与 Deadline 剩余
取 min（Deadline 恒天花板——308 语义不变）。默认空表零变化。

## Notes

- 号段：spec 529 / T811–812 / impl-432。

## Out of scope

- yml 装配面（Holder 原语先行——宿主/autoconfig 扩散）；动态热调。

## Tickets

- [x] [T811 覆盖原语与 Holder](../tickets/T811-tool-timeout-overrides.md)
- [x] [T812 manager 缝语义](../tickets/T812-tool-timeout-manager.md)
