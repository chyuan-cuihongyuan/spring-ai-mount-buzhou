# Spec 326 — 轮次重复检测（effort #326）

> wayfinder map：`.wayfinder/maps/effort-326.md`（T643–T644）。借鉴：LangChain/
> LlamaIndex 社区 context rot / LLM 打转讨论——库内硬化。

## Problem Statement

模型打转（连续复读近乎相同的输出）只能等 token 预算闸砍或心跳超时——
两者都是"事后资源兜底"：烧掉的 token 与时间不可逆，且无"正在打转"的
信号面供宿主观测或干预。

## Solution

- `TurnRepetitionDetector`（core.runaway）：`record(content)` 逐条喂数——
  相邻输出词元集 Jaccard ≥ 相似阈值（默认 80%）→ run +1；不相似 → run
  归 1 并解闩。run 达窗（默认 3）fire 一次 `Verdict(runLength,
  similarity)`（闩住防刷屏；解闩后再达窗可再 fire）。
- `RepetitionDetectorHook`（order 60，心跳后）：afterModel 喂响应文本；
  fire 计数（`buzhou.runaway.repetition.fired`）；opt-in
  `unstick=true` 时 fire 的当次调用 block 回填「[重复检测]」+ 解困指令
  （替换复读输出——行为变化故默认关 observe-only）。
- yml `buzhou.runaway.repetition.{window, similarity-percent, unstick}`：
  window 未配不装配（零变化）。

## User Stories

1. 作为运维，我想看到"会话 X 已连续 3 次近乎相同输出"的计数与 verdict，
   所以打转在烧穿预算前就被看见。
2. 作为宿主，我想开 unstick 让复读输出被替换为解困指令（换策略/换工具/
   收束结论），所以打转会话有机会自愈。
3. 作为运维，偶发的相似输出（模板化正常回复）不该误报，所以相似阈值
   与窗口都可配。
4. 作为 SRE，检测本身不许引入模型依赖——朴素词元 Jaccard 可解释可测。

## Implementation Decisions

- 相邻对相似（非全对）——run = "一直和上一条几乎一样"。
- 空白/空输出视为不相似（run 归 1）——空转不是复读。
- 检测器 per-session；超 1024 会话整体重置（诚实降级）。
- 文本抽取走 chatResponse().getResult().getOutput().getText() 主链路
  （eval 族同款），null 防御到层。

## Testing Decisions

- `TurnRepetitionDetectorTest`：达窗 fire/相似阈下不 fire/不相似解闩重计/
  闩住不刷屏/空输出归 1/构造校验。
- `RepetitionDetectorHookTest`：afterModel 喂真 ChatClientResponse——
  observe-only 不 block/unstick block 回填指令/会话隔离。
- 装配：window 配置即 bean；未配不装；window=1 红。

## Out of Scope

- embedding 语义相似；工具调用序列打转；自动上下文压缩。

## Further Notes

- runaway 族补内容面：心跳（卡死）/ 预算（烧钱）/ **重复检测（打转）**
  ——三种失控各有专人。
