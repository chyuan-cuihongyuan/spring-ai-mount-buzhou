---
id: T6069
title: R 会话 R35 JSON Patch 应用的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

文档状态怎么标准化增量化编辑？（spec 4034 / effort #4034 / R35）

## Resolution

**JsonPatchApplier（core/policy）**：RFC 6902 六操作全量
（add/remove/replace/move/copy/test）+ RFC 6901 指针（转义/
下标/- 追加）+ 原子应用（深拷贝先行，失败整patch拒）；
move 数组下标记账诚实处理。
