---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

质量门如何卡线？现状：jaCoCo 观测不设线、SpotBugs 观测（|| true）。决策点：覆盖率卡线策略（模块级 line coverage 低线起步：core≥70% 等，BUNDLE 行覆盖 check 指标，测试模块豁免）、SpotBugs 升硬门（排除清单如何管理：spotbugs-exclude.xml 渐进清偿）、卡线后失败的处理流程（修 or 白名单注记）。产出 spec 21/22 增量 + impl 70。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **覆盖率卡线**：root pom jacoco 增 `check` 执行（BUNDLE / LINE / COVEREDRATIO ≥ **0.70 统一线**）——2026-08-15 实测 13 机制模块 77.1%–90.9%，最紧模块留 7pp 余量；不做分模块差线（维护成本 > 收益，统一线升档按整体水位推进）。豁免：BOM（无代码）/ starter（纯聚合）/ examples（演示模块）经 `jacoco.check.skip=true`。
2. **SpotBugs 升硬门**：nightly `threshold=High` 只卡高危（首条 High 即失败）；Medium 及以下仍归档观测。排除清单纪律：如需排除必须 `spotbugs-exclude.xml` + issue 注记，禁止无解释豁免（当前无排除清单，首个 High 出现时建立）。
3. **失败处理流程**：覆盖率越线 = 补测试或（构造性无法覆盖时）在该模块 pom 局部降线 + 注记原因；SpotBugs High = 修复或带注记排除。两者都不许「调阈值了事」。
4. **卡线绑定 phase=verify**：本地与 CI 同门槛（无 CI-only 门）。
