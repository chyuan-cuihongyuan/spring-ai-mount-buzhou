---
id: T2809
title: 前缀块命中读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

前缀复用底子多厚怎么量化？（spec 1804 / effort #1804 / R5）

## Resolution

**vLLM block-level prefix cache 语义纯读面 `PrefixBlockHitStats`
（buzhou-resilience/cache）**：`analyze(blockSize, prompts)` 等长块切分账
BlockReport（totalBlocks/reusedBlocks + blockHitRatio/blockMissRatio，零入账
-1 哨兵）；尾块不足整块不入账（partial block 不缓存）；同请求内重复块只记
首见（跨请求再见才计复用——radix 一次插入语义）。索引全调用局部（纯函数
无静态状态，测试含无状态性回归钉死）。

