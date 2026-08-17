# impl-80 — fork 证据引用计数生命周期

**What to build:** fork 存续期间源证据不被删除（引用保留）；fork 关闭即最后引用者关闭→
物理删除；TTL/孤儿扫描引用门控；悬垂读结构化 EVIDENCE_GONE；账本跨重启持久。

**Blocked by:** None（T105 已闭合）

**Status:** done

- [x] core：`SessionForkListener` + `RuntimeConfig` 第 11 槽 forkListeners（10 参构造保留）+ fork 复制后回调（异常吞掉）
- [x] spill：`EvidenceRefLedger`（.evidence-refs.json 原子持久）+ DiskSpillStore 六路径引用感知
      （store/delete/deleteBySession/deleteExpired/sweepOrphans/readRange）+ `acquireSessionReferences`
- [x] SpillModule.configure() 贡献 fork 监听器
- [x] 测试：spill 五用例（保留/级联回归/TTL 门控/孤儿门控/账本持久）+ core 监听器 e2e
- [x] spec 26 新篇

## Done

commit：见 git log（impl-80）。验证：core+spill 全绿（webhook 计数竞态断言顺带修正）。
