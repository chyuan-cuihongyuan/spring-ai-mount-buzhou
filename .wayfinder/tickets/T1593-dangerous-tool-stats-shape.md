---
id: T1593
title: 危险工具守卫判定读面（DangerousToolStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1589
created: 2026-09-15
---

## Question

J 会话第 69 轮：guard HITL 主 hook 的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：DangerousToolGuardHook（HITL 授权主 hook，194 行）六路径全部零计数——禁用/未匹配/已授权/豁免/升级确认全静默；「等待人工确认的调用量」量化缺失使 HITL 面积无对账。HITL 授权面对账思想。

形状裁决：`DangerousToolGuardHook` 内静态 `AtomicLong` 六计数——invocations（入口）/ disabledSkips / unmatchedSkips / authorizedSkips（授权命中含 once 消费与 session 复用）/ exemptedSkips（豁免放行）/ escalations（升级确认）；嵌套 `record DangerousToolStats` + `stats()` + `resetForTest()`。守恒 `invocations = 五结局桶之和`。静态面理由同族先例；beforeTool 返回与授权消费语义逐位不变。

Out of scope：按工具名分桶（既有配置面即清单）；fingerprint 分布（参数敏感面）。
