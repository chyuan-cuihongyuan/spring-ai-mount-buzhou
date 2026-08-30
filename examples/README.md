# buzhou-examples

运维排障 Agent 的端到端 demo 与摘要质量评测（ticket 21）。一个排障场景串起 Buzhou Harness 全部机制，忠于携程 Spring-Ai-Trip 蓝本：长日志/大查询天然触发 Spill 与微压缩，改库/重启天然触发 HITL 守卫。

## Quickstart

环境：JDK 21+、Maven 3.9+（本机首次构建见仓库根 `CLAUDE.md` / `CONTRIBUTING.md` 的 `settings.xml` 说明）。

```bash
# 一条命令跑通四簇 demo + 评测（-am 连带构建依赖模块）
mvn -pl examples -am test
```

CI 沿用根目录 `mvn verify`，评测阈值断言自动纳入回归门禁。

## 四簇 demo（`src/test/.../demo/`）

同一排障 Agent 下四个测试类，各侧重一簇机制，全部用脚本化 `ChatModel`（`ScriptedChatModel`，随 `buzhou-core` test-jar 发布）驱动，不依赖真实模型与外部服务。

> 【扩充 2026-08-17】后续 effort 已在 `demo/` 扩充至 31 个测试类（Effort6/8/9/10/11/12/15 能力演示、缓存/语义缓存/共享限流双实例，及 ArgsValidationRetry / BoundedTurn / CancelMode / DeadlineHang / DeleteSessionCascade / EventBackpressure / GracefulShutdown / LeaseSteal 等端到端面）；下表四簇为首发核心。

| 测试类 | 演示机制 |
|---|---|
| `MemoryCompactionDemoTest` | 记忆压缩链：旧轮大工具返回微压缩为 evidence 占位符 + `read_evidence` 回查；超预算触发九段摘要、P0 三段锚定；大返回超阈值 Spill 落盘 + `read_range` 回读 |
| `ObservabilityReplayDemoTest` | 可观测回放：会话⊃轮次⊃模型/工具调用 Span 树（并发不串味）；按轮注入快照还原「模型实际所见」；思维链按 provider key 采集 |
| `GuardAndHitlDemoTest` | 护栏与 HITL：危险 `run_command` 阻断 → 确认事件经会话监听器透出 → 授权 → 放行；授权写入会话 state，跨实例续跑即放行 |
| `SkillAndMcpDemoTest` | 能力供给：classpath Skill 清单注入 + `load_skill` 按需取正文；MCP server 清单 `replaceAll` 差量热更（增删零重启） |

> 跨机制的端到端测试只能由本聚合模块承载（feature 模块间禁止直接依赖，见 `docs/spec/09`）。共享夹具在 `src/test/.../support/TroubleshootingFixture`（排障历史构造、九段摘要、要点常量、`fixedTool`）。

## 摘要评测（`src/test/.../evaluation/SummaryEvaluationTest`）

排障场景 20+ 轮混合大小工具返回会话，测四指标并打印报告（`System.out`）：

| 指标 | 阈值 | 测量方式 |
|---|---|---|
| P0 信息保留率 | ≥ 95% | 压缩后九段 USER_INTENT / CURRENT_STATE / NEXT_STEP 三段逐条比对预埋要点 |
| 关键事实召回率 | ≥ 90% | 预埋事实探针（订单号 / 错误码 / 流水号）在压缩后注入视图的命中率 |
| token 压缩率 | ≤ 40% | 压缩后注入视图 token / 原始全量历史 token（字符启发式） |
| 任务续接成功率 | 通过 | 压缩后续跑一轮，目标（意图 + 订单号）仍在注入视图 |

阈值断言随 `mvn verify` 作 CI 门禁。

### LLM-as-judge 方法论（不在 CI 强制）

CI 无真实 LLM，故判官模型与人工抽检作为方法论文档（见 `SummaryEvaluationTest` javadoc）：

- 判官模型与被测模型**不同源**（避免系统性偏差）；
- 输入 = {原始全量历史, 压缩后注入视图, 预埋要点清单, 续跑记录}；
- 要求判官输出 JSON：`{p0_hit:[{item,retained,evidence}], continuation_score:1-5, fact_recall:[{probe,recalled}], rationale}`；
- 每个用例 judge 结果抽样 20% 人工校准。

本地或离线评测时接入判官模型即可启用该层校验。

## 已知限制（ticket 21 范围外，留作扩展）

- **评测数据集三场景**：spec 01 数据集要求排障 / 编码 / 数据分析；当前实现排障基准（忠于蓝本），编码与数据分析场景作为扩展位，复用同一 `TroubleshootingFixture` 模式即可补齐。
- **预算曲线 / 完整两级联动**：`BudgetReport` 为 memory 模块内部类型（examples 不引用 internal）；预算无越限、evidence 回查、两级先后次序在 demo 侧以可观察方式覆盖（`MemoryCompactionDemoTest`），完整 BudgetReport 曲线留作离线评测。
- **续接成功率 ≥90% 比例测量**：CI 脚本化模型下，续接以「压缩后目标（意图+订单号）仍在注入视图」为代理断言；多 case 比例测量需真实模型，归 LLM-as-judge 离线层。
- **脚本化摘要下的指标含义**：CI 用 `ScriptedChatModel`，P0/召回验证「压缩管道触发 + 摘要块注入含要点」（管道正确性）；真实压缩保留质量靠离线 LLM-as-judge。`SummaryEvaluationTest` 先断言 `summaryStore.latest` 已生成 + 摘要块已注入，避免「未压缩也通过」。
