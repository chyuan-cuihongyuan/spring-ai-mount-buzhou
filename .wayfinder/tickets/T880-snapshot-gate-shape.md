---
id: T880
title: API 快照门自愈缺陷的修复形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

勘察发现 ApiSurfaceSnapshotTest 的 regenerateSnapshot() 是普通 @Test 随常规套件执行——先再生后比对（或反序），比对恒自愈，「新增公开类型未更新快照即失败」的门（T215/impl-179 声称的语义）形同虚设。怎么修？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 16 轮 = effort #600 / spec 615 / impl 468）：

1. regenerateSnapshot 门控为显式维护操作：`@EnabledIfSystemProperty(named="buzhou.api-snapshot.regenerate", matches="true")`——常规 verify 只比对（真门），再生成须 `-Dbuzhou.api-snapshot.regenerate=true` 显式触发。
2. 类 Javadoc 更新流程同步改写；顺带显式再生补录 F 会话 loop 5–15 的 7 个新公共型（DecayingFactStore/FactDecayPolicy/GcraRateLimitBackend/LexicalSkillRanker/HybridSkillRanker/CancelCause/CompactionShadowEvaluator——此前靠自愈遮蔽）。
3. 验证：门控后 starter 套件 11 绿 + regenerate 无属性时 skipped=1（不再自愈）。
