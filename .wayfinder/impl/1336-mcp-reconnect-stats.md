# impl 1336 — McpReconnectStats 重连退避实效读面（R37 = effort #1736 / spec 1736 / T2673-T2674）

**What**：尝试/成功/放弃三计数+成功率+退避均值峰值
**Why**：gRPC channelz——重连策略数据依据
**Verify**：McpReconnectStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
