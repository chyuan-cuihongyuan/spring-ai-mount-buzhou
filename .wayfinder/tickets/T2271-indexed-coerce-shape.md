---
id: T2271
title: ConfigMaps indexed 属性数字键归一（R9 副产出发现）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2267
created: 2026-09-15
---

## Question

M 会话第 12 轮：ConfigMaps.sub 对 .properties 源 indexed 属性绑成 Map 形态而非 List 的既有坑如何修复？

## Resolution

**用户常设授权 AFK（可推翻）}

R9 副产出实证（BindProbe）：properties 源 `dangerous-tools[0].name=x` 经 Binder.mapOf(String,Object) 绑成 `{dangerous-tools={0={name=x}}}`（indexed 被当 map key "0"）——guard dangerous-tools 等一切经 fromYml 消费 List 的键在 .properties/命令行/env-var 源下静默失效（YAML 文件源正常）。

形状：normalizeValue 的 Map 分支加数字键归一——键集全为十进制数字（非空、连续性不强求——按数值排序而非字典序避免 10<2）时按数值序转 List；存在任一非数字键保持 Map（真实数字键 map 在 YAML 语义不存在，properties 的 [i] 恒为列表语义，安全）。嵌套递归（列表元素内的 map-of-indexed 同样归一）。
