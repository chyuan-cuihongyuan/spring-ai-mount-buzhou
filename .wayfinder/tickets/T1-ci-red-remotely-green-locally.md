---
id: T1
title: CI 在 GitHub 红（badge=failing）而本地 mvn clean verify 全绿——根因与修复
type: research
status: closed
assignee: "agent"
blocked-by: []
created: 2026-08-13
resolved: 2026-08-13
---

## Question

CI badge 显示 `failing`，但本地 `mvn -B -ntp clean verify`（与 `ci.yml` 同命令）15 模块全绿。**真正的失败步骤与根因是什么？如何让 CI 在干净 runner 上稳定绿？** 这是整张图的入口门禁——core 深化的一切可信度都建立在 CI 绿之上。

## Context（2026-08-13 已核验）

- 本地 clean build 全绿；**非 clean** 的 `mvn verify` 曾报 15 个 `NoSuchMethodError`（`DanglingCallRepairerDedupTest` / `HarnessToolCallingManagerDedupTest` / `CrashRecoveryEndToEndTest`），但那是 `target/test-classes` 里**已删除且从未提交**的旧测试残留 `.class`（引用了已从 main 移除的 `DedupGate` / `RecoveryConfig` / `putIfAbsent`）造成的幽灵错误——非真实缺陷。`mvn clean` 即消，无需改源码。
- **根因已被 T2 research 纠正（2026-08-13）**：`spring-ai 2.0.0`（2026-06-12 GA）与 `spring-boot 4.1.0`（2026-06-10 GA）**均已 GA 且在 Maven Central**。故 pom 无 `<repositories>` **是对的**，加 milestone/snapshot 仓是**错误修复**。CI 失败不是「Central 没有这些制品」，而是**环境性**：最可能是 GitHub Actions `actions/setup-java` 的 `cache: maven` 缓存了 GA 前遗留的**否定解析标记**（`.lastUpdated`，在 update interval 内不重查），或 GA 上线瞬间的瞬时失败被缓存 → 干净 runner 无法重新解析 → CI 红。
- 旁证本地可信：本地 `spring-ai-bom-2.0.0.pom` 时间戳 = 2026-06-12（GA 当天），应为真 GA 制品，故「本地 clean 绿」可信，不是里程碑误报。
- 无法直接取 GitHub Actions 日志（环境无 `gh`、未鉴权）。

## Resolution

**结论（2026-08-13 research）**：CI 红是**确定性、跨 OS 的构建/测试缺陷**，**不是**「依赖解析失败 / Maven 缓存」——MAP Notes 里「环境性 maven 缓存 `.lastUpdated`」的假设**被推翻**。

**已确证的事实（无需日志即可定）**：
- 仓库已开源（`private:false`），公开 GitHub API 免鉴权可读 run/step 结论。最近 **8 次 CI run 全 failure**，集中于 2026-08-12，含**纯 `docs:` 提交**（`docs: 更新 CLAUDE.md…`、`docs(wayfinder)…`）——零 Java/POM 改动却同样失败。
- 失败步骤恒为 `Build & test`（ci.yml step[4]，`mvn -B verify`），**exit code 1**；`setup-java@v4`（含 `cache: maven`）step[3] **成功**——不是缓存恢复失败，是 build 本身挂。
- annotations 仅 3 条：`checkout@v4`/`setup-java@v4` 仍跑 Node 20（deprecated）、`setup-java v4 deprecated → v5`（deprecated）、`Process completed with exit code 1.`；**无 Maven 报错文本**（Maven 不产 annotation）。

**为何排除「依赖解析/缓存」假设**：
1. [T2](T2-spring-ai-native-vs-buzhou.md) 已证 `spring-ai 2.0.0` / `spring-boot 4.1.0` **GA 且在 Maven Central**；pom 无自定义仓 → 依赖取自 Central，本地能解析全部依赖（无 missing-artifact）。
2. Maven `.lastUpdated` 否定标记有 **24h 重查间隔**：GA 窗口（2026-06 中旬）遗留的标记到 2026-08-12 早已过期 → Maven 会重查并成功解析。故「缓存否定标记」**无法解释持续到 08-12 的红**。
3. 失败对 doc-only 提交也确定性地复现 → 与依赖可用性无关，指向**源码/测试在 ubuntu 上的确定性行为差异**。

**最可能根因（~80% 置信）**：**Linux/JDK21-runner 特定的构建或测试缺陷**——在 Ubuntu 确定性失败。注意：本机 Windows shell 的已知「假红」**全部是 Windows 本地问题、已逐一排除为 Linux-CI 根因**（2026-08-13 核验，见记忆 `windows-host-cant-build-linux-project`）：
- `RunCommandTool` 硬编码 `/bin/sh -c`（`buzhou-tools/.../RunCommandTool.java:92`）→ `/bin/sh` 在 **Linux 存在**、`RunCommandToolTest` 在 CI 能跑；它是 **Windows 本地**跑不了的假红，**非 CI 因**。
- `core.autocrlf=true` + **无 `.gitattributes`** → Windows 工作区被转成 CRLF，打断资源串断言（如 `buzhou-skills` `ClasspathSkillScannerTest`）；但 `git ls-files --eol` 核验 **blob 全 LF（0 个 CRLF/mixed）** → Linux 检出为 LF、不受影响，亦为 Windows 本地假红。
- 默认 `JAVA_HOME=D:\JDK\JDK-8`（java 1.8）→ Windows 本地编不出 JDK16+ 语法；CI 用 temurin 21，无关。

故 Linux-CI 失败的**确切身份仍未知**——所有显眼嫌疑（缓存、`/bin/sh`、CRLF、JDK8）均已排除，剩余可能：某条只在 Linux/JDK21 触发的测试（locale/TZ/charset、端口/网络/外部依赖、资源路径大小写）、runner 资源 OOM、或 JDK21 vs 本地 17/23 的 API 差异。**日志因此成为不可或缺的一块。**

**未能确定（环境受限）**：**具体哪条测试/编译失败**——需 CI 日志原文或 Linux 复现，二者在本环境都不可得（job-logs API 返回 403 需鉴权；本机 WSL 无发行版、无 Docker）。

**可信度结论**：**正确配置下**的本地绿（JDK 17/23 + LF 检出）说明代码本体健全；但本机 Windows shell 的 naive `mvn verify` 不可信（JDK8/CRLF/`/bin/sh` 三类假红）。CI 失败是 Linux/JDK21 特有、独立于这些 Windows 假红——故 local-green ≠ CI-green，确切失败行仍需日志。core 深化（T3+）可基于「代码本体健全」推进，**不必等 badge 转绿**；只有「对外发布绿徽章」这一件等 [T10](T10-fix-ci-os-specific-defect.md)。

**修复路径（决策树，交拥有环境者执行）→ graduate [T10](T10-fix-ci-os-specific-defect.md)**：
1. 取日志：`gh run view 31622806373 --log-failed`（或 Actions UI 下载），或在 Linux 复现。
2. 日志点名具体模块/测试/编译错误 → 按 OS-defect 常规修（`File.separator`/`Path`、修资源路径大小写、显式 `StandardCharsets.UTF_8`、不依赖默认 locale/TZ、避免硬编码 `/bin/sh`、给需网络的测试加 CI skip/assume）。
3. push → badge 转绿验证。
4. 顺手升 `actions/checkout@v4→v5`、`setup-java@v4→v5` 消除 Node20 / setup-java 弃用警告（cosmetic，非根因）。

## Assets

- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面](T2-spring-ai-native-vs-buzhou.md)（已闭合）：`2.0.0` 与 `4.1.0` 均为 GA、在 Maven Central → 排除「缺仓」假设。
- 取证来源（公开 API，免鉴权）：最近 CI run 列表 `actions/workflows/ci.yml/runs`；失败 run [31622806373](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/actions/runs/31622806373)（`Build & test` exit 1）；check-run annotations（仅 exit code + 弃用警告，无 Maven 文本）。
