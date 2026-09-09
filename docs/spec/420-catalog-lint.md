# Spec 420 — 工具目录 lint（effort #420）

> wayfinder map：`.wayfinder/maps/effort-420.md`（T731–T732）。D 会话第 21 轮。

## Problem Statement

目录治理只有指纹漂移侦测（变更可见）；装配期质量体检缺失——坏名字
（大写/连字符模型难引）、过短描述（模型无信息可选）、过长描述（挤占
上下文）、跨源重名（歧义），静态质量问题让模型选错工具/解析失败且
无人把关。

## Solution

`core.exec.ToolCatalogLinter`（ESLint 构建期 lint 借鉴——只报不改）：

- **装配期体检**：SessionAssemblyCustomizer 经 wrapToolCallbacks 一遍扫
  全部工具定义，三条有界规则：
  - `NAME_CONVENTION`：name 不匹配 `^[a-z][a-z0-9_]{0,63}$`（Spring AI /
    MCP 工具名惯例）；
  - `DESCRIPTION_LENGTH`：description < 10 字（过短）或 > 4096 字
    （过长）；
  - `DUPLICATE_NAME`：同名不同实例跨源重名（歧义——后注册覆盖何者不
    明）。
- **发现出口**：WARN 日志（tool+rule+detail）+ 计数
  `buzhou.tools.catalog-lint.findings`（tag rule 3 值有界）+ 会话事件
  `tool.catalog.lint`（首装配一次——后续装配去重）。
- **不拦截不改写**（lint 非门禁——HITL/kill-switch 域）。
- yml `buzhou.tools.catalog-lint.enabled=false`（opt-in）。

## User Stories

1. 作为工具作者，我想装配期知道名字/描述质量问题，so 模型选工具的
   失败面在上线前可见。
2. 作为运维，我想发现按规则计数，so 目录质量趋势可追。

## Implementation Decisions

- 三规则有界（规则集膨胀另议）；DUPLICATE 判定基于实例身份非定义相等。

## Testing Decisions

- 三规则各自正/负样本；clean 目录零发现零事件；重复装配事件一次；
  yml 装配默认关。

## Out of Scope

- 阻断；schema lint；规则热插拔；面板。

## Further Notes

- 新公共类型 `ToolCatalogLinter`（嵌套 `Finding`）随轮 regenerate 快照。
