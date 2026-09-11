---
id: T902
title: 事实衰减装配扩散的形态裁决（含移驻 core）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

DecayingFactStore（spec 604）只在 memory 模块、FactStore 生产构造点在 GuardModule（guard 模块）——模块互斥规则下 guard 不能依赖 memory，衰减无法装配。怎么接？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 27 轮 = effort #600 / spec 626 / impl 479）：

1. **移驻 core.internal.memory**（与 DefaultFactStore 同址——GuardModule 本就引用该 internal 包，先例既存）：DecayingFactStore + FactDecayPolicy + 测试整体迁移（memory 模块 facts 包撤销）。
2. GuardModule.Builder 增 `factDecay(FactDecayPolicy)`（null = 既有语义零变化）；构造点按声明包装。
3. yml：`buzhou.guard.fact-decay.half-life-turns` 声明即启用（floor 可选默认 0.25）。
4. 604 spec 的非目标「不接装配」由此轮解除（原语→装配的标准扩散）。
