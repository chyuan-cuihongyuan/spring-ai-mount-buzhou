# 746 — 能力审计按能力维度聚合

> 来源：G 会话第 47 轮 = effort #746（700 audit 维度深化）/ [T1094](../../.wayfinder/tickets/T1094-deny-by-capability.md) / [T1095](../../.wayfinder/tickets/T1095-deny-by-capability-verify.md) / impl 645 续。

## Problem

denyByModel 回答「谁被拒」——容量规划还需要「什么能力缺失」：vision 拒绝集中说明该补视觉模型声明/换视觉模型；tools 拒绝集中说明该走 function-calling 支持的路由。单维聚合决策依据不足。

## Solution

Report 增 `denyByCapability`（vision/tools → 次数，snapshot 时从 recent 环即时聚合）——与 denyByModel（谁被拒）正交双视角。

## Out of Scope
按 (model×capability) 交叉表（组合爆炸——两维各自足够）。
