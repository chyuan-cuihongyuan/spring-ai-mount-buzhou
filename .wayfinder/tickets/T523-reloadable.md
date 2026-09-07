---
Type: task
Status: closed
---
## Question

热重载原语：原子换引用 + 版本 + 订阅 + 条件 CAS。

## Resolution

done（2026-08-30）：impl-294；ReloadableConfig（volatile 读零锁 / 锁外通知 /
同值替换也计版本——重放安全）。
