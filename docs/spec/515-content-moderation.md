# Spec 515 — 内容安全词表过滤（effort #515）

> wayfinder map：`.wayfinder/maps/effort-515.md`（T781–T782）。E 会话第 16 轮。

## Problem Statement

guard 缺声明式词表过滤：宿主自定义违禁词/竞品词/合规黑名单（contains
级）无处安放。OpenAI moderation 的本地规则面思想：命中即处理
（block/mask），无 ML 依赖。

## Solution

`guard.moderation.ContentModerationHook`（order 210）：

- 词表匹配：大小写不敏感 contains（CJK 无词界——plain contains 正确
  语义；无正则无 ReDoS）。
- 双缝：beforeTurn（用户输入）与 afterTool（工具结果）。
- 动作：BLOCK（输入缝 → HookResult.block 结构化告示；工具缝 →
  replaceResult 结构化告示——CanaryGuard 告示同族可信文本）| MASK
  （命中段替换 `[已屏蔽]`）。
- 计数：`buzhou.guard.moderation.hits`（tag seam=input|tool-output）。
- yml：`buzhou.guard.moderation.{terms: [...], action: block|mask}`——
  terms 空=不装配（默认关）；GuardModule.Builder contentModeration(...)。

## User Stories

1. 作为合规宿主，我想声明违禁词表并在输入/工具结果命中时阻断或打码，
   so 合规黑名单内容不进上下文不外发。
2. 作为运营，我想按缝统计命中， so 词表有效性可观测。

## Implementation Decisions

- contains 级召回（变体/形近字归 ML 面扩散——分层诚实）。
- 告示为可信框架文本（CanaryGuard INTERCEPT_NOTICE 同族——Spotlight
  不包裹）。
- MASK 用固定占位 `[已屏蔽]`（不回显命中词——回显即二次传播）。

## Testing Decisions

- 工具缝 BLOCK：命中 → 结果被结构化告示替换；MASK → 命中段替换。
- 输入缝 BLOCK → HookResult.block；大小写不敏感；无命中透传。
- yml terms 装配/空表不装配。

## Out of Scope

- 变体匹配；自动词表；回复流出（500 通道可组合）；上下文评分。

## Further Notes

- 新公共类型 `ContentModerationHook`（嵌套 `Action`）随轮 regenerate
  快照 + api-surface.md 加行。
