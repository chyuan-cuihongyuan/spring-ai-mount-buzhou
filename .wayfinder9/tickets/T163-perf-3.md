---
Type: task
Status: closed
---
## Question

加密读写/单飞闸/读降级三面的 perf 哨兵（阈值宽松、只拦回归）与 baseline 落档如何定？

## Resolution

AFK 自决：三哨兵——加密往返（64KB AES-GCM store+load，硬顶 150ms）/单飞闸串行开销（CAS，硬顶 5ms）/
读降级路径（异常→空历史，硬顶 10ms）。首轮实测入档 baseline.md；口径同 PerfBaselineTest（10 倍宽幅、
跨机绝对值不可比、越顶=profiling 信号）。产 spec 45 §C + impl-134。
