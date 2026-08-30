# Spec 90 — 语义漂移触发压缩（effort #51）

> wayfinder map：`.wayfinder51/MAP.md`（T343–T344）。#35 fog 项收口。
> 借鉴：Letta 语义触发压缩 / Phoenix 漂移检测。

## Problem Statement

边界机会压缩（spec 70）只有积压计数判据——「话题已经换了」这个更自然的压缩时机
缺席：长会话中话题切换后旧话题轮次大概率不再被引用，但仍占上下文直到积压达标。

## Solution

`SemanticDriftDetector`（memory/compact 函数接口）：`drifted(currentInput,
summaryText)`。默认 `LexicalDriftDetector`：字符 bigram Jaccard 相似度 < 0.15
判漂移（零依赖中英文通吃；0.0=永不判漂移/1.0=极端敏感；空基准不判——缺证据不
动作）。InjectionViewProcessor 在边界压缩判据处并联 driftTrigger（需摘要基准
previous 非空；与积压触发同路径——增量摘要/事实对账/检查点全管线复用）。
MemoryModule 键 `buzhou.memory.semantic-drift`（默认关）+
`semantic-drift-threshold`。

## User Stories

1. 作为长会话用户，我要话题切换后旧轮次及时折叠，所以上下文留给新话题。
2. 作为宿主，我要默认零变化，所以升级无感、按需 opt-in。
3. 作为进阶用户，我要注入 embedding 检测器，所以词面误判可升级为真语义检测。

## Implementation Decisions

- 函数接口（实现自由：词面/嵌入/模型——SPI 不锁死）。
- 词面版阈值保守（0.15——假阴性优于假阳性：误触发压缩代价高于漏触发）。
- 判据并联：积压 OR 漂移（两个信号两个场景）。

## Testing Decisions

- 漂移触发：coversUpTo 水位推进为真信号（backlog=0 排除积压判据干扰）；
  同话题不触发（水位不动）；无检测器零变化；词面判定面（同话题/全异/阈值方向/
  空基准）。

## Out of Scope

- embedding 检测器默认装配；漂移事件 payload 字段；压缩策略变更。

## Further Notes

- 与 spec 70 组成双信号边界压缩：计数（量）+ 漂移（质）。
