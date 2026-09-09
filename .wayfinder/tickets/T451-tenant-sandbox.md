---
Type: task
Status: closed
---
## Question

多租户文件面隔离：tenants/<tenant> 子根 + 跨租户遍历拒绝 + id 白名单。

## Resolution

done（2026-08-30）：impl-275；`FileSandbox.forTenant(root, tenant)` 静态工厂 +
红队 3 例（相对/绝对穿越拒绝、白名单 fail-fast、零追加白名单收窄）。
