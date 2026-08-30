# impl-91 — 黄金轨迹扩充 A

**What to build:** evidence 引用生命周期 / outbox 跨重启 / 压缩事件三条黄金轨迹。

**Blocked by:** T115（压缩事件面）— 已闭合

**Status:** done

- [x] G7：fork 登记→源删保留→最后引用者物理删→EVIDENCE_GONE
- [x] G8：两代 forwarder 共享 store 的重启补投递 + 零死信
- [x] G9：大历史折叠→memory.compacted 观测事件（计数/回收为正）
- [x] examples 3/3 绿；spec 34 §B

## Done

commit：见 git log（impl-91）。
