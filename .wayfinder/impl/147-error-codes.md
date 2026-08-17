# 147 — 错误码统一收口（渐进首批）

**Parent:** spec 50 §A / [T178](../tickets/T178-error-codes.md)

**Status:** done

- [x] 新码：SPILL_IO_FAILED(RETRYABLE)/STORE_READ_FAILED(RETRYABLE)/SKILL_OPERATION_INVALID(NON_RETRYABLE)
- [x] 迁移：DiskSpillStore IO 面 9 处；SkillAdminApi 状态冲突 2 + 依赖未装配 2；TodoStore 序列化；ArgumentFingerprint/ReadIntegrity SHA 环境
- [x] 保留面钉住：编程式断言 IllegalArgumentException（JCS/SigningKeyRing/容量校验）；one-call-one-spill 契约 ISE
- [x] 测试：skills 断言升级（BuzhouException+码）；spill IO 失败码+cause 断言（新 SpillErrorCodeTest）；四模块全量绿
