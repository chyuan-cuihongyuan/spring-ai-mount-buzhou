# Spec 192 — 成本健康段装配（effort #140）

> wayfinder map：`.wayfinder/maps/effort-140.md`（T554–T555）。spec 190 fog 收口。

## Solution

`buzhouModelCostHealth` bean（@ConditionalOnMissingBean）：台账全局恒在 +
预算钩子自动入账 → 恒有段（空台账诚实空态）；宿主可自备 bean 覆盖。

## Testing Decisions

- 启动校验回归 + 健康面单测。
