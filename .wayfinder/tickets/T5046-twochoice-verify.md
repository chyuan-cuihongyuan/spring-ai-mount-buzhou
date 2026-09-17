---
id: T5046
title: Q 会话 R23 二择选择器的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5045]
created: 2026-09-18
---

## Question

R23 合同怎么逐一验绿？（spec 3022 / effort #3022 / R23）

## Resolution

**验证通过**：TwoChoiceSelectorTest 六测全绿——空桶选中率
0.47–0.53（理论恰 1/2：相异重抽两抽都错过 (3/4)(2/3)=1/2——
首版误按独立带放回口径算 15/16 被实测 0.4958 打脸，概率口径
带放回/相异重抽须分辨教训入档）、万球十桶二择最大负载 ≤ 单抽
同种子族且 ≤1020（Θ(ln ln n) 紧界证据）、等负载均匀 ±20%、
单桶恒 0、同种子回放、空数组/null/负负载 fail-fast。
