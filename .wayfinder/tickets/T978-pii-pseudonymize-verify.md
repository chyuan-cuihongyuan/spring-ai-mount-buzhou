---
id: T978
title: PII 格式保持假名化的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T977
created: 2026-09-13
---

## Question

形状真保（长度/字符类/分隔/大小写）？确定性（同输入恒同输出）？不可逆（原文不留痕）？redact 零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 14 轮）：① 手机号/身份证/邮箱各一——输出长度相等、数字位仍是数字、`-`/空格原样、字母大小写保持（邮箱）；② 同文本两次 pseudonymize 输出逐字符相同（确定性）；③ 输出不含原文片段（数字序列不同）；④ 非 PII 文本原样返回；⑤ 既有 redact/scan 用例零回归。`mvn -pl buzhou-guard -am test` 全绿。
