---
id: T2918
title: 频次衰减竞速的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2917]
created: 2026-09-16
---

## Question]

衰减竞速在序列/三档/零例/畸形四面下正确吗？（spec 1858 / effort #1858 / R59）

## Resolution`

**FrequencyDecayTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=FrequencyDecayTest）：100→50→25→12 序列+封底；顶替 3/1/5（首跑
红为心算期望误 7——按代码算 5，R54 入档病理第四次实证）；零计数即刻/
零命中 -1；负数四型 fail-fast。

