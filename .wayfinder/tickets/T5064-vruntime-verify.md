---
id: T5064
title: Q 会话 R32 vruntime 公平队列的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5063]
created: 2026-09-18
---

## Question

R32 合同怎么逐一验绿？（spec 3031 / effort #3031 / R32）

## Resolution

**验证通过**：VirtualRuntimeQueueTest 七测全绿——等权 9 择恰
"abcabcabc"、三倍权千择 690–810/190–310（期望 750/250）、空队列
null、work10/w2 账面 +5→+10、并列先入先选（work 0 双择）、随机
工作量等权 0.5±0.05、六路参数 fail-fast。
