# 实现纵切片（Implementation slices）总索引

`.wayfinder/impl/` 是各 effort 总纲 spec 经 `/to-tickets` 切成的 **tracer-bullet 实现纵切片**——每片是单次上下文窗口可完成、独立可验证的端到端深化。与 [`../tickets/`](../tickets/) 的**决策票**共存：凡实现片被一个未决决策门控，该决策票即列为它的 blocker。

## 编号史（2026-08-17 融合为单一目录后的口径）

- **effort #1**（[SPEC.md](../SPEC.md) → 切片）：**01–08**，当时局部编号。
- **effort #2**（[docs/spec/12](../../docs/spec/12-perfect-adoption.md) → 切片）：**01–27**，当时局部编号——与 effort #1 同号段，文件名以 slug 区分、无冲突。
- **effort #3 起全局续号**：#3 = 28–43 ｜ #4 = 44–55 ｜ #5 = 56–77 ｜ #6 = 78–86 ｜ #7 = 87–104 ｜ #8 = 105–121 ｜ #9 = 122–138 ｜ #10 = 139–155 ｜ #11 = 156–167 ｜ #12 = 168–177 ｜ #13 = 178–184（#4 起不再维护独立索引文件，状态自带于切片）。
- **effort #14/#15**：impl-185…195 **无独立切片文件**（切片记录随决策票与 commit）。
- **下一片 = 196**。

## effort #1 索引（SPEC → impl 01–08）

### 依赖图

```
01 CI 转绿 ─────┬─► 05 可运行 demo ◄── 决策 T4
               ├─► 06 真实 LLM 测试 ◄── 决策 T5
               └─► 08 四机制深度测试 ◄── 决策 T3

02 .scratch 移出        （无 blocker）
03 README 降级          （无 blocker）
04 Spring AI 边界文档    （无 blocker，T2 已闭合）
07 run_command 安全默认 ◄── 决策 T6
```

| # | 标题 | Blocked by |
|---|------|-----------|
| [01](01-ci-green-on-clean-runner.md) | CI 在干净 GitHub runner 上稳定转绿 | 无 |
| [02](02-untrack-scratch.md) ✅ done | `.scratch/` 移出 git 跟踪 + `.gitignore` | 无 |
| [03](03-readme-wording-downgrade.md) ✅ done | README「生产」措辞降级 | 无 |
| [04](04-spring-ai-boundary-doc.md) ✅ done | 撰写「与 Spring AI 原生能力边界」文档 | 无（T2 已闭合）|
| [05](05-runnable-main-demo.md) ✅ done | 提供真正可运行的 `src/main` demo | 决策 T4 |
| [06](06-real-llm-integration-test.md) ✅ done | 加入至少一条真实 LLM 行为的集成测试 | 决策 T5 |
| [07](07-run-command-safe-default.md) ✅ done | `run_command` 原子工具安全默认 | 决策 T6 |
| [08](08-depth-tests-four-mechanisms.md) ✅ done | 四机制「做深做透」深度测试基线 | 决策 T3 |

（原「Frontier」清单 8 片全部 done，并入上表状态列。）

## effort #2 索引（spec 12 → impl 01–27）

源 Spec：[docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md)；决策票：[tickets/](../tickets/)（T28–T54 已闭合）。每片自带验收标准与 Spec 同步义务；状态：`ready-for-agent` / `done`。

| # | 切片 | 模块 | Blocked by | 状态 |
|---|------|------|-----------|------|
| [01](01-fake-chat-model.md) | FakeChatModel + record/replay 测试基建 | core | — | done |
| [02](02-evict-ratio.md) | evictRatio 部分逐出保连续 | memory | — | done |
| [03](03-head-tail-window.md) | head+tail 窗口回读风味 + 显式中段标记 | spill | — | done |
| [04](04-args-validation-retry.md) | 参数 schema 校验 + per-turn 重试预算 | core | — | done |
| [05](05-cancel-mode.md) | CancelMode 三档 + token 贯穿 | core | — | done |
| [06](06-run-registry.md) | 持久 Run 注册表 + 枚举续跑（lease 门） | core | — | done |
| [07](07-event-sourced-tool-log.md) | 事件溯源工具调用日志 + 幂等键 | core | 06 | done |
| [08](08-interrupt-resume.md) | HITL interrupt/resume 按 toolCallId | core | 07 | done |
| [09](09-timetravel-fork.md) | time-travel fork（Completed-Turn 检查点） | core | 07 | done |
| [10](10-transactional-batch.md) | 事务性并行批——批提交语义 | core | 07 | done |
| [11](11-sleeptime-consolidation.md) | sleep-time 后台整理 | memory | — | done |
| [12](12-memory-tools-antipoisoning.md) | memory-as-tools 自愈记忆 + 防投毒 | memory | — | done |
| [13](13-compaction-checkpoint.md) | 压缩前检查点与三档回滚 | memory | — | done |
| [14](14-fidelity-eval.md) | 压缩保真度 eval | memory | 01 | done |
| [15](15-vector-recall-search.md) | 向量 recall 四模搜（pgvector 单库） | memory | — | done |
| [16](16-context-clearing.md) | context-clearing + 显式逐出 | spill | — | done |
| [17](17-chunk-hash-verify.md) | 内容寻址 chunk hash 回读校验 | spill | — | done |
| [18](18-semantic-readback.md) | 语义回读第 4 模式（locate→fetch） | spill | 15 | done（共享 EmbeddingProvider 先行落地） |
| [19](19-ast-aware-slicing.md) | AST-aware 切片（JavaParser + 回退） | spill | — | done（Java AST-lite 零依赖版） |
| [20](20-promptfoo-redteam.md) | CI 自动红队门（promptfoo nightly） | guard | — | done |
| [21](21-fides-taint.md) | FIDES 最小 taint（标 + 写门校验） | guard | 12 | done |
| [22](22-ecdsa-audit-trail.md) | ECDSA 签名审计链（AAT + JCS） | guard | — | done |
| [23](23-policy-engine.md) | policy-as-code 子集 + OPA sidecar SPI | guard | — | done |
| [24](24-onnx-classifier.md) | ONNX 分类器（默认关） | guard | — | done |
| [25](25-command-sandbox.md) | CommandSandbox SPI + Deno 档 | guard | — | done |
| [26](26-episodic-fewshot.md) | episodic memory few-shot | memory | 15 | done |
| [27](27-final-verification.md) | 收口：全量验证 + 落地记录回写 | 全部 | 01–26 | done |

**全部 27 片 done（2026-08-14）**。验证：`mvn -B -ntp clean verify` 16 模块 BUILD SUCCESS（576 tests / 0 fail / 0 err / 30 skip 门控）；落地记录见 [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md)「落地记录」节。

## effort #3 索引（spec 13 → impl 28–43）

来源：[docs/spec/13-production-hardening.md](../../docs/spec/13-production-hardening.md)（/to-tickets 切出）。**28–43 全部落地**（2026-08-15）：生产收口纵切片闭合；spec 同步见 docs/spec/00/01/05/07/09 各档「生产收口/存储运维」节。

| # | 切片 | Blocked by | 状态 |
|---|------|-----------|------|
| [28](28-deadline-hang-immunity.md) | core · Turn Deadline 贯穿 + 挂起免疫 + 故障注入构件 | — | done |
| [29](29-error-taxonomy-logging.md) | 横切 · 异常分类体系 + 错误码 + 日志基线 | — | done |
| [30](30-graceful-shutdown.md) | core · 优雅停机与生命周期 | 28 | done |
| [31](31-schema-migration.md) | stores · Schema 版本化迁移 + MySQL 幂等 + 恢复设施装配 | — | done |
| [32](32-transaction-correctness.md) | stores · 事务接线 + 并发正确性 + 降级语义 | 31 | done |
| [33](33-lease-renew-fence.md) | core · 租约续租 + LeaseLost + 写路径 fence | 28, 29 | done |
| [34](34-event-backpressure-thread-hygiene.md) | core · 事件背压 + 线程卫生 | 29 | done |
| [35](35-cascade-cleanup.md) | 横切 · deleteSession 级联清理 + SessionCleaner | 31 | done |
| [36](36-inmemory-bounds-quota.md) | stores · InMemory 有界化 + 容量配额 | 29, 35 | done |
| [37](37-retention-sweeper.md) | 横切 · 保留策略族 + RetentionSweeper + 触发公式 | 35, 36 | done |
| [38](38-spill-lifecycle-embedding-cache.md) | memory+spill · 增长治理 + embedding 缓存 + 后台任务治理 | 37, 29 | done |
| [39](39-audit-persistence-keyring.md) | guard · 审计链持久化 + 密钥版本化轮换 + 独立校验 | 29 | done |
| [40](40-policy-reload-sandbox-limits.md) | guard · policy 热加载 + 沙箱限额 | 39 | done |
| [41](41-leak-health-metrics.md) | 横切 · 泄漏检测 + 健康检查 + 指标 | 29, 34, 40 | done |
| [42](42-config-surface.md) | 横切 · 配置全参数化 + 启动校验 + FailureAnalyzer + 默认值安全化 | 41 | done |
| [43](43-metadata-resilience-final.md) | 收口 · 配置元数据 + 韧性矩阵补齐 + 终验 | 42 | done |

## 切片约定

- 每片遵循 `/to-tickets` 的 local-ticket 模板：`**What to build**`（端到端行为，非逐层实现清单）/ `**Blocked by**` / `**Status: ready-for-agent**` / 勾选式验收标准。
- 领取：开工前把 `Status` 改 `claimed`（或加 `Assignee:`）；解决：勾完验收标准 + 末尾补 `## Resolution`（或 `## Done`：commit 号 + 验证方式），`Status` 改 `done`。
- 不写具体文件路径 / 代码片段（易过期）；决策性骨架在总纲 spec 与决策票内。
