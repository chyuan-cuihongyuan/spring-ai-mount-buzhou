# impl 1317 — VirtualKeyShareStats 虚拟键份额读面（R18 = effort #1717 / spec 1717 / T2635-T2636）

**What**：份额降序 LinkedHashMap 保序+HHI 集中度+reset
**Why**：OpenRouter 多键遥测+HHI——轮换失效显形
**Verify**：VirtualKeyShareStatsTest 5 断言（firstEntry 编译修正） 全绿。 **Status**：done（2026-09-15）
