---
id: T2285
title: spill 默认值单一事实源（五-6 收口）与六-6/六-9 裁定的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 19 轮：五-6 剩余（spill 默认值散落）、六-6（load 死参数）、六-9（单线程调度器工厂）如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

① 五-6 spill 收口：DEFAULT_PREVIEW_CHARS(2048)/DEFAULT_LIST_PREVIEW_ITEMS(20)/DEFAULT_THRESHOLD_CHARS(32000→引用 SpillOffloadHook 既有常量) 落 SpillProperties 单一事实源——SpillModule.withDefaults 与 MediaIntake 便捷构造改引用，改默认值从散弹四处变单点；
② 六-6 裁定：SkillRegistry.load(appId, agentName, name) 前两参虽被实现忽略，但属 SPI 接口签名——0.x 阶段保留为按代理隔离技能的扩展位，Javadoc 注记诚实化（不删参数不破签名）；
③ 六-9 裁定：newSingleThreadScheduledExecutor 内部即 ScheduledThreadPoolExecutor(1)（delay queue 特性，无无界工作队列风险）——规约"禁无界队列工厂"不适用，代码不整改（与评审"建议"性质一致，非硬违规）。
