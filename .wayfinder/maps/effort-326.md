# Wayfinder Map — Buzhou 轮次重复检测（effort #326，C 会话第 27 轮）

> C 会话第 27 轮。runaway 族全是资源面（心跳/预算/计数）——LLM 最经典的
> 失控是<b>内容面</b>的：模型打转，连续产出近乎相同的输出（同句复读/同一
> 工具调用循环），token 烧满才被预算闸砍。缺"内容在打转"的早信号。
> LangChain/LlamaIndex context rot 讨论的库内硬化。

## Destination

`TurnRepetitionDetector`（core.runaway：连续输出 Jaccard 词元相似度 ≥ 阈值
计一段 run，run 达窗即fire 一次（闩住；出现不相似输出即解闩重计））+
`RepetitionDetectorHook`（order 60，afterModel 喂响应文本；fire 计数；
opt-in unstick=true 时 block 回填「[重复检测]」解困指令——替换复读输出）
+ yml `buzhou.runaway.repetition.{window, similarity-percent, unstick}`
（window 未配不装配）。

## Notes

- 号段：spec 326 / T643–T644 / impl-349。
- 借鉴：context rot / LLM 打转检测（LangChain 社区讨论）；相似度=词元
  Jaccard（朴素可解释，无模型依赖）。

## Decisions so far

- 相似度按<b>相邻对</b>判（非全对）——run 语义即"一直和上一条几乎一样"。
- fire 一次闩住：不刷屏；不相似输出解闩重计（再次打转会再 fire）。
- unstick 默认关（observe-only 计数+verdict；回填替换输出是行为变化，
  宿主显式开）。
- 检测器按 sessionId；会话数超 1024 整体重置（诚实降级入档）。

## Out of scope

- 语义级相似（embedding——无模型依赖原则）；工具调用序列打转（仅文本面
  ——工具序列另立项）；自动压缩上下文（摘要族已有）。

## Tickets

- [x] [T643 检测器 + hook + 装配](../tickets/T643-repetition.md)（impl-349）
- [x] [T644 回归与收口](../tickets/T644-repetition-close.md)（impl-349）
