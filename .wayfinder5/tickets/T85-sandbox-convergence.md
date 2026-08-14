---
Type: task
Status: open
blocked-by:
---
## Question

run_command 与 guard CommandSandbox 双轨如何合流？现状：RunCommandTool 自建 FileSandbox+CommandBlacklist+裸 ProcessBuilder；guard 的 Deno/E2E/Firecracker CommandSandbox 实现与 tools 零互引，危险路径跑的是较弱的一套。约束：依赖图物理无环、tools 不依赖 guard。决策点：port 接口放哪（core SPI 新接口 CommandExecutionBackend？guard 实现+桥接注册，tools 运行时 Optional 注入）、降级路径（无 guard 时回退现有实现）、配置面（buzhou.tools.command.sandbox=disabled|process|guard-deno|guard-e2e）、审计与指标。产出 spec 17 + impl 60。
