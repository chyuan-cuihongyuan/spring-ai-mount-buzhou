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
