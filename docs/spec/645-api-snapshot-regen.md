# 645 — API 快照再生收口（E 会话合并后）

> 来源：F 会话第 46 轮 = effort #600 / [T940](../../.wayfinder/tickets/T940-snapshot-regen-shape.md) / [T941](../../.wayfinder/tickets/T941-snapshot-regen-verify.md) / impl 498。

## 背景

E 会话 PR #19（500 系 45+ 提交）合并后快照未再生（其分支自留「快照待 regenerate 复验」旁注）；叠加 F 会话 41–43 轮三个新公共类——`ApiSurfaceSnapshotTest` 在 reactor/CI 口径下红。

## 目标

- 显式 `regenerateSnapshot`（`-Dbuzhou.api-snapshot.regenerate=true`——spec 615 硬化门合规触发，不随常规套件自愈）。
- `docs/api-surface.md` 同步：RollingJsonlWriter（core 主段）、ResponseCacheCoalescer（resilience 段）；SessionForkKeys 已随第 41 轮入档。
- 全仓 `mvn verify` 周期性复验（16 模块 + JaCoCo ≥ 70% + enforcer 收敛）。

## 非目标

不改扫描口径/门语义（spec 615 已硬化）。

## 测试

快照 diff 恰 3 行（无意外公共面泄漏）；reactor 口径门绿；全仓 verify 绿。

## 兼容性

纯再生成与文档同步，零代码行为变化。
