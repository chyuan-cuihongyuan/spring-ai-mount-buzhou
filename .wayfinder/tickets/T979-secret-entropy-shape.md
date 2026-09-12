---
id: T979
title: 秘密扫描熵阈值过滤的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SecretScanner（spec 400）纯签名正则——文档示例键（AKIAIOSFODNN7EXAMPLE）、占位串（sk-aaaa…）同样命中，redact 误杀正常内容。truffleHog 香农熵过滤怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 15 轮 = effort #714 / spec 714 / impl 517）：SecretScanner 增可选熵阈值构造（默认 null=关，既有两构造零变化）——命中文本的 Shannon 熵（字符频率，bits/char）低于阈值即丢弃该命中（疑似占位/示例非真密钥）；`DEFAULT_MIN_ENTROPY=4.0`（truffleHog base64 域 3.0 收紧一档：真随机键 4.5+，AWS 文档示例 ≈3.7 恰被滤）。**PRIVATE_KEY_BLOCK 豁免**——其签名是 BEGIN 行字面（结构性事实），熵过滤会误滤低熵行。借 trufflesecurity/trufflehog entropy detection。
