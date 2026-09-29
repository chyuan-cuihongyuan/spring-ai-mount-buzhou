---
id: V8027
title: V 会话 V14 PerfectHash 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

静态键集怎么做到零碰撞定位？（spec 8013 / effort #8013 / V14）

## Resolution

**PerfectHash（core/metrics）**：CHM 两级——一级桶无冲突
参数扫描+二级桶内参数扫描；lookup 命中 [0,n)/缺席 −1 诚实；
构建尝试上限 fail-fast；种子注入确定性。
