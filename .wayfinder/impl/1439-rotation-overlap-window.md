# impl 1439 — RotationOverlapWindow 轮换重叠窗（R39 = effort #1838 / spec 1838 / T2877-T2878）

**What**：`RotationOverlapWindow`（core/crypto 静态纯函数）——validity 三态
（CURRENT/GRACE 含边界/EXPIRED）+ 未来代 fail-fast + census 普查
（expiredRatio -1 哨兵）；零宽窗合法。

**Why**：TLS 证书轮换/Vault grace 思想——无重叠窗的轮换使用旧钥的在飞
数据瞬间全废（蓝绿不接）；重叠窗让旧代兼容到窗尽，清扫进度可读。

**Verify**：`RotationOverlapWindowTest` 4 用例全绿。

**Status**：done（2026-09-16）
