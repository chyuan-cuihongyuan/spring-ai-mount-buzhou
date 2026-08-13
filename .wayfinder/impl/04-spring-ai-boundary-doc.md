# 04 — 撰写「与 Spring AI 2.0 原生能力边界」文档

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T9](../tickets/T9-spring-ai-boundary-doc.md)（事实骨架来自已闭合的 [T2](../tickets/T2-spring-ai-native-vs-buzhou.md)）

**What to build:** 一份面向用户的「Buzhou 与 Spring AI 2.0 原生能力边界」文档，基于 T2 的逐机制 `NATIVE`/`ADDS`/`REPLACES` 表，明确三栏——

- **Spring AI 没有的（Buzhou REPLACES）**：Spill 溢出 / read_range、并行工具调用（含超时 / 取消）、Skill 能力 prompt。
- **增强的（Buzhou ADDS）**：渐进式记忆压缩（对照社区 `spring-ai-session`）、认知可观测（证据 / 推理 / span event，整合而非另起 OTel 管线）、Hook 护栏（HITL / 长产物 / 事实闭环，建在 Advisor 之上）、持久化 SPI（Summary / SessionState / SessionLease / Observability store）、原子工具集。
- **诚实的（Buzhou NATIVE = 仅 wrapper）**：**MCP 热插拔**——须明确标注 Spring AI 2.0 已原生支持动态工具增删，Buzhou 在此非差异化。

让评估者能判断 Buzhou 的真实差异化、而非被营销话术误导。

**Blocked by:** 无（T2 已闭合 → 事实骨架就绪）。

**Status:** done (assignee: zcode)

- [x] 文档落位（`docs/spec/10-spring-ai-boundary.md`）并链入 README 文档索引
- [x] 三栏（REPLACES / ADDS / NATIVE）覆盖 T2 表全部九条，标注置信度（高 / 中高 / 中）
- [x] MCP = NATIVE 诚实标注（Buzhou 仅 wrapper、非差异化）
- [x] 记忆对照 `spring-ai-session`、可观测对照原生 Micrometer / OTel
- [x] 中英同步（English + 中文 两段并列）

## Resolution

**交付**：[`docs/spec/10-spring-ai-boundary.md`](../../docs/spec/10-spring-ai-boundary.md)（89 行，中英双语），落位 `docs/spec/` 第 10 篇（接 09-modules-engineering），README「文档」段新增一行链接。

**内容**：照 T2 表组织成面向评估者/集成者的三栏——
- **REPLACES**（Spring AI 没有）：Spill 溢出 / `read_range`（高）、并行工具调用含超时取消（高，标为「最强存活差异化、护城河在实现深度」）、Skill 能力 prompt（中）。
- **ADDS**（Spring AI 有、Buzhou 增强）：渐进式记忆压缩（中高，对照社区 `spring-ai-session`）、认知可观测（高，对照原生 Micrometer/OTel、强调「整合而非另起管线」）、Hook 护栏（高，建在 Advisor 之上、含 HITL→state→Attachment 闭环）、持久化 SPI（中高）、原子工具集（中高）。
- **NATIVE**（仅 wrapper）：**MCP 热插拔**（高）——单独诚实段标注 Spring AI 2.0 已原生、Buzhou 非差异化。

**核验**：T2 全部九条机制均已覆盖、置信度标注齐全；doc 内部相对链接（`.wayfinder`/`CONTEXT.md`/`README.md`/00-09 详设）均 resolve。
