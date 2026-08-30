---
Type: task
Status: closed
---
## Question

虚拟 key 健康面：top-8 用量行 + 耗尽标记 + 按需装配。

## Resolution

done（2026-08-30）：impl-289；`health/VirtualKeysHealth` + autoconfig
@ConditionalOnBean 装配 + 红队 3 例。
