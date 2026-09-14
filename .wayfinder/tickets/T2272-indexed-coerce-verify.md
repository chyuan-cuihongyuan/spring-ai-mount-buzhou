---
id: T2272
title: ConfigMaps indexed 属性数字键归一的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2271
created: 2026-09-15
---

## Question

M 会话第 12 轮：数字键归一如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=ConfigMapsIndexedCoerceTest` 绿 + ConfigMaps 既有测试零回归——
① properties 源 `tools[0].name/required-state` + `tools[1]...` → sub() 输出 List（两项按数值序，含嵌套归一）；
② 跨十位排序正确（10 项列表 0..9..10 数值序不乱）；
③ 混合键 map（含任一非数字键）保持 Map 不误伤；
④ YAML 文件源既有 List 路径零变化。
