---
id: T2283
title: spec 07 七切面回写 + Advisor/Hook 序位常量化的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 18 轮：design-incompleteness 四-5 与五-6（order 部分）如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

① 四-5：spec 07 三处「六切面」回写七切面（spec 15 落地 onModelError 后口径追认）；「编译 6 链缓存」一句核实为单链全遍历实现（HookChain 单链）——该句不改实现表述（评审所指为字面无对应物，保留原文语义注记由后续 07 档修订轮处理）。
② 五-6 order 部分：四处序位魔法值常量化（ResponseCacheAdvisor +450 / SemanticCacheAdvisor +460 / SpillOffloadHook 100 / OnloadHook 200 → ADVISOR_ORDER_OFFSET/HOOK_ORDER static final + 链位注释）——ResilienceAdvisor.CHAIN_ORDER_OFFSET=700 先例同款，行为零变化（同值）。
spill 默认值散落四处（2048/20/32000）留候选池（涉 SpillProperties 与三消费点统一，独立轮）。
