---
Type: task
Status: open
---
## Question

离线黄金轨迹回归评估（新缺口，质量面）：examples 有零散端到端用例，但无「黄金轨迹」回归集——给定脚本化模型响应序列 + 断言 harness 行为（压缩触发点、预算拦截、降级切换、配额重置）的系统性用例库；LangSmith evals / OpenHands runtime evals 的本地零依赖版。需要决策：用例库形态（examples 内 @Tag(golden) 测试集 vs 独立 buzhou-eval 模块 vs 脚本化 JSON 用例+通用 runner）、断言维度（事件序列断言 API）、CI 接入（进 ci.yml 还是独立 workflow）、与红队/perf 夜间门的关系。产出 spec 32 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **形态：examples 内 `golden/` 包 @Tag("golden") JUnit 测试集 + 事件序列断言工具类 `EventSequenceAssert`**——不建独立模块（评估器本质是端到端测试的断言增强，独立模块徒增构建矩阵）；不搞 JSON 用例 runner（脚本化响应用 Java 构造最直接，JSON DSL 是伪需求）。
2. **断言维度**：`EventSequenceAssert`（收集 SessionEvent 流）支持：类型序列匹配（containsInOrder）、payload 字段路径断言（JsonPath 风格 getter）、间隔约束（事件 A 后必须/不得出现 B）、计数断言。黄金用例 v1 覆盖六大机制各 ≥1：压缩触发、预算拦截、降级链切换、配额重置、熔断恢复、REASK。
3. **CI 接入：进 ci.yml 常规跑**（黄金集是回归不是性能/对抗，快且该挡）——golden tag 只用于过滤选择，默认包含。
4. **与夜间门关系**：互不重叠——红队测对抗输入、perf 测时延、golden 测机制行为回归；runbook 告警清单引用黄金集失败语义。
