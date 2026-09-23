---
id: T6053
title: R 会话 R27 推测执行的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

机器性 straggler 拖死尾部怎么兜？（spec 4026 / effort #4026 / R27）

## Resolution

**SpeculativeStragglerPolicy（core/exec）**：Spark speculation——
显著慢于同伴中位 ×multiplier（中位抗离群）且进度落后 threshold
（快完成的慢任务豁免不白烧）才启副本竞争。纯裁决件。与 Hedged
requests 同思想不同面（任务级 vs 请求级）。
