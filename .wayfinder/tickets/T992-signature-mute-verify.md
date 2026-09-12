---
id: T992
title: 错误签名已知问题静默标记的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T991
created: 2026-09-13
---

## Question

muted 从 top 消失但 snapshot 照常？unmute 回归？cap 生效？reset 清空？kind 分面过滤同样排除？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 21 轮）：① record 已知签名 + mute → top(n) 不含、snapshot() 计数仍在涨；② unmute → top 回归；③ mute 65 签名 → 第 65 个被拒 false；④ reset() 后 mutedSignatures 空；⑤ top(kind,n) 同样排除；⑥ mutedSignatures 不可变。`mvn -pl buzhou-core -am test` 全绿。
