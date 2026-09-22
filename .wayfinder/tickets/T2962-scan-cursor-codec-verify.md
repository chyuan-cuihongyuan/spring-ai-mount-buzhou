---
id: T2962
title: SCAN 游标编解码的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2961]
created: 2026-09-23
---

## Question)

全周游在序列/不重/递增性/畸形四面下正确吗？（spec 1880 / effort #1880 / R81）

## Resolution`

**ScanCursorCodecTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ScanCursorCodecTest）：bits=2 全周游序 0→2→1→3→0；bits=3
周游长 8 桶不重覆盖 1..7；反转空间严格递增性质；cycleLength 幂与
畸形三型（负游标/位宽 0/位宽 64）fail-fast。
