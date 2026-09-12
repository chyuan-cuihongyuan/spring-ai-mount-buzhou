---
id: T958
title: MCP keepalive 空闲探活的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T957
created: 2026-09-13
---

## Question

探活真发 RPC？失败真重建（旧连接排空、新连接注册）？健康连接零扰动？默认关零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 4 轮）：① fake 连接 listToolNames 计数——probeOnce 后 ok 计数 +1（真探活）；② 探活抛异常——failed 计数 +1 且工厂重建被调、新连接注册 ACTIVE、旧连接 draining 关闭；③ 良性连接在坏邻居重建后仍 ACTIVE（不误伤）；④ 未配置 keepaliveInterval——probe 调度器不启动、既有 registry 用例零回归；⑤ 已摘除条目（refresh 竞态）不重复动作。`mvn -pl buzhou-mcp -am test` 全绿。
