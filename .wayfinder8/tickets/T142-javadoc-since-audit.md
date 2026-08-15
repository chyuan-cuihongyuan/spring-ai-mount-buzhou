---
Type: task
Status: open
---
## Question

javadoc @since 审计：effort #6/#7 新公共类型标注 @since 1.0.0 是否齐全（api-surface 政策承诺）？

## Resolution

AFK 自决：脚本审计 + 补齐。grep 公共面新类型（api-surface effort#6/#7 两节清单）javadoc 含 `@since`；缺失即补 `@since 1.0.0`；CI 不加门（文档纪律，runbook/CONTRIBUTING 记载口径）。产 impl-115。
