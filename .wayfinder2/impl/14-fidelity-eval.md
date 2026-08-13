# 14 — memory · 压缩保真度 eval

**What to build:** 九段式摘要的质量护栏：回放录制会话、注入仅在压缩前水位之下可答的 follow-up、用压缩后上下文跑 agent、LLM judge + evidence-id 精确断言答案保住；负例集防误触发。

**Blocked by:** 01（record/replay 基建）

**Status:** ready-for-agent

- [ ] `CompactionFidelityEval`：录制会话回放 → follow-up 注入 → 压缩后上下文跑 agent → 断言
- [ ] evidence-id 精确断言（答案须可溯源到压缩前证据）+ LLM judge（gated 可 stub 双轨）
- [ ] 负例集：不应压缩场景零误触发
- [ ] 指标：保真率 / 误触发率 / 压缩比
- [ ] 评测式断言沿用 `SummaryEvaluationTest` 方法论；可作 prompt 变更回归门（本地/gated）
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-11；[T39](../tickets/T39-memory-compaction-fidelity-eval.md)。源：langchain 144,172★（Deep Agents trace 注入式 eval；保真断言模式为研究延伸、自建）。
