---
id: V8003
title: V 会话 V2 BoyerMooreSearch 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

子串搜索怎么吃满文本侧信息？（spec 8001 / effort #8001 / V2）

## Resolution

**BoyerMooreSearch（core/metrics）**：坏字符最右位表+强好后缀
经典 shift 构造双启发取大滑动；findAll 全部（含重叠）命中；
空模式 fail-fast。
