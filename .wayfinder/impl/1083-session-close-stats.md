# 1084 — 会话关闭耗时读数

**What to build:** SessionCloseStats 公共静态面（closed/closeFailures/last+max 耗时水位）+ DefaultAgentSession.close() 埋点 + 四测。

**Blocked by:** 全仓 verify 运行——完成后编码。

**Status:** done

- [x] SessionCloseStats（core/session 公共静态面）
- [x] close() 埋点（failures 非空记失败——评审修正：close 契约为清理完毕后首失败上抛，测试改 assertThatThrownBy 断言）
- [x] SessionCloseStatsTest 四测
- [x] spec 1430 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='SessionCloseStatsTest'` 4/4 绿。
