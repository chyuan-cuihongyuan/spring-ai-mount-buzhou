---
Type: task
Status: closed
---
## Question

`BuzhouProbes`：机制→探针类归类（缺省 readiness；liveness/startup 显式
点名；归类互斥、幽灵机制 fail-fast）+ 三类裁决（DOWN 只来自同类 DOWN、
UNKNOWN 不连累、空类 UP）。

## Resolution

done（2026-09-04）：impl-355；ProbeClass 枚举 + Verdict record 落地，
八用例绿（缺省归类/DOWN 隔离/UNKNOWN 不连累/空类 UP/冲突红/幽灵红）。
