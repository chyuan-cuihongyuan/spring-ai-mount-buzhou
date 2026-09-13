# 834 — 上下文截断统计

> 来源：H 会话第 35 轮 = effort #834 / [T1169](../../.wayfinder/tickets/T1169-context-truncation-stats.md) / [T1170](../../.wayfinder/tickets/T1170-context-truncation-stats-verify.md) / impl 587。
> 借鉴：HuggingFace tokenizer truncation_strategy（≈150K star）。

## Problem

多机制都会裁上下文（微压实/spill 外卸/硬裁剪）：各裁多少、谁裁得最多——「模型看不见的上下文」侵蚀量无跨机制聚合面。

## Solution

`ContextTruncationStats`（core.spi，纯记账）：

- **策略聚合**：record(strategy, charsDropped)——events/chars 双累计；键封顶 8，超限并入 `__overflow__` 桶（量净计不丢、名不可溯——如实取舍）。
- **报告**：chars 降序 + totalEvents/totalCharsDropped。

## 兼容性

纯新增（喂点=各截断机制装配侧）；零机制变更。

## 诚实边界

策略名语义归调用方；保留量不在账（侵蚀占比需配合窗口量自算）；溢出桶名不可溯。
