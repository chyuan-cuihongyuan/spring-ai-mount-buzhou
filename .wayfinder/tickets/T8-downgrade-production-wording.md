---
id: T8
title: README "生产就绪"措辞降级，正文与 alpha 状态对齐
type: task
status: closed
assignee: zcode
blocked-by: []
created: 2026-08-13
---

## Question

README 的「生产」措辞出现在：第 5 行（中文 intro「稳定、可控、可解释地跑在生产里」）与「为什么需要 Buzhou」段（17-25，「在生产里长期跑」「生产所需的稳定性与可观测性」）。降级为用户建议的「**面向生产场景设计的实验性框架**」，使正文与已诚实的「项目状态：早期开发 alpha」（206-208）一致。具体改哪几句？中英两段是否同步？

## Context

- 项目状态段已标 alpha，正文却仍宣称生产 → 自相矛盾。降级是「core 先做深」目的地的**必然要求**（深化期不宣称生产就绪）。
- 措辞基本已定，本 ticket 偏执行（task）。
- AFK 可做：定位所有「生产就绪/跑在生产」措辞 → 统一降级 → 中英同步 → 提交。

## Resolution

**改动（`README.md`，4 处，中英同步，正文与「项目状态：alpha」对齐）**：

- **L3 英文 intro**：`makes a single agent stable...in production` → `is an experimental framework designed for production scenarios, aiming to keep a single agent stable...`
- **L5 中文 intro**：`跑在生产里` → `面向生产场景设计的实验性框架，旨在...运行`
- **L17 为什么需要段**：`在生产里长期跑` → `在生产场景中长期运行`
- **L25 能力段**：`补齐生产所需的稳定性与可观测性` → `补齐面向生产场景所需的稳定性与可观测性——目前为实验性（alpha），详见项目状态`

**核验**：`grep` 确认旧措辞（`跑在生产里 / 在生产里长期跑 / 补齐生产所需 / in production through`）全除；无 `就绪/ready/robust/battle/enterprise/hardened` 等其它绝对宣称；L25 显式锚定「项目状态」段（L206）。

**范围**：`CONTEXT.md:8`（术语定义）与 `docs/spec/00-overview.md:7`（设计意图）仍含「跑在生产里」，二者无 alpha 状态段、不构成 README 式自相矛盾，有意不在本票范围。

**实现切片**：[impl/03](../impl/03-readme-wording-downgrade.md)。
