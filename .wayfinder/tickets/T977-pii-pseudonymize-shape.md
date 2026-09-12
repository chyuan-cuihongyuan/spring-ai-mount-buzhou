---
id: T977
title: PII 格式保持假名化的形态裁决（Kappa ruled-out 后顺延）
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

原列「双 judge Kappa」缺口核查已被 spec 541 JudgeAgreement 完整实现（po/pe/κ + Landis-Koch 分级）——ruled-out 顺延。现有 PII 两面：redact 全占位符（`[PII:TYPE]` 破坏格式/长度）、PiiVault 可逆 token（`[PII-VAULT:hash]` 长度不保）。格式保持遮蔽（Presidio surrogate 思想）怎么落且诚实划界？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 14 轮 = effort #713 / spec 713 / impl 516）：`PiiDetector.pseudonymize(text, enabled)`（additive 方法）——逐命中生成**同长度同字符形态**替身：数字→伪随机数字、字母→同大小写字母、分隔符/空白原样保留。确定性：替身随机流按 `(SURROGATE_SEED 常量, type, 命中文本)` 哈希播种——同 (type,文本) 恒同替身（进程内外一致，可复现测试），线程安全（无共享可变态）。**诚实边界**：保形状不保校验位——身份证 mod-11/银行卡 Luhn 在替身上不再验真（真 FPE/FF1 需密码学实现，非目标）；验真语义属上游入站，遮蔽后校验失败是预期信号非缺陷；不可逆（不存原文——可逆需求归 PiiVault spec 507 互补面）。借鉴 microsoft/presidio 格式保持 surrogate 思想。
