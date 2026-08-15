# impl-128 — run_command 输出兜底上限可配

**What to build:** 输出内存兜底上限按部署可配（缺省 5MB 不变）；截断语义测试钉住。

**Blocked by:** None

**Status:** done

- [x] RunCommandTool 七参构造 + DEFAULT_MAX_OUTPUT_BYTES + 截断标记带实际上限
- [x] ToolsModule run-command.max-output-bytes（非正 fail-fast）
- [x] 测试：10KB 截断/5MB 缺省钉住/非法拒绝——tools 54 绿
