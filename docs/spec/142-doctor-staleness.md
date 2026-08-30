# Spec 142 — 体检陈旧度（effort #116）

> wayfinder map：`.wayfinder116/MAP.md`（T467–T468）。借鉴：Consul TTL
> health check（健康自报必须周期续约，静默即降级）。

## Problem Statement

ConfigDoctorHealth 缓存就绪时刻的体检报告后恒 UP——运行几小时后配置世界已变
（热更新/环境变量/新拼错键），健康面还在展示旧世界的「干净」，冒充现在。

## Solution

`ConfigDoctorHealth(env, freshnessTtl)` 可选 TTL：报告超过 TTL 未刷新转
UNKNOWN（details stale=true——旧快照是旧世界的，不冒充现在）；`reexamine()`
手动刷新面恒可用（运维 cron/端点驱动——不自装调度纪律）。默认无 TTL 构造 =
既有行为零变化。details 增 examinedAt（ISO 时刻）/freshnessTtlMs/stale 三键。

## User Stories

1. 作为 SRE，健康面的 UP 只在体检新鲜时成立，所以旧报告不冒充「现在干净」，
   stale 状态提示我续约或排查续约管线。
2. 作为宿主开发者，默认零变化 + 手动 reexamine，所以既有部署无感、测试可
   确定性驱动。

## Testing Decisions

- 红队：无 TTL 永不陈旧（零变化）；TTL 过期转 UNKNOWN(stale) + reexamine
  恢复；非正 TTL fail-fast。既有 unknown→UP 两例回归 + 启动校验回归。

## Out of Scope

- 其余健康段 TTL 化（同款复制轮）；autoconfig yml 键；自动重体检。

## Further Notes

- 健康段三态化先例：pending（未体检）/ stale（过期）/ UP（新鲜）。
