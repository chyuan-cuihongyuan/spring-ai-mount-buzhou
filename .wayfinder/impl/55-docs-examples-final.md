# 55 — 文档口径统一与终验

**What to build:** 文档与实际一致（模块口径/配置表/索引/兼容矩阵）；examples 有 starter 启动工程；全仓 verify 终验绿；spec 14/15 与实现对齐；MAP 收口。

**Blocked by:** 44,45,46,47,48,49,50,51,52,53,54

**Status:** done

- [ ] CLAUDE.md 模块口径统一；README 配置表补新键（impl41/42/43+本轮）；索引补 spec 11–15；兼容矩阵落 README
- [ ] examples/starter-boot-demo：starter+application.yml+Stub 模型可启动工程（README 步骤化）
- [ ] 全仓 mvn verify 绿（含新模块新测试）
- [ ] spec 14/15 与实现一致性复核；MAP Decisions/Not-yet/Out-of-scope 收口


## Done

commit: 见 git log（impl/55）。验证：全仓 mvn clean verify 绿（含 StarterBootDemoTest 全上下文冒烟）。
落地：README 配置表补 21 个新键（resilience/runaway/backpressure/tool-timeout/leak/lifecycle/retention/in-memory/dashboard 安全/otel 模式/mcp）+ 文档索引补 spec 11-15 + 兼容矩阵落位（兑现 RELEASING 承诺）；spec 15-model-resilience.md（分支三篇机制详设修订并入）；examples/StarterBootDemo（starter+application.yml 可启动 Boot 工程，零 API key，spring.ai.model.*=none 防自动装配抢跑）+ 全上下文冒烟测试；CLAUDE.md 17 模块口径在 resilience 合入后已自洽（无需改）。
