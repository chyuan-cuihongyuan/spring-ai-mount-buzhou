# Wayfinder Map — Buzhou 配置体检跨键规则（effort #77，50 轮自迭代第 42 轮）

> effort #77，延续 #76（T411–T412 / impl-261）。主线：**spec 91 fog 项「矛盾组合
> 检测（跨键语义规则表）」**——单键合法但组合矛盾/空转（开了开关没配依赖、配了
> 依赖没开开关）doctor 看不见。

## Destination

ConfigDoctor v2 跨键规则表（有界显式——4 条起步）：
1. bulkhead 开而未配 agents → WARN（NOOP 空转）；
2. acquire-timeout 配了而 bulkhead 未开 → WARN（静默空转）；
3. semantic-drift 开而 memory.enabled=false → WARN（挂不上）；
4. semantic-drift-threshold 配了而 drift 未开 → WARN（静默空转）。
计数经 crossKeyFindings 返回值（不重复扫 findings）；examine(Map) 与 Environment
入口同享。

## Notes

- 借鉴：IDE inspections 的跨符号检查（单符号合法 ≠ 组合正确）。

## Decisions so far

- 规则表有界显式（javadoc 列举——新规则须同步，防规则发散）。

## Not yet specified

- 更多规则（pii.types 与 guard.enabled 等跨模块组合——core 不依赖 guard 枚举，
  规则注册表 SPI 另议）。

## Out of scope

- 沿用 #7–#76。

## Tickets

- [x] [T415 crossKeyFindings 四规则 + 计数修正](../tickets/T415-doctor-rules.md)（impl-262）
- [x] [T416 红队（空转/孤儿依赖/矛盾/干净组合）+ 既有断言适配 + 收口](../tickets/T416-doctor-rules-close.md)
