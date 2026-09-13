---
id: T1313
title: ExportManifest 规范化摘要的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 35 轮：ExportManifest（spec 193）逐会话摘要基于原始 JSON 文本 strip——嵌套键序漂移会让同内容 manifest 误报 mismatch。911 规范化思想是否应扩散到 manifest？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 35 轮 = effort #935 / spec 935 / impl 687）：扩散成立（911 同构）。落点：① `SessionExportChecksum` 的树规范化逻辑提为包级静态 `canonicalJson(String)`（原 canonicalContentFingerprint 复用同函数——单点实现）；② `ExportManifest.addCanonical(sessionId, contentJson)`：规范化后 sha256 登记（与既有 `add` 并存——既有 manifest 校验状态兼容不被破坏）；verify 语义不变（digest 值来源透明——同批登记用同一 add 方法即自洽）。测试：键序漂移 addCanonical 摘要相同 + verify 通过；旧 add 行为逐字节不变。
