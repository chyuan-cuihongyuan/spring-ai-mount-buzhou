# 1039 — 提示词注册表解析分布读面

> 来源：J 会话第 39 轮 = effort #1039（[T1531](../../.wayfinder/tickets/T1531-prompt-resolution-stats-shape.md) / [T1532](../../.wayfinder/tickets/T1532-prompt-resolution-stats-verify.md) / impl 791）。与 R17 同谱系分轴：技能解析未命中（R17）→ 提示词解析未命中（本面）；OPA decision log 判定显形谱系。

## Problem Statement

InMemoryPromptRegistry（spec 401 提示词版本化注册表）resolve/resolveVersion 解析全程零计数：解析多少次、命中多少、**未命中多少**（提示词名拼错、标签缺失=配置错误信号）不可见——模板渲染走错版本或落空的排查无据。

## 目标

- `InMemoryPromptRegistry` 增量（core/prompt，实例级）：`resolutions` / `hits` / `misses` 三 AtomicLong——守恒不变量 **attempts == hits + misses**。
- 计数收敛到公共解析核心（resolve(name,label) 与 resolveVersion 共用 findVersion，不重复计）；resolve(name) 委托 resolve(name, LATEST) 同口径。
- 嵌套 record `PromptResolutionStats(long attempts, long hits, long misses)` + `resolutionStats()` 快照。

## 兼容性

纯增量读面：publish/label/resolve/resolveVersion 返回值与异常语义逐位不变；无新配置项。

## Out of Scope

- 按提示词名分桶（名数可控但先验总体分布已足）。
- 跨实例共享注册表（诚实边界维持 spec 401 原口径）。
