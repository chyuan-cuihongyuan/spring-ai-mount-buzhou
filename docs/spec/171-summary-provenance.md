# Spec 171 — 摘要溯源台账（effort #204）

> wayfinder map：`.wayfinder204/MAP.md`（T537–T538）。借鉴：W&B artifact
> lineage——产物（摘要）到源（原始消息）的证据链可回查。

## Problem Statement

压缩折入后原始消息被替换为摘要占位，「这段结论出自哪些消息」不可回查：
审计要证据链、争议裁决要原文出处、摘要质量分析要对照集——现在都无从下手。
管线已有折叠事实（MicroCompactionResult.compactedMessageIds / 折入回调），
只差记下来。

## Solution

`SummaryProvenance`（buzhou-memory）+ `SummaryProvenanceListener`：

- **累积**：`onCompacted` 把本轮微压缩折叠的 messageIds 累积到会话待落账池。
- **落账**：`onSummaryFolded`（摘要保存成功回调）把待落账池固化一条 lineage：
  `Entry(generation, trigger, sourceMessageIds, foldedCount, at)`。
- **回查**：`lineage(sessionId)` → List\<Entry\>（代际序）；`sourcesOf(sessionId,
  generation)` 直取某代源集。
- **有界**：per-session LRU 64 会话 × 每会话保留最近 32 条 lineage（审计窗
  有界——内存纪律诚实边界：更早代际不驻留，持久化留档）。

## User Stories

1. 作为审计员，摘要说「用户已同意退款」——我回查该代 lineage 的源消息 id，
   拿到证据链裁决争议。
2. 作为质量分析，对照某代摘要与其源集——摘要保真度有量化入口。
3. 作为运维，foldedCount 曲线 = 压缩强度侧写（配合 trigger 分布看折入动因）。

## Implementation Decisions

- 实现 CompactionListener（spec 95 挂点——零管线侵入；异常吞语义沿 listener
  契约「观测双写不影响主链」）。
- listener 累积与台账合一（单一对象即可装配）。

## Testing Decisions

- onCompacted 累积 → onSummaryFolded 落账（源集/count/trigger/generation 正确）；
  多轮微压缩合并到一代；两代 lineage 序；sourcesOf 精确取；LRU 有界。

## Out of Scope

- 持久化 lineage；JSONL 导出；摘要内容 diff。

## Further Notes

- 证据链拼图：spill 证据存活（26）→ 摘要出处（本轮）→ 事件日志回查（12）。
