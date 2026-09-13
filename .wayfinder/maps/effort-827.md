# effort #827 — 定价表覆盖审计

- 会话：H 会话 800 系第 28 轮 ｜ spec [827](../../../docs/spec/827-pricing-coverage-audit.md) ｜ 票 [T1155](../tickets/T1155-pricing-coverage-audit.md)/[T1156](../tickets/T1156-pricing-coverage-audit-verify.md) ｜ impl580
- 借鉴：LiteLLM model_prices 覆盖思想（BerriAI/litellm ≈28K star）——被调用模型必须在价目库内

## 勘察（排重）

- PricingTable（16）：查价+热载——无「被调用模型 vs 表键」覆盖对账。
- CostAttribution*：归因执行面——查不到价即静默 0（缺口即在此暴露）。
- grep -i `coverage`：SpecCoverage 是测试域——定价域缺位。

## 决定

`PricingCoverageAudit`（core.budget，纯函数）：audit(pricedKeys, called)——匹配三层（精确→忽略大小写→剥 provider/ 前缀，LiteLLM 同款形态）；调用名去重+脏名忽略；coverageRatio（空调用=1.0 空真）+unknown 典序封顶 32+「…（共 N 个）」汇总行；空价表全 unknown 比率 0。键集由调用方自 PricingTable 采集（零侵入）。

## 测试

三层匹配（精确+GPT-4O 大小写+openai/gpt-4o 剥前缀）覆盖率 0.75 精确/未知典序+封顶 37→33+汇总行+covered 1/全覆盖 1.0/空调用空真+脏名三形态/空价表比率 0——5 例全绿。

## 诚实边界

键集静态采集（PricingTable 零变更；热载后需重审——时点口径）；匹配不做通配（价表若有通配键需调用方自行展开）；前缀剥离仅一层（多层 provider 前缀不递归）。
