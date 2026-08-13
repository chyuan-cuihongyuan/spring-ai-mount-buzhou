# 04 — 撰写「与 Spring AI 2.0 原生能力边界」文档

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T9](../tickets/T9-spring-ai-boundary-doc.md)（事实骨架来自已闭合的 [T2](../tickets/T2-spring-ai-native-vs-buzhou.md)）

**What to build:** 一份面向用户的「Buzhou 与 Spring AI 2.0 原生能力边界」文档，基于 T2 的逐机制 `NATIVE`/`ADDS`/`REPLACES` 表，明确三栏——

- **Spring AI 没有的（Buzhou REPLACES）**：Spill 溢出 / read_range、并行工具调用（含超时 / 取消）、Skill 能力 prompt。
- **增强的（Buzhou ADDS）**：渐进式记忆压缩（对照社区 `spring-ai-session`）、认知可观测（证据 / 推理 / span event，整合而非另起 OTel 管线）、Hook 护栏（HITL / 长产物 / 事实闭环，建在 Advisor 之上）、持久化 SPI（Summary / SessionState / SessionLease / Observability store）、原子工具集。
- **诚实的（Buzhou NATIVE = 仅 wrapper）**：**MCP 热插拔**——须明确标注 Spring AI 2.0 已原生支持动态工具增删，Buzhou 在此非差异化。

让评估者能判断 Buzhou 的真实差异化、而非被营销话术误导。

**Blocked by:** 无（T2 已闭合 → 事实骨架就绪）。

**Status:** ready-for-agent

- [ ] 文档落位（`docs/spec/` 新增一篇 或 README 单列一节）并链入 README / spec 索引
- [ ] 三栏（REPLACES / ADDS / NATIVE）覆盖 T2 表全部条目，标注置信度
- [ ] MCP = NATIVE 诚实标注（Buzhou 仅 wrapper、非差异化）
- [ ] 记忆对照 `spring-ai-session`、可观测对照原生 Micrometer / OTel
- [ ] 中英同步
