# 23 — guard · policy-as-code 内嵌子集 + OPA sidecar SPI

**What to build:** 危险工具规则升级为默认拒、可分析的 policy：内嵌声明式 JSON 规则子集（主体×工具×资源×label 谓词→allow/deny/escalate，决策附 reason），PolicyEngine SPI + OPA sidecar adapter 给要 Rego 全表达力的部署方。

**Blocked by:** None — can start immediately.（taint label 谓词衔接随 21 联动）

**Status:** done（2026-08-14：PolicyDecision（allow/deny/escalate+reason、Input、Rule、LabelPredicate）+ EmbeddedPolicyEngine（默认拒/首条命中/glob/label 谓词/escalate+审批=allow）+ PolicyEngine SPI（OPA sidecar adapter 预留）+ PolicyGateHook（order 275、taint 标签衔接、policy.decided 事件）；EmbeddedPolicyEngineTest 3 例；guard 44/44 绿；spec 07 新节）

- [ ] 声明式 JSON 规则子集：主体 × 工具 × 资源 × label 谓词 → `allow`/`deny`/`escalate`（→HITL），**默认拒**、决策附 reason
- [ ] `PolicyEngine` SPI；既有危险工具门配置**迁移为子集的自然特例**（兼容不破坏）
- [ ] OPA sidecar adapter（opa-java optional 依赖、探测式启用）
- [ ] label 谓词可表达 taint 标签（与 21 的 TaintLabel 衔接）
- [ ] cedar-java 保持注记备选（不引入）
- [ ] 端到端：默认拒、escalate 走 HITL、reason 可观测；既有配置迁移后行为不变
- [ ] spec 07（Hook 护栏）同步

> spec 12 §guard-24；[T52](../tickets/T52-guard-policy-engine.md)。源：opa 12,099★ 概念（JVM 无成熟内嵌→自有子集）。
