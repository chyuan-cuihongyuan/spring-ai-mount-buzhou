# effort #834 — 上下文截断统计

- 会话：H 会话 800 系第 35 轮 ｜ spec [834](../../../docs/spec/834-context-truncation-stats.md) ｜ 票 [T1169](../tickets/T1169-context-truncation-stats.md)/[T1170](../tickets/T1170-context-truncation-stats-verify.md) ｜ impl587
- 借鉴：HuggingFace tokenizer truncation_strategy（huggingface/transformers ≈150K star）——截掉什么/多少是可见性侵蚀量

## 勘察（排重）

- ContextWindowResolver：窗口大小解析——无截断量统计。
- MicroCompactionResult/CompactionRatioStats：压实域单机制——跨机制聚合缺位。
- grep -i `truncation.*stat|dropped.*chars`：无命中。

## 决定

`ContextTruncationStats`（core.spi，synchronized 记账）：record(strategy, charsDropped)——策略开集键封顶 8（超限并入 __overflow__ 桶，净计不丢量）；events/chars 双累计+快照 chars 降序；null/空白/负值忽略；空真。喂点=各截断机制装配侧。

## 测试

chars 降序+双累计精确（4300）/溢出桶净计（5000 不丢）+封顶 8+1/脏入参三形态+空真——3 例全绿。

## 诚实边界

策略名语义归调用方（本类不解释）；「保留量」不在账（只记裁掉——侵蚀占比需调用方自配窗口量）；溢出桶聚合按量保留（量不丢、名不可溯——如实取舍）。
