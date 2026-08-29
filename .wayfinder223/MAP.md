# Wayfinder Map — Buzhou 降级链单窗视图（effort #223，B 会话第 46 轮）

> B 会话第 46 轮。链（15）+驱逐（149）+演练（195）三处状态各看各的——
> 「备胎全景一页纸」缺位。借鉴 Grafana 单窗 dashboard 思想：三源合成一视图。

## Destination

FallbackChainView（resilience/fallback 纯组合）：report(chain, ejection, drill,
maxAge) → per-model 行（inChain/ejected/fresh/verifiedAt）+ 建议序（链序内
健康+新鲜优先）。零状态纯函数。

## Notes

- 号段：B=奇数 spec（本轮 207）；轮次 .wayfinder200+。
- 只读组合不改三源；「建议序」= 链序过滤（驱逐剔除 + 过期未验标注不剔除——
  标注与剔除分明）。

## Decisions so far

- 行含三源字段原始值——面板自己决定呈现。

## Not yet specified

- 视图 JSON 导出；健康端点接线。

## Out of scope

- 沿用各轮；自动重排链。

## Tickets

- [x] [T581 FallbackChainView 三源合成](tickets/T581-chain-view.md)（impl-318）
- [x] [T582 视图回归（三源字段/剔除/标注/空链）](tickets/T582-chain-view-tests.md)（impl-318）
