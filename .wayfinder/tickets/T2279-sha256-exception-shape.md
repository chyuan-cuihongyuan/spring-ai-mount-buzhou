---
id: T2279
title: SHA-256 裸异常迁移 CONFIG_INVALID + 审计死代码清扫的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 16 轮：design-incompleteness 四-4（裸 IllegalStateException 四处）与六-2（AuditChain.verifySignature 死代码）如何清扫？

## Resolution

**用户常设授权 AFK（可推翻）**

全量重扫实为 11 处（清单后新增 5 处：SessionArchiver/ExperimentBucketer/EvalRunner/EvalDatasetStore/SessionExportChecksum——同型 MessageDigest.getInstance 不可达路径）。形状：统一迁移 BuzhouException(ErrorCode.CONFIG_INVALID, FATAL, "SHA-256 摘要不可用（JVM 环境缺陷）", e)（spec 50 §A 封口同批 ArgumentFingerprint/ReadIntegrity 已合规的先例形态，结构化错误码可分类可观测）；AuditChain.verifySignature（private 零调用，逻辑已一字不差迁 AuditChainVerifier.SignatureOps）删除。
