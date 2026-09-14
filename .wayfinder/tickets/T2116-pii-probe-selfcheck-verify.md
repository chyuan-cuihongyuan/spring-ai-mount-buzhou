---
id: T2116
title: PII 合成探针召回标定与误报哨兵的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2115
created: 2026-09-14
---

## Question

如何证明探针口径、基线标定与只读可重复？

## Resolution

**用户常设授权 AFK（可推翻）**

`PiiProbeSelfCheckTest` 四测全绿（`mvn -pl buzhou-guard -am test`）：报告结构与典序；**基线标定锚**（内建池×内建检测器=全召回 overallRecall=1.0——评审修正：初版卡号样本未过 Luhn 校验被检出 6/7，换业界通用测试 PAN 后全绿）；负例误报恒 0；同 detector 重复探针逐位一致（纯函数）。
