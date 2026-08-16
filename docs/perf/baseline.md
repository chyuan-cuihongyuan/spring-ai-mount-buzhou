# 性能基线（首轮实测）

> effort #5 / T93 / impl-68。基准代码：`examples/src/test/.../perf/PerfBaselineTest.java`（`@Tag("perf")`）。
> CI：日常构建默认排除（`surefire.excluded.groups=perf`）；`perf-nightly` workflow 以
> `-Dgroups=perf` 激活（weekly + manual），报告随工件归档。

## 首轮实测（2026-08-15，Apple Silicon 本机，JDK 21）

| 场景 | P50 | P95 | 哨兵（10 倍冗余硬顶） |
|------|-----|-----|------|
| 100 轮会话端到端（零延迟模型，harness 自身开销） | 0.29 ms/轮 | 0.55 ms/轮 | P95 < 500 ms |
| 微压缩吞吐（500 条工具返回全量逐出） | — | — | ≥ 3,000 msgs/s（实测 ~1.8M msgs/s） |
| 消息存储 append+load-all round-trip（逐轮全量读回） | 0.00 ms | 0.01 ms | P95 < 300 ms |

## 解读规则

1. **跨机器绝对值不可比**——只看同机时间序列趋势（nightly 工件历史对比）。
2. 哨兵为 **10 倍级宽幅硬顶**：越顶说明发生了量级回归（算法退化 / N+1 / 泄漏型变慢），
   触发后应**人工 profiling 定位根因**，不许调阈值了事。
3. 微小波动（<2 倍）视为环境噪声，不追查。


## effort #7 增补哨兵（T125 / impl-100；2026-08-15 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶（10 倍宽幅） |
|------|------|---------|------------------|
| outbox 扫描 | 千条未决积压 append+due(50) 批扫（内存 store，spec 33 §C 前缀路径） | ~2ms | 100ms |
| 索引查询 | 万行 tag+appId 过滤 + 分页 50 | ~4ms | 150ms |
| 导出往返 | 500 消息 toJson+fromJson | ~35ms | 800ms |

同 PerfBaselineTest 口径：跨机器绝对值不可比，只看同机趋势；越顶 = profiling 信号。


## effort #8 增补哨兵（T144 / impl-117；2026-08-15 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶 |
|------|------|---------|------|
| skill_search | classpath 全集子串匹配 | ~1ms | 100ms |
| 死信重放存储面 | 百条 dead 迁回 outbox | ~5ms | 500ms |
| 迁移单会话 | 2 消息跨 runtime 往返 | ~3ms | 300ms |


## effort #9 增补哨兵（T163 / impl-134；2026-08-16 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶 |
|------|------|---------|------|
| spill 加密往返 | 64KB 载荷 AES-GCM store+load 单次 | ~8ms | 150ms |
| 单飞闸开销 | 串行轮次进出闸（CAS 占位/释放） | <1ms | 5ms |
| 读降级路径 | EMPTY 策略读失败→空历史 | <1ms | 10ms |

## effort #10 增补哨兵（T183 / impl-152；2026-08-16 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶 |
|------|------|---------|------|
| TTFT 打点开销 | 带打点的流式端到端消费（spawn→stream→blockLast） | <5ms | 50ms |
| rateTurn 写入 | 单次反馈（校验+state put+事件） | <1ms | 20ms |
| 候选限流闸 | RPM 预检+扣减单次 | <0.1ms | 5ms |
| shadow 提交 | submit 即发即忘（护栏+虚拟线程启动） | <1ms | 20ms |

## effort #11 增补哨兵（T197 / impl-163；2026-08-16 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶 |
|------|------|---------|------|
| 评估 runner 全链路 | 3 项数据集完整 run（spawn×4+chat×3+打分+记录写） | <5ms | 80ms |
| 数据集 scan | 50 项 items() + 全量 run 摘要查询 | <2ms | 40ms |
| 反馈回流导入 | 单会话 scan 反馈+历史读+负轮写入（幂等路径） | <2ms | 40ms |

## effort #12 增补哨兵（T209 / impl-174；2026-08-16 首轮实测）

| 哨兵 | 场景 | 首轮 P95 | 硬顶 |
|------|------|---------|------|
| 缓存 call 命中路径 | advisor 查键+重放包装（spawn+chat 全链路） | <2ms | 15ms |
| 缓存键计算 | 10 条消息 sha256 规范序列化 | <0.5ms | 10ms |
| 流式命中重放 | Flux.just 订阅消费（spawn+stream+blockLast） | <3ms | 20ms |
