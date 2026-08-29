# Spec 214 — clearAll + usageAll（effort #151）

> wayfinder map：`.wayfinder151/MAP.md`（T579–T580）。双小方法轮。

## Solution

①`TurnHeartbeat.clearAll()`：全清（部署重启/测试复位语义——表归零，幂等）。
②`VirtualKeys.usageAll()`：与 topUsage 同序的全量用量视图（健康/导出便利面）。
