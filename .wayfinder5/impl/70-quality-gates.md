# 70 — 覆盖率/SpotBugs 质量门卡线（T95 决策落地）

**What to build:** root pom jacoco check（LINE ≥ 0.70 统一线，phase=verify）+ BOM/starter/examples 豁免 + spotbugs workflow 升 High 硬门。

**Blocked by:** None.

**Status:** done

## Done

验证：`./mvnw -pl buzhou-core clean verify` 显式输出 `jacoco:check (enforce-line-coverage) → All coverage checks have been met`（门在 verify 生效）；基线实测 13 机制模块 77.1%–90.9%。
落地：root pom jacoco 增 `enforce-line-coverage` check 执行（BUNDLE/LINE/COVEREDRATIO ≥ 0.70，`jacoco.check.skip` 属性可控）；BOM/starter/examples 豁免注记；spotbugs.yml 从 `|| true` 观测升 `threshold=High` 硬门（Medium 以下仍归档；排除纪律 = exclude 文件 + issue 注记）。