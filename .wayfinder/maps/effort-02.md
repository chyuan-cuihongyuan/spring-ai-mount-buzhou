# Wayfinder Map — Buzhou core/memory/spill/guard 做完美（Tier-2/3）

> effort #2，延续 [effort #1](effort-01.md)（effort #1：Tier-1 已落地、T16–T27 闭合）。

## Destination

把 `buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard` 四机制从「Tier-1 对标开源最优」推进到「**做完美**」：**Tier-2 全量落地 + Tier-3 精选落地**，全部以 **GitHub stars ≥ 10K 的开源项目**为采纳事实源（非 OSS 标准/论文只作注记），产出 ready-for-agent 的 **spec 12** 及其实现。到达 = 四机制在测试基建、proactive 恢复、记忆自愈与防投毒、回读正确性与语义定位、形式化信息流控制上达到或超越 2026 年业界最完备水平。

## Notes

- **领域**：Spring AI 2.0.0 之上的 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见仓库 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **KB 门禁**：下钻业务源码前先按仓库根 `AGENTS.md` 的 KB 路由（`.Knowledge/manifest-routing.json`）。
- **每会话**：先读本 MAP → 从 frontier 取一张 ticket → resolve 后回写「Decisions so far」。
- **用户常设授权（2026-08-14）**：全程「不需询问意见、全部按推荐迭代」——决策票允许以 ratify 研究推荐的方式 AFK 闭合（Resolution 注明可推翻）。
- **事实源**：[docs/research/oss-perfect-tier23.md](../../docs/research/oss-perfect-tier23.md)（[T28](../tickets/T28-oss-perfect-tier23-verification.md)，4 并行子 agent 2026-08-14 核验；star 数为 GitHub API 当日精确值）。
- **10K+ stars 政策**：采纳事实源只认 ≥10K OSS；注记源（MSRC FIDES / IETF AAT / Anthropic 文档 / Claude Code 镜像 / Codex-MCP spec 仓）可 informing 设计但不算达标采纳依据；不达标依赖不得进入 classpath（cedar-java/NeMo/Rebuff/PyRIT/garak 等均注记或出界，见研究文档 §1.2/§6.1）。
- **上游刻度**：effort #1 已把 Tier-1 十二项落地（docs/spec/11）；本轮不重做、只强化既有真原创（并行工具+虚拟线程、evidence-id 确定性指针、九段 P0–P3、动态预算、三模回读、读写失败非对称、HITL session-state 授权、确定性事实采集、spotlighting/canary、对账/双时序/增量摘要/compact_now）。
- **tracker 约定**：见 [effort #2 约定存档](readme-effort-02.md)；票号 T28 起全局续用。
- **建造 Spec（ready-for-agent）**：[docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) —— Tier-2 全量 + Tier-3 精选跨四模块 PRD（问题/方案/39 用户故事/27 项实现决策/测试决策/出界）；执行切片 = `impl/`（`/to-tickets` 切出）。
- **测试哲学不变**：好测试只测外部行为；主接缝 = examples 端到端；[T29](../tickets/T29-core-fake-chat-model.md)（FakeChatModel+record/replay）是地基票，多数后续票回归依赖它。

## Decisions so far

- [「做完美」第二轮 OSS 核验（10K+ 门槛 + Tier-2/3 深挖）](../tickets/T28-oss-perfect-tier23-verification.md) — 4 并行子 agent 核验 40+ 仓库 star 数并深挖实现细节，产出 [docs/research/oss-perfect-tier23.md](../../docs/research/oss-perfect-tier23.md)；**关键修正**：LangGraph superstep≠整批回滚、Codex=头尾各半掐中间、MCP poll_token 非标准、graphiti/E2B/promptfoo 实测达标、OPA 无成熟 JVM 内嵌、Mastra/instructor org 迁移；出界清单见研究文档 §6.1。
- **core T29–T35（7 票，ratify → spec 12 §core-1..7）** — [FakeChatModel+record/replay](../tickets/T29-core-fake-chat-model.md)（Phase 0 地基）、[参数 schema 校验+重试预算](../tickets/T30-core-tool-args-validation-retry.md)、[CancelMode 三档](../tickets/T31-core-cancel-mode.md)、[Run 注册表+lease 门](../tickets/T32-core-run-registry.md)、[事件溯源 ToolCallLog+幂等键](../tickets/T33-core-event-sourced-tool-log.md)、[interrupt/resume 按 toolCallId+fork](../tickets/T34-core-interrupt-resume-timetravel.md)、[批提交语义](../tickets/T35-core-transactional-parallel-batch.md)（LangGraph 修正版）。
- **memory T36–T42（7 票，ratify → spec 12 §memory-8..14）** — [evictRatio 0.7+梯子](../tickets/T36-memory-evict-ratio.md)、[sleep-time 后台整理](../tickets/T37-memory-sleeptime-consolidation.md)、[memory-as-tools+防投毒](../tickets/T38-memory-tools-antipoisoning.md)、[压缩保真 eval](../tickets/T39-memory-compaction-fidelity-eval.md)、[压缩前检查点三档回滚](../tickets/T40-memory-compaction-checkpoint.md)、[向量 recall 四模搜](../tickets/T41-memory-vector-recall-search.md)、[episodic few-shot](../tickets/T42-memory-episodic-fewshot.md)。
- **spill T43–T47（5 票，ratify → spec 12 §spill-15..19）** — [head+tail 窗口风味](../tickets/T43-spill-head-tail-window.md)、[context-clearing+显式逐出](../tickets/T44-spill-context-clearing.md)、[chunk hash 回读校验](../tickets/T45-spill-chunk-hash-verify.md)、[语义回读两段式](../tickets/T46-spill-semantic-readback.md)、[AST-aware 切片](../tickets/T47-spill-ast-aware-slicing.md)。
- **guard T48–T53（6 票，ratify → spec 12 §guard-20..25）** — [promptfoo 红队门](../tickets/T48-guard-promptfoo-redteam-gate.md)、[FIDES 最小 taint](../tickets/T49-guard-fides-minimal-taint.md)、[ECDSA 审计链](../tickets/T50-guard-ecdsa-audit-trail.md)、[CommandSandbox 三档（Deno 必做）](../tickets/T51-guard-command-sandbox.md)、[policy 子集+OPA SPI](../tickets/T52-guard-policy-engine.md)、[ONNX 分类器默认关](../tickets/T53-guard-onnx-classifier.md)。
- [spec 12 范围切定与优先级（「做完美」收口）](../tickets/T54-scope-cut-and-priority.md) — Tier-2 全量+Tier-3 精选**全数入范围**，按 Phase 0（地基）→1（廉价 wins）→2（core 恢复链）→3（memory）→4（spill）→5（guard 形式化）→6（长线）排布；Firecracker/E2B 完整实现与 FIDES 二期出界。

## Not yet specified

- **FIDES 二期**（变量隐藏 Hide/Expand、隔离 LLM+约束解码、类型容量格 bool⊑enum⊑string）——待 [FIDES 最小 taint](../tickets/T49-guard-fides-minimal-taint.md) MVP 落地后按效用损失实测再定。
- **embedding provider 抽象的最终形状**——[向量 recall 三模搜](../tickets/T41-memory-vector-recall-search.md)与[语义回读第 4 模式](../tickets/T46-spill-semantic-readback.md)共享基建，实现时定（模型切换/dimension/中文分词等细节随之浮现）。
- **多实例分布式接管**（lease 升级为分布式锁+心跳、Run 注册表跨实例枚举）——[Run 注册表](../tickets/T32-core-run-registry.md)落地后按需求浮现。
- **ONNX 分类器模型分发细节**（HF gated license：用户自备下载路径/校验和/版本钉住）——[分层分类器](../tickets/T53-guard-onnx-classifier.md)实现时定。
- **跨 agent/线程共享记忆块**（Letta shared blocks）——依赖 Buzhou 尚无的 sub-agent/并行 session 架构，另行 effort。
- **压缩保真 eval 的 judge 模型选型与阈值标定**——[保真 eval](../tickets/T39-memory-compaction-fidelity-eval.md) 落地时以既有 `SummaryEvaluationTest` 方法论延伸。

## Out of scope

- **MCP widget/structuredContent 双受众 UI 渲染 + poll_token 下载端点**：Buzhou 是 headless Java 库、无 UI 面（[T28](../tickets/T28-oss-perfect-tier23-verification.md) 研究裁决；Handle 可借鉴 outputSchema 形状作注记）。
- **Rebuff 依赖**（已归档 + 1.5K★；canary 能力 Tier-1 已自研落地）。
- **NeMo Guardrails / guardrails-ai / PyRIT / garak 依赖引入**（均 <10K★；概念已注记萃取；红队门用达标的 promptfoo）。
- **Temporal/Dapr workflow engine 整体引入**：只取事件溯源+幂等键思想，Completed-Turn 恢复语义替代全量 replay。
- **Firecracker / E2B 沙箱档完整实现**（本轮只交付 SPI + Deno 档 + 接口预留）。
- **非 core/memory/spill/guard 模块的做深**（observe/otel/dashboard/mcp/skills/tools 维持现状）、发布 Maven Central、examples 超出既有 demo+集成测试的扩展（沿用 effort #1 边界）。
- **sub-agent / multi-agent 编排架构**（共享记忆块的前提，另行 effort）。
- **多实例分布式接管**（单实例语义先行）。

## Tickets

全部 ticket 已闭合（T28 research 子 agent 闭合；T29–T54 随 spec 12 批准 ratify 闭合，用户常设授权可推翻）。索引：

**研究**
- [「做完美」第二轮 OSS 核验（10K+ 门槛 + Tier-2/3 深挖）](../tickets/T28-oss-perfect-tier23-verification.md) — `research` · ✅ **closed**

**core**
- [core · FakeChatModel + record/replay 确定性测试基建](../tickets/T29-core-fake-chat-model.md) — ✅ **closed**（spec 12 §core-1，Phase 0 地基）
- [core · 工具参数 schema 校验 + per-turn 重试预算](../tickets/T30-core-tool-args-validation-retry.md) — ✅ **closed**（§core-2）
- [core · 显式取消 CancelMode 三档 + token 贯穿](../tickets/T31-core-cancel-mode.md) — ✅ **closed**（§core-3）
- [core · 持久 Run 注册表 + 枚举续跑（lease 复用）](../tickets/T32-core-run-registry.md) — ✅ **closed**（§core-4）
- [core · 事件溯源工具调用日志 + 幂等键](../tickets/T33-core-event-sourced-tool-log.md) — ✅ **closed**（§core-5）
- [core · HITL interrupt/resume 按 toolCallId + time-travel fork](../tickets/T34-core-interrupt-resume-timetravel.md) — ✅ **closed**（§core-6）
- [core · 事务性并行批——批提交语义](../tickets/T35-core-transactional-parallel-batch.md) — ✅ **closed**（§core-7）

**memory**
- [memory · evictRatio 部分逐出保连续](../tickets/T36-memory-evict-ratio.md) — ✅ **closed**（§memory-8）
- [memory · sleep-time 后台整理](../tickets/T37-memory-sleeptime-consolidation.md) — ✅ **closed**（§memory-9）
- [memory · memory-as-tools 自愈记忆 + 防投毒](../tickets/T38-memory-tools-antipoisoning.md) — ✅ **closed**（§memory-10）
- [memory · 压缩保真度 eval](../tickets/T39-memory-compaction-fidelity-eval.md) — ✅ **closed**（§memory-11）
- [memory · 压缩前检查点与三档回滚](../tickets/T40-memory-compaction-checkpoint.md) — ✅ **closed**（§memory-12）
- [memory · 向量 recall 三模搜（pgvector 单库）](../tickets/T41-memory-vector-recall-search.md) — ✅ **closed**（§memory-13）
- [memory · episodic memory few-shot](../tickets/T42-memory-episodic-fewshot.md) — ✅ **closed**（§memory-14）

**spill**
- [spill · head+tail 窗口回读风味 + 显式中段标记](../tickets/T43-spill-head-tail-window.md) — ✅ **closed**（§spill-15）
- [spill · context-clearing（已消费 tool_result → Handle + 显式逐出）](../tickets/T44-spill-context-clearing.md) — ✅ **closed**（§spill-16）
- [spill · 内容寻址 chunk hash 回读校验](../tickets/T45-spill-chunk-hash-verify.md) — ✅ **closed**（§spill-17）
- [spill · 语义回读第 4 模式（locate→fetch 两段式）](../tickets/T46-spill-semantic-readback.md) — ✅ **closed**（§spill-18）
- [spill · AST-aware 切片（JavaParser + 分隔符回退）](../tickets/T47-spill-ast-aware-slicing.md) — ✅ **closed**（§spill-19）

**guard**
- [guard · CI 自动红队门（promptfoo）](../tickets/T48-guard-promptfoo-redteam-gate.md) — ✅ **closed**（§guard-20）
- [guard · FIDES 最小 taint 信息流控制（标 + 写门校验）](../tickets/T49-guard-fides-minimal-taint.md) — ✅ **closed**（§guard-21）
- [guard · ECDSA 签名审计链（IETF AAT + JCS）](../tickets/T50-guard-ecdsa-audit-trail.md) — ✅ **closed**（§guard-22）
- [guard · CommandSandbox 三档选型与默认](../tickets/T51-guard-command-sandbox.md) — ✅ **closed**（§guard-23：SPI+Deno 必做、重载档预留）
- [guard · policy-as-code 内嵌子集 + OPA sidecar SPI](../tickets/T52-guard-policy-engine.md) — ✅ **closed**（§guard-24）
- [guard · 分层分类器（ONNX Prompt-Guard，默认关）](../tickets/T53-guard-onnx-classifier.md) — ✅ **closed**（§guard-25）

**综合**
- [spec 12 范围切定与优先级（「做完美」收口）](../tickets/T54-scope-cut-and-priority.md) — ✅ **closed**（Phase 0–6 排布）

**Frontier**：∅（决策图已走完）。**实现已全部落地**：`.wayfinder/impl/` 27/27 切片 done（2026-08-14），`mvn -B -ntp clean verify` 16 模块 BUILD SUCCESS（576 tests / 0 fail / 0 err / 30 skip 门控）；落地记录与机制 Spec 同步见 [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md)「落地记录」节。
