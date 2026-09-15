---
id: T1852
title: R22 HookAdvisor 补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1851
created: 2026-09-15
---

## Question

R22 补测后：HookAdvisor 分支覆盖提升多少？buzhou-core 模块是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，定向 + 模块复测）：

1. **分支提升**：HookAdvisor 70%→**100%**（20/0，切面分发全覆盖）。
2. **语义修正入档**：初版用例误建模「onModelError 直接向上转发 Replace」——HookChain.run 的链契约是 applyReplace 回填 ctx 后 continue（Replace 不向上转发）；按链契约重建模（hook 应用 Replace→CONTINUE）后 9 用例全绿。测试建模错误非主代码缺陷。
3. **同构对称**：adviseCall 与 adviseStream 逐分支成对断言（Block 短路/replace 回填/放行 rethrow）。
4. 主代码零变化。
