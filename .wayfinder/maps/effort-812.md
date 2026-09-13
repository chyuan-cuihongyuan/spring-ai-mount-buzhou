# effort #812 — 观测管道内存限流器

- 会话：H 会话 800 系第 13 轮 ｜ spec [812](../../../docs/spec/812-pipeline-memory-limiter.md) ｜ 票 [T1125](../tickets/T1125-pipeline-memory-limiter.md)/[T1126](../tickets/T1126-pipeline-memory-limiter-verify.md) ｜ impl565
- 借鉴：OpenTelemetry Collector memory_limiter processor（open-telemetry 生态）——按内存预算拒收+计数

## 勘察（排重）

- AsyncObservabilityPipeline：queueCapacity 按条数（ArrayBlockingQueue）——无字节/权重维。
- SpillQuota（spill）：spill 存储配额不同域。
- grep -i `memorylimiter|memlimit|inflight.*byte`：无命中。

## 决定

`PipelineMemoryLimiter`（observability）：tryAdmit(weight)——inFlight+weight>max 即拒（CAS 循环并发安全，拒收不记账）+release 归账（updateAndGet 防 0 下穿）；单位无关（字节/字符由调用方权重函数定）；零/负权重恒过不计；max<1 fail-fast；stats 四口径。拒绝语义=信号：丢弃/降级/背压决策归调用方（OTel refuse-and-log 同口径）。

## 测试

恰达上限准入+超限拒收不记账/单项超上限永不入账/release 归账+超发防负+预算完整恢复/零负权重恒过不计 admit/8 线程×500 并发守恒不变量（admitted=1000、refused=3000、inFlight=1000 三方一致）/fail-fast——6 例全绿。

## 诚实边界

权重由调用方估算（本类不测量对象——字符口径与 738 一致的精神）；不执行丢弃策略（只给信号）；单上限无 soft/hard 两档（OTel 有 soft→hard 渐进——单机内存管道一步拒收足够，两档留位）。
