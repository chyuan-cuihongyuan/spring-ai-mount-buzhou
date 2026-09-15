---
id: T2864
title: 对冲延迟策略的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2863]
created: 2026-09-16
---

## Question]

对冲阈值在分位/退守/边界/畸形四面下正确吗？（spec 1831 / effort #1831 / R32）

## Resolution

**HedgeDelayPolicyTest 4 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=HedgeDelayPolicyTest）：40 样本 P95=380（rank 38）；2 样本退守地板
25+乱序 P50=13；边界 100/100 即发；空样本/分位越界/负地板/null 样本/
负裁决入参 fail-fast。初版退守路径负样本漏检，校验前移自查修正。

