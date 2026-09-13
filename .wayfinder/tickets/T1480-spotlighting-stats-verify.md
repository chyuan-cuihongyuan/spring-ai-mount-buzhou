---
id: T1480
title: Spotlighting 应用与损坏计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1479
created: 2026-09-14
---

## Question

J 会话第 15 轮：应用与损坏计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpotlightingStatsTest，AssertJ 同仓风格）：wrap→unwrap 往返 wrapped=1/unwrapped=1/malformed=0；unwrap 明文（无头）三计数全零（非包裹不计）；畸形包裹（只有头无 END）→ wrapped=1/malformed=1/unwrapped=0；守恒 wrapped == unwrapped + malformed；resetForTest 归零；往返内容恒等回归（wrap→unwrap==原文）。BeforeEach/AfterEach 双 reset。定向 `mvn -pl buzhou-core test -Dtest='SpotlightingStatsTest,SpotlightingTest'` 绿。
