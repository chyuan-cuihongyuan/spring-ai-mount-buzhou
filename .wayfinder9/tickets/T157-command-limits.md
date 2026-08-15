---
Type: task
Status: closed
---
## Question

RunCommandTool 内置 /bin/sh 路径无输出上限与进程树强杀（限额只在沙箱档 SandboxLimits 有）：内置路径补哪些可移植限额（输出字节上限/超时强杀子孙进程/截断标记）？与黑名单防线如何叠加？

## Resolution

AFK 自决（勘察纠偏）：图前扫描高估缺口——进程树强杀与 5MB 输出兜底已存在（impl-49/60，诚实入档）。
票据缩窄为真实缺口：输出内存兜底上限可配——RunCommandTool 七参构造（maxOutputBytes 正数 fail-fast、
DEFAULT_MAX_OUTPUT_BYTES=5MB 公开）+ ToolsModule yml 键 run-command.max-output-bytes（非正 fail-fast）
+ 截断语义钉住（标记可见/进程跑完/exit 照常）。rlimit/cgroup 出界（纯 JDK 不可移植；沙箱档已有
SandboxLimits）。产 spec 43 §A + impl-128。
