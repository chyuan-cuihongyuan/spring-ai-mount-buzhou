---
id: T2124
title: 取消延迟环与未决键语义的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2123
created: 2026-09-14
---

## Question

如何证明在途判定、未决键生命周期与分位读数正确？

## Resolution

**用户常设授权 AFK（可推翻）**

直调 observer 回调的确定性单测（等全仓 verify 完成后编码）：onCancel 无在途轮不入账；取消→错误终结入环（延迟=终结−请求）；正常终结清未决键不入环；P50/P95 recent-rank；reset 归零。E2E：脚本模型挂起流+StepVerifier 式取消（如 reactor-test 不可用则 blockInterruptibly 替身）断言 tracked=1 且延迟≥0。
