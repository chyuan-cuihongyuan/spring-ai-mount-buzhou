# 1110 — MCP 危险工具默认动词模式（M 系 R8）

**What to build:** BuzhouMcpProperties 缺省 → 七动词前缀 glob 默认集；显式空保留 = 关闭逃生门。

**Blocked by:** T2265 / T2266（同轮 shape+verify）。

**Status:** done

- [x] DEFAULT_DANGEROUS_TOOL_PATTERNS 常量 + compact constructor null→默认
- [x] Javadoc 三态语义（缺省=默认集/空=显式关/非空=透传）
- [x] 属性面三断言 + 注册表端到端命中/不误伤断言
- [x] 既有 McpRealProtocolTest 零回归 + design-incompleteness S1 标记闭环

## Done

验证：mcp 模块测试绿。commit 见本轮 fix 提交。
