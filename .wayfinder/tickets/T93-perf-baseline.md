---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

性能基准怎么做？现状：零 JMH/计时回归。决策点：轻量计时 harness（不引 JMH——需独立模块+ shade，成本高；用简单 warmup+分位数计时测试，标记 @Tag(perf) 默认 CI 跳过 nightly 跑？）、基准场景（微压缩吞吐、spill 回读延迟、100 轮会话端到端 P50/P95）、阈值（观测不卡门，报告落档 docs/perf/）、防回归解读规则。产出 spec 21 增量 + impl 68。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **形态**：不引 JMH（独立模块 + shade 成本高，且基准目标是**粗粒度回归哨兵**而非微纳秒精度）。examples 模块新增 `PerfBaselineTest`（`@Tag("perf")`），自写 mini-harness：warmup + 计时迭代 + P50/P95 分位数。
2. **CI 分层**：root pom surefire 默认 `excludedGroups=perf`（日常零开销）；`perf-nightly` workflow（weekly + manual）`-Dgroups=perf` 跑基准、产出报告工件。
3. **场景三件**：①微压缩吞吐（500 条工具返回消息 → DefaultMicroCompactor，msgs/s）；②spill 写入+read_range 回读 round-trip（P50/P95 ms）；③100 轮会话端到端（ScriptedChatModel 零延迟模型 → 每轮 wall time P50/P95，度量 **harness 自身开销**——框架回归最敏感信号）。
4. **阈值**：**宽幅硬顶哨兵**（观测档语义：单机绝对值无意义，10 倍级回归才失败——防环境噪声误报），数值写入测试常量并落档 `docs/perf/baseline-<date>.md`（首轮实测记录）。
5. **解读规则**（写进报告模板）：跨机器绝对值不可比；只看同机时间序列趋势；10 倍越顶 = 哨兵触发，需人工 profiling 而非调阈值了事。
