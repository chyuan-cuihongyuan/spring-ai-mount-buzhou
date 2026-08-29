# Spec 115 — 配置体检跨键规则（effort #77）

> wayfinder map：`.wayfinder77/MAP.md`（T415–T416）。spec 91 fog 项收口。
> 借鉴：IDE inspections 的跨符号检查。

## Problem Statement

doctor v1 只查单键（拼写/值域）：单键全合法但组合矛盾/空转的配置看不见——
「开了 bulkhead 没配 agents（NOOP）」「配了 drift-threshold 没开 drift（静默空转）」
都是用户以为生效实际没生效的典型。

## Solution

ConfigDoctor v2 跨键规则表（有界显式——javadoc 列举，新规则须同步）：
1. bulkhead.enabled=true 且未配 agents → WARN「NOOP 空转」；
2. acquire-timeout 已配且 bulkhead.enabled 未开 → WARN「静默空转」；
3. semantic-drift=true 且 memory.enabled=false → WARN「挂不上」；
4. semantic-drift-threshold 已配且 drift 未开 → WARN「静默空转」。
计数经 crossKeyFindings 返回（不重复扫 findings）；examine(Map) 与 Environment
入口同享。

## User Stories

1. 作为宿主，我以为开了的能力真的开了——组合矛盾启动期点名。

## Testing Decisions

- 四规则各自命中 + 干净组合零跨键发现；既有值域/近邻断言适配（新 WARN 计入）。

## Out of Scope

- 跨模块规则注册表 SPI；规则自定义。

## Further Notes

- doctor 双层：v1 单键（拼写/值域）+ v2 跨键（组合语义）。
