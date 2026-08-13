---
id: T10
title: 取 CI 失败日志（或 Linux 复现）→ 修 OS 相关构建/测试缺陷 → 让 badge 转绿
type: task
status: open
assignee: ""
blocked-by: []
created: 2026-08-13
---

## Question

[T1](T1-ci-red-remotely-green-locally.md)（已 closed，research）确证 CI 红是**确定性 OS/runner 相关构建/测试缺陷**（非依赖、非缓存）。本 ticket 是其**执行尾**：拿到失败原文、定位并修复具体缺陷、让 GitHub CI badge 在干净 runner 上稳定转绿。

具体要做的决定/动作：

- 用哪种方式取到失败原文——`gh run view --log-failed` / Actions UI 下载日志 / 还是在 Linux（WSL 装发行版 / Docker / 真 Linux 机）本地复现？
- 日志点名的具体模块/测试/编译错误是哪一条？根因落在哪类 OS gap（路径大小写、行尾、charset/locale、`/bin/sh` 进程依赖、端口/网络/资源、OOM）？
- 修法选型（尽量最小、不引入平台分支）：`File.separator`/`Path`、修资源路径大小写、显式 `StandardCharsets.UTF_8`、不依赖默认 locale/TZ、去硬编码 `/bin/sh`（或按平台分流）、给需网络的测试加 CI `assume`/skip。
- 是否顺带升 `actions/checkout@v4→v5`、`setup-java@v4→v5`（消 Node20 / setup-java 弃用警告，cosmetic 非根因）。

## Context

- **HITL/环境**：本环境没有所需能力——CI 日志原文（`gh` 未鉴权 / job-logs API 返回 403）、Linux 复现（本机 WSL 无发行版、无 Docker）。故交拥有环境者执行。
- 入口 run：[31622806373](https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou/actions/runs/31622806373)（sha `cbbca3e2`，`Build & test` step exit 1，8 连红集中于 2026-08-12）。
- 嫌疑排查（来自 [T1](T1-ci-red-remotely-green-locally.md)）：显眼的 `/bin/sh` 硬编码、CRLF、JDK8 默认**均已排除为 Linux-CI 根因**（Windows 本地假红；`/bin/sh` 在 Linux 存在、blob 全 LF、CI 用 JDK21）；真正 Linux/JDK21 特有的失败身份未知，**首步=取日志看 Maven 报错**。候选方向：只在 Linux 触发的测试（locale/TZ/charset、端口/网络/外部依赖、资源路径大小写）、OOM、JDK21 vs 17/23 差异。
- 完成后：badge 绿 = 解锁对外「CI 绿」信誉项，并为 [T4](T4-runnable-main-demo.md) / [T5](T5-real-llm-integration-test.md) 的 CI 验证扫清环境（注意：二者依赖前提——「依赖可解析」——已被 T1 确证满足，本 ticket 只影响「CI 上验证」，不阻塞其形态决策）。

## Resolution

<!-- 完成后填写：取日志方式 + 失败原文要点 + 修复的具体缺陷与 commit + push 后 badge 状态 -->
