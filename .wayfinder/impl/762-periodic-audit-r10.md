# 762 — J 系周期预检（R10）

**What to build:** 隔离 worktree 全仓 verify + 双门复跑 + 三处主仓破损收口（guard 保序 / 910–915 README 行 / SessionExportDiff 快照行）+ 台账对账。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 全仓 `clean verify`：16 模块 SUCCESS（guard 修复后全链路复跑）
- [x] guard ToolDenialLog topDenials 保序收口（修复归属 K 在制，本轮代收）
- [x] 910–915 README 行补全 + SessionExportDiff 快照行
- [x] 双门 SpecCoverage/ApiSurfaceSnapshot 绿（隔离树验证）
- [x] 台账对账无缺位 + spec 1009 入档

## Done

验证：隔离 worktree 全仓 `mvn -B -ntp clean verify` 16 模块 SUCCESS；双门测试绿。commit 见本轮 `fix` 提交。
