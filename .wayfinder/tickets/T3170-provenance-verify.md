---
id: T3170
title: 工具溯源索引的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3169]
created: 2026-09-17
---

## Question

ToolProvenanceIndex 合同（双向账/孤儿/覆盖/冲突面/畸形）怎么钉住？（spec 2034 / effort #2034 / R35）

## Resolution

**七用例一次全绿**（buzhou-mcp）：双向账（providersOf/server/tool
计数）/ 摘 fs-server 孤儿恰 {read_file,write_file} 且旁源 fetch_page
不动 / 共供 search 摘 s1 不孤儿摘 s2 才孤儿 / 覆盖重注册 a 失源 c
入账不重复注册 / 三源 search 冲突面 3 且 solo 不入 / 摘未知 server
空 / 畸形七型（null/空 server、null 名单、名单含 null/空白、null
unregister、null 查询）fail-fast。
