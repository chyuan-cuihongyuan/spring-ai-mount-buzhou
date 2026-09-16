---
id: T3192
title: 属性白名单过滤器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3191]
created: 2026-09-17
---

## Question

AttributeWhitelist 合同（留/丢计数/通配/全拒/畸形）怎么钉住？（spec 2045 / effort #2045 / R46）

## Resolution

**七用例全绿**（首跑 1 红：构造器建 TreeSet 先于空项校验——null 入
TreeMap 即 NPE；先校验后建集修后 7/7）：盘内留盘外 email/prompt 各
计 1 / 通配全留零丢+isAllowAll / 空表全拒显式 / 值同引用原样 /
逐次独立计数 / 快照字典序 alpha<zeta / 畸形四型（null 名单含语义
提示、名单含 null、空白项、null attributes）fail-fast。
