# impl-79 — 熔断冷却自适应退避

**What to build:** provider 持续故障时熔断探测节奏指数放缓（冷却 ×2 递增封顶 backoff-cap），
探测成功即复位；跳闸事件与指标透出退避档位；首跳行为与旧版完全一致。

**Blocked by:** None — can start immediately（T104 已闭合）

**Status:** done

- [x] `Circuit` 配置增 `backoffCap`（默认 8，fail-fast）+ `backoffMultiplier(trips)`；6 参便捷构造保留
- [x] `ModelCircuitBreaker`：consecutiveTrips/effectiveCooldownMs 贯穿 admit/占位/逃生；事件 payload 增
      `consecutiveTrips`/`openDurationMs`；gauge `buzhou.resilience.circuit-backoff-multiplier`
- [x] `ResilienceStats`：`circuitBackoff` 快照（details + updateCircuitBackoff）
- [x] 测试：翻倍/复位/cap 封顶三新用例 + 既有 12 用例回归（81 测试全绿）
- [x] spec 25 新篇

## Done

commit：见 git log（impl-79）。验证：`mvn clean test -pl buzhou-resilience`（81/81 绿）。
