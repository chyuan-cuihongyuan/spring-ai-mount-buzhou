# 内置原子工具清单与安全边界

Type: grilling
Status: open
Blocked by: 03

## Question

内置原子工具的范围与边界：文章点名文件读写、命令执行、任务清单、HTTP 调用——开源版给哪些、每个的参数 Schema 要点？安全边界（命令执行的目录限制/黑名单、文件读写的根目录沙箱、HTTP 调用的内网防护 SSRF）默认策略是什么？任务清单（todo）工具的数据放哪（会话作用域？）？这些工具默认开启还是显式 opt-in？与 Skill 的关系（工具的实现说明是否以 Skill 形式下发）？
