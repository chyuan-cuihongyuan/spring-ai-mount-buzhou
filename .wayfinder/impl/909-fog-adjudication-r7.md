# 909 — R7：T1819 治本 + 两雾区裁决

**What to build:** WebhookDeadReplayAuditTest forwarder 收口（T1819 治本）；BRANCH 维度读面化裁决与 report-aggregate 不引入裁决入档。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] WebhookDeadReplayAuditTest：forwarders 登记列表 + @AfterEach close（T1819 治本）
- [x] 泄漏源+受害者同群组定向验证全绿
- [x] BRANCH 13 模块实测分布入台账；report-aggregate 不引入入档
- [x] spec 1206 + README 行

## Done

验证：泄漏源+受害者+邻域同群组全绿（9 用例）；BRANCH 13 模块分布与 report-aggregate 不引入裁决入 map/spec；K 线雾区清零。commit 见本轮 `test(core)` 提交。
