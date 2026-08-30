---
Type: task
Status: closed
---
## Question

run(dataset, evaluator, parallelism) 重载：clamp(1..32)；虚拟线程池 invokeAll；
结果按项 index 聚合；旧签名委托 parallelism=1。

## Resolution

done（2026-08-29）：impl-214；3 例×3 轮（并行等值同序/并发窗口/clamp）。
