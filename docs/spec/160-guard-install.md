# Spec 160 — 基数守卫装配（effort #124）

> wayfinder map：`.wayfinder124/MAP.md`（T513–T514）。spec 132 fog「默认装配
> 接入（opt-in 键）」收口。

## Problem Statement

TagCardinalityGuard 只有编程面 wrap——全局 metrics 装配面（Holder 安装处）
没有开关，yml 用户开不了守卫。

## Solution

`buzhou.metrics.cardinality-guard.enabled`（默认 false 零变化）：开启时
`buzhouMetricsHolderInstaller` 在安装前以 TagCardinalityGuard 装饰
MicrometerBuzhouMetrics——全局所有 buzhou 指标路径经守卫。metadata 手维护
补键 + 矩阵 env 直读登记。

## User Stories

1. 作为运维，一行配置开启全局 tag 基数守卫，所以越界 tag 值折入 __overflow__
   不打爆时序库。
2. 作为既有用户，默认关零变化——升级无感。

## Testing Decisions

- 装配路径红队：ApplicationContextRunner 走真实 BuzhouMetricsConfiguration，
  断言 Holder 安装类型（关 = 裸 Micrometer / 开 = Guard 装饰）+ @AfterEach
  reset。矩阵两测全绿。

## Out of Scope

- folds() 告警面；差异化封顶；热开关。

## Further Notes

- 安装点唯一性是本接线的前提（spec 132 全局旋钮 + 单点安装）。
