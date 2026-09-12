---
id: T939
title: JSON 拼接收口的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T938
created: 2026-09-13
---

## Question

四处改造后行都合法？注入字符回读得原值？既有行形态（字段序/正常值）零漂移？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 45 轮）：① 四处对抗用例：注入 `"`、`\`、`\n`、`\t` 的字段值 → Jackson 读回该行 JSON 断言字段原值；② 正常值行形态与既有快照用例零回归（字段序稳定）；③ `mvn -pl buzhou-core -am test` 全绿。
