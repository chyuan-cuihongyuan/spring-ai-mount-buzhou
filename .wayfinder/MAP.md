# Wayfinder Map — Buzhou core 做深做透

## Destination

把 `buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard` 四个核心机制做到**真实鲁棒**：先让 CI 在干净 runner 上真正绿（根因 T1 已定、执行 T10），摸清 Spring AI 2.0 原生边界（T2），再锚定「做深做透」的量化验收基线（T3），随后按基线逐机制深化。用户列出的 1-7 信誉项（措辞降级 / 可运行 demo / 真实 LLM 集成测试 / Spring AI 边界文档 / run_command 安全默认 / `.scratch` 卫生）作为**深化过程的副产品**收口，不另起 effort。目的地是「core 先做深」，不是「打满九机制」。

## Notes

- **领域**：Spring AI 2.0.0 之上的 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见仓库 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **KB 门禁**：下钻业务源码前先按仓库根 `AGENTS.md` 的 KB 路由（`.Knowledge/manifest-routing.json`）。
- **每会话**：先读本 MAP → 从 frontier 取一张 ticket → resolve 后回写「Decisions so far」。
- **tracker 约定**：见 `.wayfinder/README.md`。
- **已知事实（2026-08-13 核验）**：
  - CI badge = `failing`，但本地 `mvn -B -ntp clean verify`（与 `ci.yml` 同命令）**15 模块全绿**。
  - 本地非 clean 的 `mvn verify` 曾报 15 个 `NoSuchMethodError`——那是 `target/test-classes` 里**已删除且从未提交**的旧测试（`*DedupTest` / `CrashRecovery*`）残留 `.class` 造成的幽灵错误，非真实缺陷，`mvn clean` 即消。
  - **CI 根因已被 T1 research 二次纠正**：`spring-ai 2.0.0` / `spring-boot 4.1.0` **均为 GA、在 Maven Central**，pom 无 `<repositories>` 是对的；本地能解析全部依赖（取自 Central）。先前「`.lastUpdated` 缓存否定标记」假设**已被推翻**（24h 重查会自愈、doc-only 提交同样失败、`setup-java` 缓存步骤成功）——真实根因是**确定性 Linux/JDK21 构建测试缺陷**（显眼嫌疑 `/bin/sh`/CRLF/JDK8 均已排除为 Windows 本地假红、blob 全 LF；Linux 特有失败身份未知、需日志）。具体哪条失败需 CI 日志 / Linux 复现，见 [T10](tickets/T10-fix-ci-os-specific-defect.md)（HITL/环境）。正确配置下本地绿可信，core 深化（T3+）不必等 badge 绿。
  - GitHub Actions 取证：仓库已开源，公开 API 免鉴权可读 run/step 结论与 annotation；**完整日志文本**需鉴权（`gh` 未登录、job-logs API 403），Linux 复现亦不可得（本机 WSL 无发行版、无 Docker）——故 T1 已取到「哪步失败」但未取到「Maven 报错原文」，执行尾交 [T10](tickets/T10-fix-ci-os-specific-defect.md)。

## Decisions so far

- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面](tickets/T2-spring-ai-native-vs-buzhou.md) — `spring-ai 2.0.0` / `spring-boot 4.1.0` **均为 GA、在 Maven Central**（→ [T1](tickets/T1-ci-red-remotely-green-locally.md) 进一步**推翻缓存假设**、定为 OS 缺陷）；逐机制 NATIVE/ADDS/REPLACES 表成为 [T3](tickets/T3-depth-definition-of-done.md) 与边界文档 [T9](tickets/T9-spring-ai-boundary-doc.md) 的事实骨架；**MCP 热插拔=NATIVE** 须诚实标注。
- [.scratch/ 移出 git 跟踪 + 加入 .gitignore](tickets/T7-remove-scratch-from-git.md) — 59 个内部草稿 `.md` 经六类敏感扫描**无任何命中** → **仅 untrack**（`git rm -r --cached` + `.gitignore`），保留历史、不做 `filter-repo` 重写；`CLAUDE.md`/`docs/agents/issue-tracker.md` 的引用为路径模板、移出后仍成立；实现见 [impl/02](impl/02-untrack-scratch.md)。
- [README "生产就绪"措辞降级](tickets/T8-downgrade-production-wording.md) — README 4 处「生产」措辞（L3 英 / L5 中 intro、L17 为什么需要、L25 能力段）降级为「**面向生产场景设计的实验性框架**」、中英同步、L25 显式锚定「项目状态：alpha」；`CONTEXT.md`/`docs/spec/00-overview.md` 的「跑在生产里」为术语定义/设计意图、无 alpha 矛盾、有意不动；实现见 [impl/03](impl/03-readme-wording-downgrade.md)。
- [撰写「与 Spring AI 原生能力边界」文档](tickets/T9-spring-ai-boundary-doc.md) — `docs/spec/10-spring-ai-boundary.md`（中英双语）落位 docs/spec 第 10 篇、README 加链接；T2 九机制全覆 + 置信标注，REPLACES（Spill/并行工具/Skill）/ ADDS（记忆/可观测/Hook/持久化/原子工具）/ **NATIVE（MCP 热插拔 诚实标注非差异化）**；实现见 [impl/04](impl/04-spring-ai-boundary-doc.md)。
- [run_command 默认关闭 vs 沙箱](tickets/T6-run-command-safety-default.md) — **默认关**（`ToolsModule.Builder` 既有默认，已由 `ToolsModuleTest` 守护）；沙箱方案=否（黑名单+FileSandbox+超时+HITL 多层已足、跨平台沙箱成本不值）；opt-in 经 `enabledDangerousToolNames()` 挂 HITL；`/bin/sh` POSIX 约束写入 `RunCommandTool` javadoc；implementer 确认既有安全默认（用户未应答 grilling、可推翻）；实现见 [impl/07](impl/07-run-command-safe-default.md)。
- [CI 在 GitHub 红而本地绿的根因](tickets/T1-ci-red-remotely-green-locally.md) — 公开 API 取证：最近 **8 连红**（含 doc-only 提交）、恒挂在 ci.yml `Build & test`（`mvn -B verify`）exit 1、`setup-java` 缓存步骤成功；**推翻「`.lastUpdated` 缓存」假设**（24h 自愈 + T2 证依赖 GA 在 Central + 本地解析成功），定为**确定性 Linux/JDK21 构建测试缺陷**（`/bin/sh`/CRLF/JDK8 均已排除为 Windows 本地假红、blob 全 LF；Linux 特有失败身份未知）；具体失败行需日志 / Linux 复现 → graduate [T10](tickets/T10-fix-ci-os-specific-defect.md) 执行；正确配置下本地绿可信、T3+ 不必等 badge。

## Not yet specified

- core / memory / spill / guard 各自的「深度」ticket（属性测试 / 故障注入 / 预算压力下压缩正确性 / read_range 字节·jsonpath·分页正确性 / HITL→state→attachment 闭环 / 内部 SPI 契约稳定）——等 [T3 验收基线](tickets/T3-depth-definition-of-done.md) 定后逐项 graduate。
- ~~item 6「Spring AI 边界文档」~~ → 已 graduate 为 [T9](tickets/T9-spring-ai-boundary-doc.md)（基于 T2 表）。
- T2 已揭示 MCP 热插拔 = NATIVE；该结论已注入 T9，MCP 本身维持 out-of-scope，无需再范围 ticket。
- 跨 OS 测试健壮性（`/bin/sh` 硬编码、路径大小写、charset/locale 依赖）——[T10](tickets/T10-fix-ci-os-specific-defect.md) 先修已知缺陷；「做深」是否把「干净 Linux/CI 上稳定绿」纳入每模块 DoD，等 [T3 验收基线](tickets/T3-depth-definition-of-done.md) 定。
- crash-recovery / 并行工具调用 / 动态预算 的正确性深挖范围，部分依赖 T1 结果与 T3 基线。

## Out of scope

- `buzhou-observe-otel` / `buzhou-observe-dashboard` / `buzhou-mcp` / `buzhou-skills` / `buzhou-tools`（除 run_command 安全）的「做深」——本次目的地只含 core+memory+spill+guard，其余维持现状。
- 发布到 Maven Central（已有 `RELEASING.md` 流程，属后续独立 effort）。
- 除「一个可运行 src/main demo + 一个真实行为集成测试」外的 `examples/` 扩展。
- 从 git 历史抹除 `.scratch`（除非 T7 发现含敏感信息；默认仅 untrack）。

## Tickets

开放 ticket 在 `tickets/`；frontier = `status:open` + `assignee:""` + 无未闭合 `blocked-by`。索引（含依赖）：

- [CI 在 GitHub 红而本地绿的根因与修复](tickets/T1-ci-red-remotely-green-locally.md) — `research` · ✅ **closed**（根因=确定性 OS 缺陷，推翻缓存假设；执行尾见 T10）
- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面（含 2.0.0/4.1.0 发布状态）](tickets/T2-spring-ai-native-vs-buzhou.md) — `research` · ✅ **closed**
- [core/memory/spill/guard "做深做透"的验收基线](tickets/T3-depth-definition-of-done.md) — `grilling` · **frontier**（T2 已闭合，解锁）
- [可运行 src/main demo 的形态](tickets/T4-runnable-main-demo.md) — `prototype` · **frontier**（T1 已闭合、依赖前提满足；CI 绿由 T10 单独追踪、不阻塞形态决策）
- [真实 LLM 集成测试策略](tickets/T5-real-llm-integration-test.md) — `prototype` · **frontier**（同 T4）
- [run_command 默认关闭 vs 沙箱执行](tickets/T6-run-command-safety-default.md) — `grilling` · ✅ **closed**
- [.scratch 移出 git 历史 + 加 .gitignore](tickets/T7-remove-scratch-from-git.md) — `task` · ✅ **closed**
- [README "生产就绪"措辞降级，正文与 alpha 对齐](tickets/T8-downgrade-production-wording.md) — `task` · ✅ **closed**
- [撰写「与 Spring AI 原生能力边界」文档（item 6）](tickets/T9-spring-ai-boundary-doc.md) — `task` · ✅ **closed**（T2 已闭合，解锁）
- [取 CI 失败日志/ Linux 复现 → 修 OS 缺陷 → badge 转绿](tickets/T10-fix-ci-os-specific-defect.md) — `task` · **frontier**（HITL/环境）

**Frontier（本会话后可领取）**：T3、T4、T5、T10。（T1、T2、T6、T7、T8、T9 已闭合）
