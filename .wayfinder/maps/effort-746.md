# effort #746 — 能力审计按能力维度聚合（700 深化）

- 会话：G 会话 700 系第 47 轮 ｜ spec [746](../../../docs/spec/746-deny-by-capability.md) ｜ 票 [T1094](../tickets/T1094-deny-by-capability.md)/[T1095](../tickets/T1095-deny-by-capability-verify.md) ｜ impl645 续（audit 深化）
- 借鉴：—（700 audit 维度扩展）

## 勘察（排重）

- 700 audit 只有 denyByModel——「vision 拒绝多还是 tools 拒绝多」（补声明 vs 换模型的决策依据）无维度。

## 决定

Report 增 `denyByCapability`（vision/tools→次数）——snapshot 时从 recent 环即时聚合。容量规划双视角：denyByModel（谁被拒）×denyByCapability（什么能力缺失）。

## 测试
跨模型同能力聚合精确/与 denyByModel 并存。
