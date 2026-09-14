# 1514 — SHA-256 裸异常迁移 CONFIG_INVALID + 审计死代码清扫

> 来源：M 会话第 16 轮 = effort #1514（impl 1117）。design-incompleteness 四-4 闭环 + 六-2 部分。

## 背景

- 四-4：`IllegalStateException("SHA-256 不可用")` 裸异常残留——全量重扫 11 处（清单记录 4 处后又新增 5 处同型），全部在 `MessageDigest.getInstance("SHA-256")` 的 JVM 缺陷不可达路径。
- 六-2：`AuditChain.verifySignature`（private）零调用方——逻辑已一字不差迁 `AuditChainVerifier.SignatureOps`。

## 目标

- 11 处统一迁移 `BuzhouException(ErrorCode.CONFIG_INVALID, "SHA-256 摘要不可用（JVM 环境缺陷）", e)`（结构化错误码 FATAL 可分类可观测——spec 50 §A 封口先例形态）；
- 删除死方法 verifySignature。

## 兼容性

异常类型变化仅在 JVM 缺陷不可达路径（无测试断言旧类型）；死代码删除零编译影响。
