---
Type: task
Status: open
---
## Question

MCP 工具结果入上下文尺寸防护（新缺口）：MCP server 返回的工具结果无尺寸上限即进模型上下文（DB 查询工具返回万行、远程 fetch 返回整页 HTML 场景）；本地工具侧有 spill/ReadRange 兜底，MCP 侧裸奔。需要决策：防护位置（McpConnection 调用包装 vs HarnessToolCallingManager 结果后处理 vs 通用 ToolResultLimiter hook）、阈值口径（字符数 vs token 估算）、超限行为（硬截断+提示 vs 截断+转 spill 引用提示读取）、配置粒度（全局 vs per-server）。产出 spec 31 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **防护位置：通用后处理，落 core `exec` 结果包装层（HarnessToolCallingManager 工具结果统一出口）**——不绑 MCP（本地工具同样受益）；按工具名前缀可豁免（如 spill 自家的 read_range 已自治理）。
2. **阈值口径：字符数（默认 20_000 chars ≈ 5K token），可配 `buzhou.tools.result-limit-chars`**——token 估算在结果侧多一次全量扫描成本，字符数够近似且零歧义。
3. **超限行为：截断 + 结构化提示头**——保留头部原文 + `…[结果已截断：原始 N 字符，超出 M。可用 spill 工具分页读取或细化查询]` 提示尾；**不自动转 spill**（自动 spill 需要工具结果与工具调用的关联落盘，M1 复杂度不抵收益，fog 记录）。事件 `tool.result.truncated`（toolName/originalChars/limit）。
4. **配置粒度：全局阈值 + per-tool 覆盖 map**（`buzhou.tools.result-limit-overrides`，glob 工具名，值=字符数或 -1 禁用）——per-server 粒度需要 server→tool 映射维护，等漂移检测（T86）基线消费成熟再议。
