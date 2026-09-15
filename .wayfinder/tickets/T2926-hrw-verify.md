---
id: T2926
title: Rendezvous 哈希的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2925]
created: 2026-09-16
---

## Question]

键归属在确定性/最小迁移/分布/畸形四面下正确吗？（spec 1862 / effort #1862 / R63）

## Resolution`

**RendezvousHashingTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=RendezvousHashingTest）：同参同归属；3→2 节点 200 键全检——迁移
仅发生在原属 n3 的键；300 键 3 节点各 >50 弱均匀；空键/空节点表/空白
节点/null keys fail-fast。

