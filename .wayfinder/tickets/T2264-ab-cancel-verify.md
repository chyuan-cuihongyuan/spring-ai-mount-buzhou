---
id: T2264
title: A/B 对比 run 宿主取消面的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2263
created: 2026-09-15
---

## Question

M 会话第 7 轮：A/B 取消面如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=PairwiseCancelTest` 绿 + 既有 pairwise/SPRT 测试零回归——
① 第 2 项 judge 触发 requestCancel()：前 2 项有裁决、剩余进 skipped、judge 恰调 2 次、summary.hostCancelled=true；
② 未取消 run hostCancelled=false（零行为变化）；
③ 取消后下一次 compare 完整执行（残留清零）。
