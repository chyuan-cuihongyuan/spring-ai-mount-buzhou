---
id: T6162
title: S 会话 S31 Jump Hash 跳跃一致哈希的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6161]
created: 2026-09-24
---

## Question

S31 合同怎么逐一验绿？（spec 5030 / effort #5030 / S31）

## Resolution

**验证通过**：JumpHashTest 六测全绿——论文圣像值钉住；
确定性同键同桶；单调稳定（5→6 留桶 ≥800/1000）；均衡
（max/min<2）；指纹圣像 + 双入口一致；畸形 fail-fast。
