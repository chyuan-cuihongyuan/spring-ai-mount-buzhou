---
Type: task
Status: closed
---
## Question

`CircuitBreakerStateBackend` SPI（buzhou-resilience.circuit）：recordTrip(model, openedAt,
cooldownMs, consecutiveTrips) / activeTrip(model) → TripMarker?（存活且未过冷却）/
clear(model)。默认 no-op（进程语义）。Redis 实现（buzhou-store-redis）：TTL 键
`<prefix>cb:<模型净化名>`，TTL=cooldownMs（毫秒级 PEXPIRE），值=trips@openedAtEpochMs；
键存活即 OPEN；过期即可探测（无需显式时间比对）。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 57 对应节。
