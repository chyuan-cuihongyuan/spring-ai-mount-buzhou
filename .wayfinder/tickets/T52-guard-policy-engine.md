---
id: T52
title: guard · policy-as-code 内嵌子集 + OPA sidecar SPI
type: grilling
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

危险工具门的规则如何从 bespoke 配置升级为可分析、默认拒的 policy？事实源：OPA（12,099★ 达标概念源；但 **JVM 无成熟内嵌**——官方 opa-java 26★ 明言仅 REST 调 sidecar）；cedar-java（75★ 注记备选：唯一 JVM 内嵌引擎，Maven `com.cedarpolicy:cedar-java:4.3.1` JNI+五平台原生库——双不达标）；Cedar 引擎 1,656★ 不达标。

## 待定决策（研究推荐 + 取舍）

1. **内嵌自有可分析子集**：声明式 JSON 规则（主体 × 工具 × 资源 × label 谓词 → allow/deny/escalate），语义对齐 OPA「结构化 input → 决策 + reason」模型，默认拒——推荐采纳（与既有危险工具门同构、是其泛化；零不达标依赖）。
2. `PolicyEngine` SPI + **OPA sidecar adapter**（opa-java optional 依赖）给要 Rego 全表达力的部署方——推荐采纳。
3. cedar-java 注记备选（若未来要求「数学可验证」再评估）——采纳。
4. 规则语言面：label 谓词如何衔接 T49 taint 标签（`context.taint ⊔ args.taint` 可作谓词输入）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §5.5（内嵌子集+SPI 3–5 天 + adapter 2 天，ROI 高）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-24**（用户常设授权 2026-08-14 ratify、可推翻）。内嵌可分析子集（默认拒+reason）+PolicyEngine SPI+OPA sidecar adapter（optional）；cedar-java 注记备选。
