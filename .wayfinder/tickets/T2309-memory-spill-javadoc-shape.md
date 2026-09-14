---
id: T2309
title: memory/spill 公开类型类级 Javadoc 补齐（五-1 扩散）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2257
created: 2026-09-15
---

## Question

M 会话第 33 轮：design-incompleteness 五-1 的 memory/spill 部分（43 个零 Javadoc 公开类型）如何处置？

## Resolution

**用户常设授权 AFK（可推翻）**

实扫 40 缺（memory 21 + spill 19）全部补齐（角色一句话——类名语义+既有方法注释提炼；spec 1529 引注）；R4 的 CoreApiJavadocCoverageTest 门辖 core 六包，模块级门不入本轮（模块测试基线由各模块自持——门扩散留候选池）。两模块测试零回归（187+180）。
