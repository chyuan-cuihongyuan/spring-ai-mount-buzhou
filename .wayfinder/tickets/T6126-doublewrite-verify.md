---
id: T6126
title: S 会话 S13 DoubleWrite 双写缓冲的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6125]
created: 2026-09-24
---

## Question

S13 合同怎么逐一验绿？（spec 5012 / effort #5012 / S13）

## Resolution

**验证通过**：DoubleWriteBufferTest 五测全绿——stage→
recoverable 显影；同页覆盖最新版胜；容量满自动落盘守恒；
flush 清队返回页集；空 data/负容量 fail-fast；确定性回放。
