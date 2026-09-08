# Wayfinder Map — Buzhou 工具目录 lint（effort #420，D 会话第 21 轮）

> D 会话第 21 轮。勘察（2026-09-08）：目录治理有指纹漂移侦测
> （ToolCatalogFingerprint/CatalogDriftWatcher——变更可见）——但**装配期
> 质量体检缺失**：坏名字（大写/连字符）、过长/过短描述、跨源重名，这些
> 让模型选错工具/解析失败的静态质量问题无人把关。ESLint/ruff 的「lint
> 在构建期」思想无对应物。

## Destination

`core.exec.ToolCatalogLinter`（ESLint 构建期 lint 借鉴——不改行为只报
发现）：装配期经 wrapToolCallbacks 一遍扫全部工具定义——检查 ①name 约定
（^[a-z][a-z0-9_]{0,63}$——Spring AI/MCP 工具名惯例）②description 长度
（<10 过短——模型无信息可选；>4096 过长——挤占上下文）③跨源重名（同名
不同实现——歧义）。发现 = WARN 日志（tool+rule+detail）+ 计数
`buzhou.tools.catalog-lint.findings`（tag rule 有界 3 值）+ 会话事件
`tool.catalog.lint`（首装配一次）；**不拦截不改写**（lint 非门禁——
safe 纪律）。yml `buzhou.tools.catalog-lint.enabled=false` opt-in。

## Notes

- 号段：spec 420 / T731–T732 / impl-393。
- 借鉴源：ESLint（26k★）构建期静态检查；MCP 工具名约定。
- 纪律：只报不改（门禁是 HITL/kill-switch 域）；规则集有界 3 条
  （规则膨胀另议）；每会话装配各跑一遍但事件只首拍。

## Out of scope

- 阻断模式；参数 schema lint（ToolArgsValidator 已管运行时）；规则
  热插拔；面板端点。

## Tickets

- [x] [T731 ToolCatalogLinter](../tickets/T731-catalog-linter.md)
- [x] [T732 装配 + 事件](../tickets/T732-catalog-lint-assembly.md)
