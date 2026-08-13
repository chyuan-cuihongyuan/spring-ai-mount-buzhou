# 03 — README「生产」措辞降级，中英与 alpha 状态对齐

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T8](../tickets/T8-downgrade-production-wording.md)

**What to build:** 让 README 正文措辞与已诚实的「项目状态：早期开发 alpha」一致——把所有「生产就绪 / 跑在生产 / 生产所需的稳定性」类措辞统一降级为「**面向生产场景设计的实验性框架**」，中英两段同步。消除「正文宣称生产、状态段标 alpha」的自相矛盾。

**Blocked by:** 无 — 可立即开始。

**Status:** done (assignee: zcode)

- [x] 定位 README 中所有「生产」相关措辞（含中文 intro 与「为什么需要 Buzhou」段）—— 4 处：L3 英文 intro、L5 中文 intro、L17「为什么需要 Buzhou」段、L25 能力段
- [x] 统一降级为「面向生产场景设计的实验性框架」，中英同步
- [x] 全文不再出现「生产就绪 / 可直接上生产」类绝对宣称（grep 核验）
- [x] 与「项目状态：alpha」段措辞自洽

## Resolution

**改动（`README.md`，4 处，中英同步）**：

| 行 | 原措辞（绝对宣称） | 降级后 |
|---|---|---|
| L3 英文 intro | "makes a single agent stable...**in production**" | "is an **experimental framework designed for production scenarios**, aiming to keep a single agent stable..." |
| L5 中文 intro | "...让单个 Agent 稳定、可控、可解释地**跑在生产里**" | "...Buzhou 是一个**面向生产场景设计的实验性框架**，旨在让单个 Agent 稳定、可控、可解释地运行" |
| L17 为什么需要段 | "一个要在**生产里长期跑**的 Agent" | "一个要在**生产场景**中长期运行的 Agent" |
| L25 能力段 | "Buzhou 只在外围补齐**生产所需**的稳定性与可观测性" | "...补齐**面向生产场景所需**的稳定性与可观测性——目前为**实验性（alpha）**，详见[项目状态](#项目状态)" |

**核验**：`grep` 确认 `跑在生产里 / 在生产里长期跑 / 补齐生产所需 / in production through` 全部移除；无 `就绪 / ready / robust / battle / enterprise / hardened` 等其它绝对宣称；L25 现显式锚定「项目状态：alpha」段（L206），正文与状态段不再自相矛盾。

**范围决策（有意不动）**：`CONTEXT.md:8`（"Agent Runtime" 术语定义）与 `docs/spec/00-overview.md:7`（spec 总览）仍含「跑在生产里」——前者是术语**概念定义**、后者是**设计意图陈述**，二者均无「项目状态：alpha」段、不构成 README 式「正文宣称生产 vs 状态段标 alpha」的自相矛盾（SPEC item 4 所指），故保留为设计意图措辞、不在此票范围。
