# 680 — 软截止预警集成（第 28 轮补记切片）

**What to build:** HarnessToolCallingManager 软截止窗集成（setSoftDeadlineWindow + checkSoftDeadlineWindow 一次性预警 + beginTurn 复位 + softDeadlineWarned 读面）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] setSoftDeadlineWindow / checkSoftDeadlineWindow / softDeadlineWarned
- [x] SoftDeadlineFlagTest 三用例（默认关/复位/值对象窗判定）
- [x] spec 927（本切片为本轮对账时补写的切片文件——原轮次提交时遗漏，代码与测试已在 58bf053b 前的 R28 提交中落地）

## Done

验证：SoftDeadlineFlagTest 全绿。原提交见 R28（本文件为对账轮补写切片留档）。
