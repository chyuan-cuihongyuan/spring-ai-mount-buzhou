---
id: T2998
title: LRU-K 驱逐的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2997]
created: 2026-09-23
---

## Question)

驱逐在扫描抗性/退化/并列/畸形下正确吗？（spec 1898 / effort #1898 / R99）

## Resolution`

**LruKEvictionTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=LruKEvictionTest）：K=2 扫描键先逐热键保住；K=1 退化 LRU 序；
同候选取倒数第 K 次最早；畸形两型（K=0/时间倒退）fail-fast。
