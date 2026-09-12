---
id: T1050
title: G 会话 700 系收口的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1049
created: 2026-09-13
---

## Question

50/50 轮闭环？全仓终验绿？台账全闭环？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 50 轮）：① 全仓 `mvn -B -ntp clean verify` 16 模块全 SUCCESS（四硬门全过）；② effort-700 map 50 轮台账全 ✅ + Destination 达成；③ 票 T951–T1050 百张全闭环；④ spec 700–749 连续 50 份；⑤ impl 535–552（G 段 535–552）对账一致；⑥ 收口提交推送 GitHub。
