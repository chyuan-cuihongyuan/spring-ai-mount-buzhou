# 02 — memory · evictRatio 部分逐出保连续

**What to build:** 微压缩/摘要逐出候选消息时按可配比例（默认 0.7）部分逐出并保留连续尾窗，预算仍超时按 10% 步进梯子加压；最近 N Turn 原文与上一次增量摘要永不逐出。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] `evictRatio` 参数化（默认 0.7）+ 10% 步进升级梯子（预算仍超时逐级加压）
- [ ] 不变式：最近 N Turn 原文 + 上一次增量摘要永不逐出（与 summarized_message_ids 双水位兼容）
- [ ] P3 段先承受逐出压力（对齐 P0–P3 优先级）
- [ ] 单测断言：逐出后上下文连续（占位符与保留原文衔接、无断崖）
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-8；[T36](../tickets/T36-memory-evict-ratio.md)。源：letta 24,230★（SDK 默认 0.3 摘要/0.7 保留+步进）。
