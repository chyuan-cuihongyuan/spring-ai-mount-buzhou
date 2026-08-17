# impl-129 — 配置校验补全

**What to build:** runaway/backpressure 全键 fail-fast；webhook 静默回退改显式拒绝。

**Blocked by:** None

**Status:** done

- [x] BuzhouRunawayProperties 全键校验（步数/时长/比例/重复检测/升级策略）
- [x] BuzhouBackpressureProperties 全键校验（并发/时长/两处策略词封闭）
- [x] BuzhouWebhookProperties 静默回退 → fail-fast（null 默认不变）
- [x] 测试：合法全量通过 + 逐键非法拒绝 + 语义回归——core 315 绿
