# Wayfinder Map — Buzhou 内容安全词表过滤（effort #515，E 会话第 16 轮）

> E 会话第 16 轮。勘察：guard 有注入分类器（ONNX 概率面）/PII/秘密/
> taint——**声明式词表过滤**（宿主自定义违禁词/竞品词/合规黑名单，
> contains 级）空白；CanaryGuardHook 是密语泄漏检测非词表。OpenAI
> moderation 的本地规则面思想。

## Destination

`guard.moderation.ContentModerationHook implements BuzhouHook`（order
210——InputFlood 220 前）：yml 声明违禁词表（大小写不敏感 contains——
CJK 无词界，plain contains 是 CJK 正确语义，无 ReDoS）+ 动作二选一：
BLOCK（beforeTurn 命中 → HookResult.block 结构化告示；afterTool 命中 →
replaceResult 结构化告示——CanaryGuard 告示同族）| MASK（命中段替换
[已屏蔽]）；命中计数 `buzhou.guard.moderation.hits`（tag seam=input/
tool-output 有界）。McpModule 式 GuardModule.Builder contentModeration(...)
+ fromYml `moderation: {terms: [...], action: block|mask}`（默认关）。

## Notes

- 号段：spec 515 / T781–T782 / impl-418。
- 借鉴源：OpenAI moderation categories 的本地词表面（无 ML 依赖子集）。
- 诚实边界：contains 级召回（不处理变体/拼音/形近字——NER/ML 面归
  InjectionClassifier 扩散）；BLOCK 动作阻断的是本轮进入（输入缝）与
  单条工具结果（工具缝）。

## Out of scope

- 变体/形近字匹配；自动学习词表；输出缝（模型回复 500 已有 StreamTextFilter
  通道——宿主可组合）；上下文级评分。

## Tickets

- [x] [T781 双缝词表匹配与两动作](../tickets/T781-content-moderation-hook.md)
- [x] [T782 yml 装配与计数](../tickets/T782-content-moderation-assembly.md)
