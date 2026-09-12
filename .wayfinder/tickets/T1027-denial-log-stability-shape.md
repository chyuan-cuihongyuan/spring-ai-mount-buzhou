---
id: T1027
title: ToolDenialLog 排序稳定性补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

topDenials 同 count 并列稳定性与环形窗口滑动幂等性需补验。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 39 轮 = effort #737 / spec 737 / impl 541，测试域补验轮）：并列字典序稳定、重复记录累加、窗口滑动幂等三组用例。
