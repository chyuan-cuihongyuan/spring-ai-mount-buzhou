# impl-130 — 停机排空补全

**What to build:** SleepTimeScheduler 优雅 close（有界排空→硬截断）；webhook close 排空预算可配 + 语义钉住。

**Blocked by:** None

**Status:** done

- [x] SleepTimeScheduler.close() 优雅化 + closeGrace 构造参
- [x] BuzhouWebhookProperties.closeDrainTimeout（兼容构造 + @ConstructorBinding）+ forwarder 生效预算
- [x] 测试：webhook 在途等待+到期排空（挂起收件方确定性）；scheduler 预算内完成/预算外硬截断——core 316 / memory 92 绿
