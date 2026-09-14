---
id: T2260
title: BuzhouTool destructive 风险注解的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2259
created: 2026-09-15
---

## Question

M 会话第 5 轮：注解驱动名单如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-tools -am test` 绿——
① 既有 enabledDangerousToolNames 断言零变化（containsExactlyInAnyOrder("write_file","run_command","http_request")——行为等价迁移）；
② 标注正确性钉住：RunCommandTool/SandboxRunCommandTool/WriteFileTool/HttpRequestTool 的 @BuzhouTool.destructive()=true、ReadFileTool/TodoTool=false（注解面回归防线）；
③ 默认装配（三写侧工具全关）名单为空不误伤。
（订正：ToolsModule 无第三方工具装配入口——builder 无 tools() 槽，附加工具走 RuntimeConfig.autoTools 域，故验收取标注正确性而非第三方入册。）
