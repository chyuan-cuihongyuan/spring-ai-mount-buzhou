# effort #730 — 限流头跨供应商归一解析（719 扩散）

- 会话：G 会话 700 系第 31 轮 ｜ spec [730](../../../docs/spec/730-ratelimit-header-normalization.md) ｜ 票 [T1060](../tickets/T1060-rl-header-normalization.md)/[T1061](../tickets/T1061-rl-header-normalization-verify.md) ｜ impl630
- 借鉴：—（719 扩散）

## 勘察（排重）

- 719 parse 仅 OpenAI 头名——Anthropic（anthropic-ratelimit-requests/tokens-*）宿主须自行映射；归一解析零命中。

## 决定

`parseFlexible(HttpHeaders)`：先 OpenAI 头名（非 empty 即用——不混合来源），缺项回退 Anthropic 头名（requests/tokens remaining+limit+tokens-reset）。

## 测试

Anthropic 头解析+0.8 MEDIUM/OpenAI 优先不混合。

## 诚实边界
两家覆盖（其他供应商后续按需）；reset 头 Anthropic 仅 tokens 维度（requests-reset 不解析——保守）。
