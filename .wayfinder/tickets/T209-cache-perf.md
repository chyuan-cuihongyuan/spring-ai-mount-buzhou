---
Type: task
Status: closed
blocked-by: T205-cache-stream.md
---
## Question

哨兵：命中路径开销（应 <1ms 量级）/ 键计算开销（sha256 规范序列化）/ 流式命中重放开销；
baseline 落档。

## Resolution

impl-174 落地：三哨兵（命中路径/键计算/流式重放）首轮 <3ms；baseline 增表。T209 关闭。
