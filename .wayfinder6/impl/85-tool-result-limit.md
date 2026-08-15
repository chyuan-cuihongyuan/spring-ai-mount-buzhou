# impl-85 — 工具结果尺寸防护

**What to build:** 工具结果入模型上下文前的字符限幅（默认 20K + 提示尾 + per-tool glob
豁免），MCP 大结果不再炸上下文。

**Blocked by:** None（T110 已闭合）

**Status:** done

- [x] `ToolResultLimiter`（截断+提示尾 / glob 覆盖 / read_range 默认豁免 / 指标）
- [x] `ToolResultLimiterHolder` 全局默认 + auto-config 据配置灌入（buzhou.tools.*）
- [x] `HarnessToolCallingManager` 响应收集点应用 + per-session setter
- [x] 测试：五用例 + 全量回归——core 289/289 绿
- [x] spec 31 新篇

## Done

commit：见 git log（impl-85）。
