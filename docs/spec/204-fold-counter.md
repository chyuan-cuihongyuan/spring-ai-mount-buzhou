# Spec 204 — 守卫折入指标（effort #146）

> wayfinder map：`.wayfinder146/MAP.md`（T568–T569）。

## Solution

TagCardinalityGuard 折入路径统一走 `foldCounted()`：本地 `folds()` 之外同步
计 `buzhou.metrics.tag-overflow` counter（无 tag——守卫的越界面不能自己变成
无界面）。守卫失守从「查得到」变「告警得到」。

## Testing Decisions

- 既有 7 例回归（fold 语义不变）。
