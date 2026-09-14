# 906 — core 零覆盖尾巴清扫（R4）

**What to build:** AttachmentRenderer（default maxChars 截断合同）与 CommandBackend.CommandOutcome（success 谓词矩阵）两测试类；判据收紧（zero = miss≥1）后隔离 worktree 复扫确认 core 清零。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] core：AttachmentRendererTest（4 用例）+ CommandBackendTest（5 用例）
- [x] spec 1203 + README 行
- [x] 隔离 worktree 复扫：core 零覆盖（miss≥1）残留恰 1 项 = R1 豁免台账的 SmartLifecycle 匿名类（mis=2）；复核浮出 SnapshotMessage → 归 R5；core 2653 用例全量绿

## Done

验证：两靶点清零；收紧判据下无未入档残留；并发压测 T1815 加固（护栏生效，同环境两跑非确定性挂死不再复现）。commit 见本轮 `test(core)` 提交 + T1815 `fix(core)` 提交（被 N 会话 cc084a62 卷入，路径追认入 map）。
