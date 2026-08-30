# impl-82 — 会话可移植导出/导入

**What to build:** 单 JSON 文档承载 messages+summary+state；导入默认 Id 重映射、keepIds
冲突 fail-fast；导入会话可 spawn 续用；spill 引用清单派生。

**Blocked by:** T105（悬垂读容错）、T106（mediaRefs 随 metadata 自然导出）——均已闭合

**Status:** done

- [x] `SessionExport`（format/version 单文档 + toJson/fromJson DTO epoch-millis + spillRefs 派生）
- [x] `AgentRuntime.exportSession/importSession` default UOE + DefaultAgentRuntime 实现
      （重映射一致重写、SessionImportException、指标+日志、不建租约）
- [x] 测试：往返保真+续用 / keepIds 跨环境落位+同环境冲突 / 三类拒绝 / spill 清单——core 277/277 绿
- [x] spec 28 新篇

## Done

commit：见 git log（impl-82）。
