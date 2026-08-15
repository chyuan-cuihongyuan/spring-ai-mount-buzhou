---
Type: task
Status: closed
---
## Question

guard/spill/skills/tools/store-jdbc/store-redis 泛化 throw（勘察计数 ~103 处）外部可见面渐进挂 BuzhouException+ErrorCode（新码按需增配 RetryCategory）；内部断言类 IllegalArgumentException 保留；破坏性变更入档 api-surface。验证：抽样模块单测仍绿 + 错误码覆盖统计。

## Resolution

spec 50 §A / impl-147 落地（渐进首批，诚实范围）：新码 3 个（SPILL_IO_FAILED/STORE_READ_FAILED/
SKILL_OPERATION_INVALID）；迁移外部可见失败面——DiskSpillStore IO 9 处、SkillAdminApi 4 处
（状态冲突→SKILL_OPERATION_INVALID、依赖未装配→CONFIG_INVALID）、TodoStore 序列化→DATA_CORRUPTION、
SHA-256 环境缺失 2 处→CONFIG_INVALID。**保留面钉住**：编程式断言 IllegalArgumentException
（keyVersion/JCS/容量校验——调用方 bug 信号非运行故障）与 one-call-one-spill 契约 ISE 不迁
（~70 处后续按模块渐进）。破坏性变更（异常类型）记入 T186 待办。四模块全绿
（spill 123/skills 67/tools 54/guard 96，含断言升级与新 SpillErrorCodeTest）。
