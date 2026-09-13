# 663 — AIMD 自适应批量 yml 装配

**What to build:** webhookEventForwarder bean 构造点 Binder 直读 buzhou.webhook.adaptive-batch → setter；装配分支测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] autoconfig Binder 直读 + setter 接线
- [x] 装配分支测试（true/缺省）
- [x] spec 910 + README 行（欠账：906–910 五行）

## Done

验证：装配测试 + 既有 forwarder 用例全绿。commit 见本轮 `feat(core)` 提交。
