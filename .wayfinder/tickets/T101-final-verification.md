---
Type: task
Status: closed
assignee: zcode
blocked-by: T82,T83,T84,T85,T86,T87,T88,T89,T90,T91,T92,T93,T94,T95,T96,T97,T98,T99,T100
---
## Question

全仓终验：mvn clean verify 全绿、覆盖率/SpotBugs 质量门通过、redteam nightly 基线刷新、SBOM 落档、spec/impl 对照表核验（每张票 Done 有 commit 与验证方式）、README 徽章与项目状态更新。产出 impl 76。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **验证矩阵**：全仓 `mvn clean verify`（含 LINE≥70% 覆盖率硬门 + 质量门）为本地金标准；SpotBugs High / redteam 硬门 / SBOM / perf 哨兵为 nightly/weekly workflow 侧门（本机不重放远端行为，记录执行入口）。
2. **spec/impl 对照**：22 张决策票（T81–T102）全部 closed 且 impl 56–77 逐票 Done（commit 号 + 验证方式）——终验时逐一核对。
3. **README 更新**归 Cycle 22（与知识库同步、MAP 闭合一起收口）；本轮只核验现状不重复改。
