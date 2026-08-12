---
id: T1
title: CI 在 GitHub 红（badge=failing）而本地 mvn clean verify 全绿——根因与修复
type: research
status: open
assignee: ""
blocked-by: []
created: 2026-08-13
---

## Question

CI badge 显示 `failing`，但本地 `mvn -B -ntp clean verify`（与 `ci.yml` 同命令）15 模块全绿。**真正的失败步骤与根因是什么？如何让 CI 在干净 runner 上稳定绿？** 这是整张图的入口门禁——core 深化的一切可信度都建立在 CI 绿之上。

## Context（2026-08-13 已核验）

- 本地 clean build 全绿；**非 clean** 的 `mvn verify` 曾报 15 个 `NoSuchMethodError`（`DanglingCallRepairerDedupTest` / `HarnessToolCallingManagerDedupTest` / `CrashRecoveryEndToEndTest`），但那是 `target/test-classes` 里**已删除且从未提交**的旧测试残留 `.class`（引用了已从 main 移除的 `DedupGate` / `RecoveryConfig` / `putIfAbsent`）造成的幽灵错误——非真实缺陷。`mvn clean` 即消，无需改源码。
- **根因已被 T2 research 纠正（2026-08-13）**：`spring-ai 2.0.0`（2026-06-12 GA）与 `spring-boot 4.1.0`（2026-06-10 GA）**均已 GA 且在 Maven Central**。故 pom 无 `<repositories>` **是对的**，加 milestone/snapshot 仓是**错误修复**。CI 失败不是「Central 没有这些制品」，而是**环境性**：最可能是 GitHub Actions `actions/setup-java` 的 `cache: maven` 缓存了 GA 前遗留的**否定解析标记**（`.lastUpdated`，在 update interval 内不重查），或 GA 上线瞬间的瞬时失败被缓存 → 干净 runner 无法重新解析 → CI 红。
- 旁证本地可信：本地 `spring-ai-bom-2.0.0.pom` 时间戳 = 2026-06-12（GA 当天），应为真 GA 制品，故「本地 clean 绿」可信，不是里程碑误报。
- 无法直接取 GitHub Actions 日志（环境无 `gh`、未鉴权）。

## Resolution

<!-- 待领取后填写：取真实 CI 失败日志确认是「依赖解析失败」→ 修复方向（按 T2 结论，非加仓）：清掉 setup-java 的 maven cache / 加 `-U` 强制更新 / 或临时移除 `cache: maven` → push 验证 badge 转绿。勿加 milestone/snapshot 仓。 -->

## Assets

- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面](T2-spring-ai-native-vs-buzhou.md)（已闭合）结论：`2.0.0` 与 `4.1.0` 均为 GA、在 Maven Central → 排除「缺仓」假设，锁定环境性缓存问题。
