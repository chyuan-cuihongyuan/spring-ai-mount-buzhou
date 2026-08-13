# 实现纵切片索引（spec 12 → impl）

源 Spec：[docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md)；决策票：[tickets/](../tickets/)（T28–T54 已闭合）。每片自带验收标准与 Spec 同步义务；状态：`ready-for-agent` / `done`。

| # | 切片 | 模块 | Blocked by | 状态 |
|---|------|------|-----------|------|
| [01](01-fake-chat-model.md) | FakeChatModel + record/replay 测试基建 | core | — | done |
| [02](02-evict-ratio.md) | evictRatio 部分逐出保连续 | memory | — | done |
| [03](03-head-tail-window.md) | head+tail 窗口回读风味 + 显式中段标记 | spill | — | done |
| [04](04-args-validation-retry.md) | 参数 schema 校验 + per-turn 重试预算 | core | — | ready-for-agent |
| [05](05-cancel-mode.md) | CancelMode 三档 + token 贯穿 | core | — | ready-for-agent |
| [06](06-run-registry.md) | 持久 Run 注册表 + 枚举续跑（lease 门） | core | — | ready-for-agent |
| [07](07-event-sourced-tool-log.md) | 事件溯源工具调用日志 + 幂等键 | core | 06 | ready-for-agent |
| [08](08-interrupt-resume.md) | HITL interrupt/resume 按 toolCallId | core | 07 | ready-for-agent |
| [09](09-timetravel-fork.md) | time-travel fork（Completed-Turn 检查点） | core | 07 | ready-for-agent |
| [10](10-transactional-batch.md) | 事务性并行批——批提交语义 | core | 07 | ready-for-agent |
| [11](11-sleeptime-consolidation.md) | sleep-time 后台整理 | memory | — | ready-for-agent |
| [12](12-memory-tools-antipoisoning.md) | memory-as-tools 自愈记忆 + 防投毒 | memory | — | ready-for-agent |
| [13](13-compaction-checkpoint.md) | 压缩前检查点与三档回滚 | memory | — | ready-for-agent |
| [14](14-fidelity-eval.md) | 压缩保真度 eval | memory | 01 | ready-for-agent |
| [15](15-vector-recall-search.md) | 向量 recall 四模搜（pgvector 单库） | memory | — | ready-for-agent |
| [16](16-context-clearing.md) | context-clearing + 显式逐出 | spill | — | ready-for-agent |
| [17](17-chunk-hash-verify.md) | 内容寻址 chunk hash 回读校验 | spill | — | ready-for-agent |
| [18](18-semantic-readback.md) | 语义回读第 4 模式（locate→fetch） | spill | 15 | ready-for-agent |
| [19](19-ast-aware-slicing.md) | AST-aware 切片（JavaParser + 回退） | spill | — | ready-for-agent |
| [20](20-promptfoo-redteam.md) | CI 自动红队门（promptfoo nightly） | guard | — | ready-for-agent |
| [21](21-fides-taint.md) | FIDES 最小 taint（标 + 写门校验） | guard | 12 | ready-for-agent |
| [22](22-ecdsa-audit-trail.md) | ECDSA 签名审计链（AAT + JCS） | guard | — | ready-for-agent |
| [23](23-policy-engine.md) | policy-as-code 子集 + OPA sidecar SPI | guard | — | ready-for-agent |
| [24](24-onnx-classifier.md) | ONNX 分类器（默认关） | guard | — | ready-for-agent |
| [25](25-command-sandbox.md) | CommandSandbox SPI + Deno 档 | guard | — | ready-for-agent |
| [26](26-episodic-fewshot.md) | episodic memory few-shot | memory | 15 | ready-for-agent |
| [27](27-final-verification.md) | 收口：全量验证 + 落地记录回写 | 全部 | 01–26 | ready-for-agent |

**frontier**（无未闭合 blocker）：01–06、11–13、15–17、19、20、22–25。推荐实现序（研究 ROI）：**01（地基）→ 02/03（廉价 wins）→ 04 → 05 → 20 → 06+07 → 11 → 12 → 21 → 22 → 13 → 14 → 15 → 18 → 17 → 16 → 08 → 23 → 10 → 24 → 25 → 19 → 26 → 27**。
