---
id: T6066
title: R 会话 R33 祖先费率打包的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6065]
created: 2026-09-23
---

## Question

R33 合同怎么逐一验绿？（spec 4032 / effort #4032 / R33）

## Resolution

**验证通过**：AncestorFeerateTest 六测全绿——子 5.5 全池最高
拖整包先于 5.0 单干者入场（父自身包诚实 1.0 不含子孙）；共享
父去重双子只计一次；三链累积（k 包 1400/300）；packageOf
父先子后拓扑；未知父/重复 id/零 size fail-fast；费率并列
id 字典序确定性。
