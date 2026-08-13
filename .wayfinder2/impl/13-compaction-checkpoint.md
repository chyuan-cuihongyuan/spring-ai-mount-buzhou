# 13 — memory · 压缩前检查点与三档回滚

**What to build:** 压缩提交前按水位不可变快照消息窗；压缩事故后可三档回滚（仅消息窗 / 加撤销摘要生效 / 连事实台账）。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] `CompactionCheckpoint`（sessionId、seq、preWatermark、消息窗引用）：compact_now/增量摘要提交前按水位键不可变快照
- [ ] 回滚档①：仅恢复消息窗
- [ ] 回滚档②：恢复消息窗并撤销摘要生效（双时序 valid_to 直接表达）
- [ ] 回滚档③：连同事实台账回滚（默认关）
- [ ] 端到端：压缩后回滚各档→会话/摘要/台账状态正确
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-12；[T40](../tickets/T40-memory-compaction-checkpoint.md)。源：cline 66,136★（检查点与压缩解耦、三档回滚；Buzhou 按压缩事件触发、成本更低）。
