---
Type: task
Status: open
---
## Question

guard/spill/skills/tools/store-jdbc/store-redis 泛化 throw（勘察计数 ~103 处）外部可见面渐进挂 BuzhouException+ErrorCode（新码按需增配 RetryCategory）；内部断言类 IllegalArgumentException 保留；破坏性变更入档 api-surface。验证：抽样模块单测仍绿 + 错误码覆盖统计。
