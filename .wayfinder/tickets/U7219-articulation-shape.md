---
id: U7219
title: U 会话 U10 割点桥检测的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

单点故障怎么一次遍历识别？（spec 7009 / effort #7009 / U10）

## Resolution

**ArticulationPoints（core/concurrent）**：disc/low
低链接一次遍历；根两孩子特判；升序确定性；重边幂等。
