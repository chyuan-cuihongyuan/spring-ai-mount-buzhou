---
id: T1027
title: 数据集标签与过滤验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1026]
created: 2026-09-12
---

## Question

标签幂等/归一/兼容如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 14 轮 = effort #713）：①打标幂等+大小写归一+去标不炸；②listDatasetsByTag 只回命中集按名序；③旧记录（无 tags 字段 JSON）解码空表；④非法 tag fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
