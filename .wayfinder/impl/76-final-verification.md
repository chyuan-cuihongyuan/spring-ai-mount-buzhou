# 76 — 全仓终验（T101 决策落地）

**What to build:** 全仓 clean verify（含质量门）+ spec/impl 对照核验 + 远端门执行入口记录。

**Blocked by:** impl 57–75（全部已 done）。

**Status:** done

## Done

验证（2026-08-15 终验）：
- **`mvn clean verify` BUILD SUCCESS**——18 模块全部 SUCCESS（含 jaCoCo LINE≥70% 覆盖率硬门，01:26 min）。
- **全仓测试 1021 个**（0 失败 0 错误）；effort #5 新增约 170+ 用例（熔断 17 / 降级链 6 / 预算 5 / 配额 5 / 沙箱合流 8 / MCP 漂移 3 / 结构化 4 / fork 4 / webhook 3+1 / 手动压缩 3 / perf 3 / examples 演示 7 / 正规化与门禁若干）。
- **对照核验**：22 张决策票 21 张 closed（T102 本轮后闭合）；impl 56–75 逐票 done 且带 commit 号与验证方式。
- **远端门入口**（本机不重放，workflow 承载）：SpotBugs High 硬门（weekly spotbugs.yml）/ redteam 双硬门（nightly redteam-nightly.yml + metrics.mjs）/ SBOM+依赖扫描（weekly supply-chain.yml）/ perf 哨兵（weekly perf-nightly.yml）。
- 覆盖率实测区间 77.1%–90.9%（13 机制模块），硬门 70% 全部满足。