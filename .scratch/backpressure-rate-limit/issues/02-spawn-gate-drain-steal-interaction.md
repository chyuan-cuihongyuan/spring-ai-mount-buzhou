# 02 — spawn 闸边界交互: drain 唤醒拒绝 + steal 不占容量

**What to build:** spawn 闸与既有机制的边界语义收口。drain 交互：drain 拒新判定**先于**容量裁决——drain 置位时新 spawn 一律抛 `RuntimeDrainingException`；**已在容量排队中的 spawn 等待者须被唤醒并拒绝**（不睡死在容量信号量上），拒绝语义向 drain 口径对齐。steal 交互：`spawn(steal=true)` 是已活跃会话的接管路径（易主续接），**不占新容量**——实现期确认 steal 绕过容量计数（或先释放原持有再计），勿把接管误判为超限；容量不足不影响合法接管。两路交互各配 e2e 断言（复用 01 的测试骨架 + `GracefulShutdownEndToEndTest` 的 drain 断言形态 + `CrashRecoveryEndToEndTest` 的双 runtime 租约交接形态）。

**Blocked by:** 01（背压地基：两档策略 + spawn 闸 + e2e 骨架）

**Status:** ready-for-agent

- [ ] e2e：drain 置位后新 spawn 抛 `RuntimeDrainingException`（不经容量排队）
- [ ] e2e：容量排队中的 spawn 在 drain 置位时被唤醒拒绝，drain 不被排队请求卡住（drain 正常完成）
- [ ] e2e：容量已满时 `spawn(steal=true)` 接管既有会话成功，不被容量闸拒绝
- [ ] drain 唤醒拒绝与容量拒绝的异常类型/事件语义区分清晰（调用方可按类型分流）
