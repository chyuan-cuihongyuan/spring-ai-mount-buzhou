# 600 — MCP 工具注解观测面与注解漂移

> 借鉴：[modelcontextprotocol/spec](https://github.com/modelcontextprotocol/modelcontextprotocol) 工具 `annotations` 字段语义（readOnlyHint / destructiveHint / idempotentHint / openWorldHint / title）。
> 来源：F 会话第 1 轮 = effort #600（600 系 50 轮自迭代） / [T851](../../.wayfinder/tickets/T851-mcp-tool-hints.md) / impl 453。

## 背景

MCP 协议为每个工具定义可选 `annotations`，向客户端声明工具的行为特征（是否只读、是否有破坏性、是否幂等、是否开放世界交互）。本仓 `buzhou-mcp` 建连时只消费工具名与 `ToolCallback`，注解整体丢弃——运维看不到 server 自报的工具风险画像，server 端悄悄翻转注解（如把 `readOnlyHint=true` 改为 `false`）也无任何信号。

## 目标

1. 注解以 **buzhou 自有类型** 进入目录观测面：`McpConnection.toolHints()` → `McpClientRegistry.toolHints()` 聚合（server → tool → hints 的基线快照，查询零 RPC）。
2. 漂移口径扩展：`tools/list_changed` 通知中同名工具 hints 变化 → 独立事件 `mcp.tool-hints-drift`（payload：server / changedCount / changed 名单，名单有界）+ 指标 `buzhou.mcp.tool-hints-drift`（tag: server）+ WARN 日志。
3. 建连快照一次成型：工厂建连时单次 `listTools` 同时缓存工具名基线与 hints 基线（RPC 次数与现状持平）。

## 非目标

- **不做护栏裁决**：注解不接 guard gate、不改变 `dangerousToolNames()` 的客户端风险分类口径（既有决策：不信任 server 自报元数据）。
- 不做注解的配置化覆写/白名单（客户端侧覆写属于后续独立机制）。
- 不改名字差量漂移事件 `mcp.tools-drift` 的既有口径（向后兼容）。

## 设计

- 新公共 record `McpToolHints(String title, boolean readOnlyHint, boolean destructiveHint, boolean idempotentHint, boolean openWorldHint)`；静态工厂 `from(McpSchema.Tool)`：`annotations()` 为 null 时全 false + 空标题（MCP 规范未声明即未知，统一按 false 记录）。
- `McpConnection.toolHints()`：default `Map.of()`——伪连接/自定义实现零成本兼容。
- 注册表条目新增 `hintsBaseline`（volatile 整体替换，与 `toolNamesBaseline` 同生命周期）：
  - `handleToolsChanged` 在名字差量之外，对**两边基线都有 hints** 的同名工具做 `McpToolHints` 相等性比较；非空差集发 `mcp.tool-hints-drift` 并推进基线；**基线无 hints（旧 server/伪连接）时跳过 hints 差量**（退化不误报）。
  - 名字差量非空、hints 差量为空 → 只发既有 `mcp.tools-drift`；两者都有 → 各发各的（两条独立事件）。
- 空差量静默语义不变：同一通知既无名字差量也无 hints 差量 → 不发任何事件。
- 指标走 `BuzhouMetricsHolder`（与 `buzhou.mcp.tools-drift` 同风格，tag 仅 server）。

## 事件与指标

| 名称 | 触发 | payload |
|------|------|---------|
| `mcp.tool-hints-drift` | 同名工具注解变化非空 | server / changedCount / changed（上限 20） |
| 指标 `buzhou.mcp.tool-hints-drift` | 同上 | counter，tag: server |

## 测试

- 单元（buzhou-mcp）：聚合面——伪连接提供 hints 后 `registry.toolHints()` 可见、DRAINING 条目不可见；注解漂移——同名翻转 readOnlyHint 发 `mcp.tool-hints-drift` 且**不发** `mcp.tools-drift`；注解与名字同变 → 两事件都发；hints 不变重复通知 → 静默；基线无 hints → 跳过不误报。
- 既有 `McpToolsDriftTest` 全绿（名字差量口径零回归）。

## 兼容性

- 纯增量：新 default 方法 + 新事件类型，无既有签名变更；默认零行为变化（不配置不消费即无感知）。
