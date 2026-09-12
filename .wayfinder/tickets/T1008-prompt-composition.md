---
id: T1008
title: 提示词角色构成拆解读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

181 水位只答「占多少」不答「谁在吃」。加角色构成拆解吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 5 轮 = effort #704 / spec 704 / impl 604）：`PromptComposition.analyze(Prompt)` 纯函数——按 MessageType 角色聚合 chars/messages/share（total=0 诚实零），sections 字符降序+字典序稳定；core.message 落点（Role 同域）。字符口径与 181 同边界（token 归装配侧）；纯读数不拦截。
