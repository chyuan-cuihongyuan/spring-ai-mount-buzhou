# impl 1307 — SessionAgeHistogram 会话年龄分桶直方（R8 = effort #1707 / spec 1707 / T2615-T2616）

**What**：实例面 AtomicLongArray 桶式 1h/1d/7d 四桶+eldest 哨戒；Idle 房规镜像。
**Why**：「活多久」治理轴——清理调参依据（Prometheus histogram 思想）。
**Verify**：SessionAgeHistogramTest 3 断言全绿。 **Status**：done（2026-09-15）
