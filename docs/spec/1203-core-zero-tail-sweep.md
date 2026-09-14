# 1203 — core 零覆盖尾巴清扫与判据收紧（R4）

> 来源：K 会话第 4 轮 = effort #1203（[T1811](../../.wayfinder/tickets/T1811-core-zero-tail-sweep-shape.md) / [T1812](../../.wayfinder/tickets/T1812-core-zero-tail-sweep-verify.md) / impl 906）。方法论：coverage-guided test completion 收官档——**判据收紧**（zero 判据 miss≥5 → miss≥1）后复扫，尾巴逐类清零或豁免入档（R1 纪律延续）。

## Problem Statement

隔离 worktree 复扫（core 820 类）显示：低覆盖档（<50% 且 miss≥10）已清空，但零覆盖判据收紧到 miss≥1 后浮出两个小类——`AttachmentRenderer`（mis=6，接口 default 截断方法从未执行）与 `CommandBackend.CommandOutcome`（mis=4，`success()` 是 run_command 的成功判定谓词）。R1 的 miss≥5 门槛正是为过滤「无行为小类」设的，但这两个类证明小类也可能行为敏感——门槛该收紧，尾巴该收清。

## 目标

- **AttachmentRendererTest**（default 方法合同，java.util 接口 default 体测试思想——实现方覆写与否都需基线断言）：
  - 空文本 passthrough（empty 进 empty 出，截断逻辑不碰 empty）；
  - `maxChars<=0` 表示不限制（整文返回）；
  - 限内（length ≤ maxChars）passthrough 原文；
  - 超限截断：返回前 maxChars 字符（纯文本截断基线——覆写方按事实粒度截断并附 key 指针是增强非替代）。
- **CommandBackendTest**（success() 谓词矩阵）：
  - exitCode=0 && !timedOut → success；
  - exitCode≠0 → 非 success（即使 !timedOut）；
  - timedOut → 非 success（即使 exitCode=0——超时即失败优先级高于退出码）；
  - truncated 不参与 success 判定（截断是输出量控制非失败语义）+ record 分量透传。

## 实现决策

- 纯测试增量：两个测试类与被测类型同包（spi / exec），主代码零变化。
- AttachmentRenderer 是 @FunctionalInterface → lambda stub（CompositeAttachmentRendererTest 先例的 returning 工厂同型）；CommandOutcome 是 record → 直接构造。
- **证据基建修正**：core 复扫改走隔离 worktree（`.scratch/k-wt`，本仓 e84940f6 记载的既定解法）——主工作区复扫已被并行会话构建竞争卡死一次（surefire XML 停滞 50 分钟），后续 core 证据一律隔离产出。

## 测试决策

- 好测试标准：断言 default 体/谓词的输入→输出合同，不测接口被谁调用；每个断言一条可陈述语义（如「超时即失败优先级高于退出码」）。
- seam：AttachmentRenderer 走 `render(sessionId, turn, maxChars)` 三参入口单点；CommandOutcome 走 `success()` 单点。
- 先例：CompositeAttachmentRendererTest（lambda stub + Optional 断言）。
- 验收门：隔离 worktree 复扫 core 零覆盖（miss≥1 口径）清零且无未入档残留 + core 全量测试绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- 不追 record/enum 编译合成行（switch 桥接、lambda 合成方法）——LINE 计数已含但属编译器产物，豁免入档。
- 不做分支覆盖（BRANCH）维度扩仓（fog 保持，另轮裁决）。
- 不触碰 I/J 会话在跑主题与号段产物。

## Further Notes

- 本轮后 core 在 miss≥1 口径下的零覆盖残留预计仅剩装配期匿名片段（AgentSession 内部匿名 / SmartLifecycle 匿名类）——与 R1 豁免台账一致即收官，不一致则逐条入档。
- 「小而行为敏感」的教训入台账：判据是过滤噪音的工具，不是豁免义务——门槛收紧后浮出的类要么测、要么显式豁免，不许静默。
