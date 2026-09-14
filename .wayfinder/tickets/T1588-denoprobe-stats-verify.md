---
id: T1588
title: Deno 沙箱探测读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1587
created: 2026-09-15
---

## Question

J 会话第 66 轮：DenoProbeStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DenoProbeStatsTest，桩 SandboxProcessLauncher 骨架——见既有 DenoSandbox 测试）：探测成功 → probes=1 probeSuccesses=1；TTL 内二次 available → probeCacheHits=1（无新探测）；launcher 抛异常 → probeUnavailables=1；双守恒恒等式成立；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='DenoProbeStatsTest'` 绿 + 既有 DenoSandbox 回归绿。
