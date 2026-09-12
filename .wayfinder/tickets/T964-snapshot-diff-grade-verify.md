---
id: T964
title: API 快照 diff 破坏性分级的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T963
created: 2026-09-13
---

## Question

分类器对 added/removed/混合三类 diff 分级正确？breaking 判定只看 removed？既有门行为（全等失败、regenerate 门控）不变？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 7 轮）：① 纯 added → breaking=false、gradeMessage 含「非破坏」与新增计数；② 纯 removed → breaking=true、失败名前置；③ 混合 → 两侧分级各自正确；④ 既有比对测试（regenerate 门控 + classpath 假设门）行为不变——分类器只改失败信息装配。`mvn -pl buzhou-spring-boot-starter -am test` 全绿。
