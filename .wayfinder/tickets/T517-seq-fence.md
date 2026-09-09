---
Type: task
Status: closed
---
## Question

信封单调 seq + 接收方四裁决围栏。

## Resolution

done（2026-08-30）：impl-292；forwarder 信封注入进程内 seq（重启=新纪元）+
SequenceFence（CONTINUE/GAP/DUPLICATE/RESET，无 seq 兼容放行）。
