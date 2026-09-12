---
id: T1030
title: PII 格式保形掩码的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

占位符 redact 与 vault 之间缺「保形展示」形态——加结构化掩码原语吗？密码学 FPE 做不做？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 16 轮 = effort #715 / spec 715 / impl 615）：`FormatPreservingMasker` 静态原语——maskPhone（保 3+4）/maskIdCard（保 4+2 尾 X 留）/maskEmail（首字符+域名）/maskIp（前两段）/通用 mask(text,keepHead,keepTail)；形状校验复用 PiiDetector 正则口径（两侧不漂移），校验失败→等长全星 fail-closed 不抛。非密码学 FPE（同入同码——强去标识归 507/加密族）。纯静态不自动挂 redact 管道（行为变更归宿主自选）。
