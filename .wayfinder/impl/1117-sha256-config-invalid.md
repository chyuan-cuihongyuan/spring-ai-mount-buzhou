# 1117 — SHA-256 异常迁移 + 审计死代码（M 系 R16）

**What to build:** 11 处裸 IllegalStateException → BuzhouException(CONFIG_INVALID)；AuditChain.verifySignature 死方法删除。

**Blocked by:** T2279 / T2280（同轮 shape+verify）。

**Status:** done

- [x] 11 处迁移（core 6 / guard 4 / resilience 1，批量缩进保持）
- [x] verifySignature 死方法删除（零调用实证）
- [x] 三模块编译绿 + 受影响 9 用例零回归

## Done

验证：编译 + 定向测试绿。commit 见本轮 refactor 提交。
