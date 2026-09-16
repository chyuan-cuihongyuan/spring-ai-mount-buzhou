# Spec 2013 — 启动豁免窗追踪（effort #2013，R14）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3127–T3128，impl 1564）。
> 借鉴：K8s startup probe——慢启动期失败不计 liveness 账，首次成功毕业。

## Problem Statement

慢启动实例（MCP 建连 / 工具冷加载 / 模型首-token 慢）在启动期内的
失败抖动会被故障检测（φ 检测器 / 熔断器）当作真实故障——冷启动被
误判不健康，触发不必要的驱逐；但豁免若无边界，僵尸实例也会借「启动
中」永久免罪。

## Solution

`StartupGraceTracker`（core/concurrent，synchronized 小临界区）：

- `begin(instanceId, now)`：锚定启动（豁免窗自此刻；重复锚定=重启
  重锚，窗口重算）；
- `reportFailure(id, now)` 自动分流：已毕业或窗外 → 计账（counted）；
  未毕业且窗内 → 豁免（exempted，不进故障账）；未锚定一律计账；
- `graduate(id)`：首次成功即毕业（幂等——后续失败全额计账）——
  豁免给冷启动，不给僵尸；
- 读数：`activeGraces(now)`（正在受宽容的实例面）/ `stats()`（豁免
  /计账/毕业三计数）；
- 契约：graceMillis > 0、id 非空、now ≥ 0 fail-fast。

## User Stories

1. 作为故障检测作者，冷启动抖动不进 φ 输入——真故障信号不被启动
   噪声稀释。
2. 作为 SRE，exempted 高企而 graduations 不动=僵尸借豁免——显形可
   治。

## Testing Decisions

- 窗内豁免（含 9999 界内边界）；恰到期计账（>=grace 边界）；毕业
  幂等 + 毕业后窗内计账；未锚定计账；重锚窗口重算；多实例独立；
  畸形五型 fail-fast。

## Out of Scope

- 不接 φ 检测器/熔断器管线（接线归后续轮）。

## Further Notes

- 与 PhiAccrualFailureDetector 配对：φ 收心跳建嫌疑，本件在嫌疑的
  输入端滤掉冷启动噪声。
