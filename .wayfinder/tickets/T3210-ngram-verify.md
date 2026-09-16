---
id: T3210
title: n-gram 特征提取的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3209]
created: 2026-09-17
---

## Question

NgramExtractor 合同（滑窗序/去重序/短文本/词窗/畸形）怎么钉住？（spec 2054 / effort #2054 / R55）

## Resolution

**八用例一次全绿**（buzhou-core）：abcd 恰 ab/bc/cd / abab 去重 ab/ba
与频次 3 项 / "ab" n=3 整段 / 空文本与纯空白空 / 双词窗 3 项与三词
窗恰 1 / 词数不足返原词 / 单字符 gram / 畸形四型（null ×2、n=0、
n=−1）fail-fast。
