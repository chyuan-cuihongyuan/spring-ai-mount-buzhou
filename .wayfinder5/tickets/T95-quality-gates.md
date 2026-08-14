---
Type: task
Status: open
blocked-by:
---
## Question

质量门如何卡线？现状：jaCoCo 观测不设线、SpotBugs 观测（|| true）。决策点：覆盖率卡线策略（模块级 line coverage 低线起步：core≥70% 等，BUNDLE 行覆盖 check 指标，测试模块豁免）、SpotBugs 升硬门（排除清单如何管理：spotbugs-exclude.xml 渐进清偿）、卡线后失败的处理流程（修 or 白名单注记）。产出 spec 21/22 增量 + impl 70。
