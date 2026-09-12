---
id: T864
title: 黄金轨迹 payload 归一化的口径裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

黄金轨迹断言器（spec 32）只有类型序/计数/payload 谓词——payload 值级断言会撞 UUID/时刻/时长等运行期易变值而 flaky。归一化口径怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 8 轮 = effort #600 / spec 607 / impl 460）：

1. EventSequenceAssert 增 `assertPayloadNormalized(type, expected)` + 公开静态 `normalizePayload/normalizeValue`（test-jar 面，全模块黄金轨迹可复用）。
2. 哨兵口径：UUID→`<uuid>`、ISO-8601 时刻→`<instant>`、ISO 时长→`<duration>`、13 位数字→`<epochMs>`、10 位数字→`<epochSec>`；Map/List 深层递归、键保序；其余值原样。
3. epoch 量级判定有假阳性可能（13 位 microUsd = 千万美元级账目）——测试面可接受，入档诚实边界。
4. 既有谓词断言不动（零行为变化）。
