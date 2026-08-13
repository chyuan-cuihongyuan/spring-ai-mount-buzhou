# 实现纵切片（Implementation slices）

`.wayfinder/impl/` 是 [`SPEC.md`](../SPEC.md)（`/to-spec` 产物）经 `/to-tickets` 切成的 **tracer-bullet 实现纵切片**——每片是单次上下文窗口可完成、独立可验证的端到端深化。

与 [`../tickets/`](../tickets/) 的 **决策票 T1–T9**（wayfinder：research / grilling / prototype / task，用于「该决定什么」）**共存**：凡实现片被一个未决决策门控，该决策票即列为它的 blocker。编号 `01..08`（实现片）独立于 `T1..T9`（决策票）。

## 依赖图

```
01 CI 转绿 ─────┬─► 05 可运行 demo ◄── 决策 T4
               ├─► 06 真实 LLM 测试 ◄── 决策 T5
               └─► 08 四机制深度测试 ◄── 决策 T3

02 .scratch 移出        （无 blocker）
03 README 降级          （无 blocker）
04 Spring AI 边界文档    （无 blocker，T2 已闭合）
07 run_command 安全默认 ◄── 决策 T6
```

## Frontier（可立即领取）

- **01** — 无 blocker，可立即开工（ci.yml 修复已应用，badge 验证待用户 push）。
- ✅ **02 已完成**（`.scratch/` 移出 git 跟踪 + `.gitignore`）。
- ✅ **03 已完成**（README「生产」措辞降级，与 alpha 对齐）。
- ✅ **04 已完成**（与 Spring AI 边界文档，中英双语）。
- ✅ **07 已完成**（run_command safe-default 既有实现 + 跨平台约束 javadoc + T6 决策）。
- ✅ **05 已完成**（可运行 src/main demo BuzhouDemo：stub-first + 微压缩 + evidence 回查，T4 拍板）。
- 06 — 等决策票 **T5** 拍板（依赖前提已满足）。
- 08 — 等决策票 **T3** 拍板。

## 索引

| # | 标题 | Blocked by |
|---|------|-----------|
| [01](01-ci-green-on-clean-runner.md) | CI 在干净 GitHub runner 上稳定转绿 | 无 |
| [02](02-untrack-scratch.md) ✅ done | `.scratch/` 移出 git 跟踪 + `.gitignore` | 无 |
| [03](03-readme-wording-downgrade.md) ✅ done | README「生产」措辞降级 | 无 |
| [04](04-spring-ai-boundary-doc.md) ✅ done | 撰写「与 Spring AI 原生能力边界」文档 | 无（T2 已闭合）|
| [05](05-runnable-main-demo.md) ✅ done | 提供真正可运行的 `src/main` demo | 决策 T4 |
| [06](06-real-llm-integration-test.md) | 加入至少一条真实 LLM 行为的集成测试 | 01 + 决策 T5 |
| [07](07-run-command-safe-default.md) ✅ done | `run_command` 原子工具安全默认 | 决策 T6 |
| [08](08-depth-tests-four-mechanisms.md) | 四机制「做深做透」深度测试基线 | 决策 T3 + 01 |

## 约定

- 每片遵循 `/to-tickets` 的 local-ticket 模板：`**What to build**`（端到端行为，非逐层实现清单）/ `**Blocked by**` / `**Status: ready-for-agent**` / 勾选式验收标准。
- 领取：开工前把 `Status` 改 `claimed`（或加 `Assignee:`）；解决：勾完验收标准 + 末尾补 `## Resolution`，`Status` 改 `done`。
- 不写具体文件路径 / 代码片段（易过期）；决策性骨架（如 NATIVE/ADDS/REPLACES 表）在 [SPEC.md](../SPEC.md) 与决策票内。
