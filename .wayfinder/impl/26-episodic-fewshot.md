# 26 — memory · episodic memory few-shot

**What to build:** 成功任务经验复用为新任务 few-shot 示例：EpisodeLedger 采集成功轨迹、goal 向量召回 top-k、按预算注入 system prompt「过往成功示例」块。

**Blocked by:** 15（向量 recall——embedding provider 与检索基建）

**Status:** done（2026-08-14：EpisodeLedger——成功经验存 state（episode.N）、goal 向量召回 top-k（语义地板 0.10 防哈希碰撞噪声）、fewShotBlock 按预算渲染；默认关（无 provider 显式 no-op）；MemoryDeepeningFeaturesTest 召回/注入块/无关零命中断言）

- [ ] `EpisodeLedger{task_signature, goal, tool_trace_digest, outcome, embedding}` 存储与契约
- [ ] 采集 hook：任务成功判定后写入（sleep-time 整理器蒸馏可选路径）
- [ ] 检索注入：新任务以 goal 向量召回 top-k、按预算渲染进 system prompt「过往成功示例」块（复用预算渲染机制）
- [ ] 总开关默认关
- [ ] 端到端：同类第二任务召回并注入示例、无关任务不注入
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-14；[T42](../tickets/T42-memory-episodic-fewshot.md)。源：langgraph 39,627★ / langchain 144,172★（episodic=成功交互存为学习示例）。
