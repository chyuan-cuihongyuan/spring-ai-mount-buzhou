---
id: T2280
title: SHA-256 异常迁移与死代码清扫的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2279
created: 2026-09-15
---

## Question

M 会话第 16 轮：清扫如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：三模块编译绿 + 受影响测试类（ExportBundle/PostmortemBundle/SessionCanaryRegistry/AuditChainVerifier/EvalDatasetStore/EvalRunnerCancel/SessionExportConditional）9+ 用例零回归——异常类型变化（ISE→BuzhouException CONFIG_INVALID）在不可达路径（JVM 缺陷分支），无测试断言旧类型；死方法删除零编译影响（private 零调用）。
