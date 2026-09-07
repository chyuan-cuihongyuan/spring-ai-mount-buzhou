---
Type: task
Status: closed
---
## Question

`EncryptingSummaryStore`：载体往返（结构键承载信封、版本以底层为准、
AAD 绑定路由字段）、旧明文透传、幂等包装安全。

## Resolution

done（2026-09-04）：impl-359；六用例（载体形态/全往返版本保真/history
多版/旧明文透传/幂等/AAD 换绑失败）绿。
