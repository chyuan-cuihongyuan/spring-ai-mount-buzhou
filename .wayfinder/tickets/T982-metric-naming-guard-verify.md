---
id: T982
title: 指标命名规范守卫测试的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T981
created: 2026-09-13
---

## Question

存量 316 名全过（守卫非恒绿——注入违规可红）？配置键/文案不误伤？规则与既有形态兼容？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 16 轮）：① 规则单测：合法（buzhou.tool.calls / buzhou.archive.pdb-rejected）与非法（大写段/空格/双点/驼峰）各自判定正确；② 提取器单测：合成源码片段（含 counter/timer 调用点 + METRIC 常量 + 干扰项配置键/日志文案）只提取调用点与常量；③ 全仓扫描断言零违规（存量实测通过）。`mvn -pl buzhou-spring-boot-starter -am test` 全绿。
