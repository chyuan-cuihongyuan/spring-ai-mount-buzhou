---
Type: task
Status: closed
---
## Question

虚拟 key 配额：per-key token 硬顶 + 原子扣减 + 结构化拒绝 + 窗口清零。

## Resolution

done（2026-08-30）：impl-274；`budget/VirtualKeys`（register/trySpend/
spendOrThrow/usage/topUsage/reset）+ 红队 6 例。
