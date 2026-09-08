# Spec 407 — 在线采样入评测集（effort #407）

> wayfinder map：`.wayfinder/maps/effort-407.md`（T705–T706）。D 会话第 8 轮。

## Problem Statement

eval 族回流全是拉式：宿主点名会话批量回流（TrajectoryImporter）/负反馈
回流（FeedbackImporter）。生产流量没有自动采样入集——评测集覆盖面受
宿主直觉限制，长尾真实问法进不了回归集。

## Solution

`core.eval.TurnSamplerHook`（Honeycomb head-based deterministic sampling
借鉴）：

- **afterTurn 观察位**（order 900 尾观察——不拦不改）：
  - **确定性采样**：`floorMod(hash(sessionId:turn), 100) < rate-percent`
    ——同轮同判可复现（重放不改变采样决定；率 0 = 关、100 = 全采）；
  - **过滤**：input/response 任一空白跳过；input 短于 min-input-chars
    （默认 0）跳过；
  - **入集**：`EvalDatasetStore.addItem(dataset, input, response,
    sessionId, turn)`——provenance 与拉式回流同溯源域，跨路去重天然
    成立；
  - **fail-soft**：数据集未建等异常捕获 + 计数
    `buzhou.eval.sampling-failed`——采样是旁路，绝不炸轮；成功采样计数
    `buzhou.eval.sampled-turns`。
- 装配：yml `buzhou.eval.sampling.{enabled=false, dataset, rate-percent,
  min-input-chars}` 声明即挂 hook + 暴露 `EvalDatasetStore` bean（宿主
  createDataset 建集用——集必须预建，采样不建集）。
- 采样语义 = 进候选池；golden 与否仍是人工判断（机制不预设）。

## User Stories

1. 作为评测作者，我想按率自动采生产轮次入集，所以 长尾真实问法进
   回归集不靠宿主直觉点名。
2. 作为宿主，我想采样决定确定性可复现，所以 重放/调试时采样行为一致。
3. 作为运维，我想采样故障 fail-soft，so 评测回流永不影响生产轮次。

## Implementation Decisions

- hash 采样（String.hashCode——稳定跨进程 JDK 语义）而非随机（可复现）。
- dataset 必须预建：采样期发现未建属宿主漏配，计数暴露不重建。

## Testing Decisions

- 率 100 全采 / 率 0 零采 / 中间率确定性（同轮同判）；
- 空白跳过 + min-input 过滤；
- 未建集 fail-soft（轮不炸 + 计数）+ 预建后成功入集含 provenance；
- yml 装配 + 默认关无 bean。

## Out of Scope

- 错误偏向尾采样；跨实例率共享；自动评分入集；容量上界治理。

## Further Notes

- 新公共类型 `TurnSamplerHook` / `BuzhouEvalSamplingProperties` 随轮
  regenerate 快照。
