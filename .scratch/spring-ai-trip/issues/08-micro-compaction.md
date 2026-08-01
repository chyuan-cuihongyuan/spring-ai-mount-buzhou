# 微压缩策略模型

Type: grilling
Status: resolved
Blocked by: 02, 06

## Question

工具结果微压缩的精确规则：完结轮次如何判定（横跨多条消息的工具调用链，"整轮真正结束"的判据）？按工具分策略的策略模型（永不压缩清单、过期即清、存活轮数）长什么样？evidence-id 的生成规则与回查 API？替换占位符的确切文案与注入形态？压缩时机（新一轮加载历史注入模型前那一刻）在管线中的确切位置？

## Answer

**定案：结论落地即完结 + 注入前总先微压缩 + 三字段策略 + 消息 id 即证据指针。**

1. **完结轮次判据**：一轮完结 = 历史中该轮所有 tool_calls 均有对应 ToolResponse，且其后存在不含 tool_calls 的 assistant 文本回复（模型已给出结论）。微压缩只动完结轮次内的工具结果，在途轮次绝不触碰；判定在加载时基于持久化消息模型计算（参照 AgentScope `findSafeCutoffPoint`「不拆对」原则）。
2. **触发时机**：加载历史构建注入视图时**总是先跑微压缩**（纯内存、零 LLM 成本）→ 再算动态预算 → 仍超阈值才动用 LLM 摘要（忠于蓝本「压缩发生在注入那一刻」与读写分离；与 Claude Code 每轮 MicroCompact 对齐）。
3. **策略模型**（工具级，工具声明默认 + 配置通配覆盖——机制见 ticket 05）：
   - `neverCompress`：关键操作死保（默认 false；写类/不可逆类内置工具默认 true）；
   - `maxAgeTurns`：结果存活轮数，超过即可回收（默认 3）；
   - `minSizeChars`：小于此不回收（默认 200，小结果不值得清）；
   - 全局 `protectRecentTurns`：最近 N 轮原文死保（默认 1）。
4. **占位符**（忠于蓝本）：`[旧工具结果已清理，可按 evidence-id=<msgId> 回查]`，占位符替换发生在注入视图层，持久层原文不动。
5. **evidence-id 与回查**：evidence-id 直接就是持久层消息 id（MessageStore 全保真，原文永远在，无额外写放大）。框架提供统一**证据回查内置工具**（按 id 取原文，支持范围读取——范围读取实现放 core，与 Spill 回读工具共享），由 memory 模块提供、默认自动注册，可被工具策略关闭。

### 影响面

- ticket 12（Spill 回读工具）：范围读取实现不再归 spill 独有，提升为 core 共享能力，spill 回读与 evidence 回查是两个包装。
