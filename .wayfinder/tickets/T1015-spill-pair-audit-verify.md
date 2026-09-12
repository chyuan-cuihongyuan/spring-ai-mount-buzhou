---
id: T1015
title: spill 双文件配对完整性巡检验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1014]
created: 2026-09-12
---

## Question

配对判定与统计口径如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 8 轮 = effort #707）：@TempDir 植文件四用例——健康对不报+孤 data 带精确字节+孤 meta 报 META_WITHOUT_DATA+总数统计；root 缺席空 Report；null fail-fast。buzhou-spill 全模块零回归（C 会话排除集）。
