# 内置原子工具清单与安全边界

Type: grilling
Status: resolved
Blocked by: 03

## Question

内置原子工具的范围与边界：文章点名文件读写、命令执行、任务清单、HTTP 调用——开源版给哪些、每个的参数 Schema 要点？安全边界（命令执行的目录限制/黑名单、文件读写的根目录沙箱、HTTP 调用的内网防护 SSRF）默认策略是什么？任务清单（todo）工具的数据放哪（会话作用域？）？这些工具默认开启还是显式 opt-in？与 Skill 的关系（工具的实现说明是否以 Skill 形式下发）？

## Answer

**定案：无害默认开/危险 opt-in + 沙箱黑名单 SSRF 默认防护 + todo 入会话 state + 瘦 Schema 深度说明走 Skill。**

1. **清单**（首发）：`read_file` / `write_file`（文件读写）、`run_command`（命令执行）、`todo`（任务清单）、`http_request`（HTTP 调用）；框架机制衍生：`read_range`（12）、`load_skill`（16）、evidence 回查（08，与 read_range 同能力另一包装）。
2. **默认开关**：无害工具（read_range/load_skill/todo/read_file 只读）默认开；危险工具（write_file/run_command/http_request）默认关，绑定级配置 opt-in 注册，且默认挂 25 的危险工具守卫——05「安全项全开」原则落地。
3. **安全边界默认**：文件读写根目录沙箱（默认工作目录，可配白名单）；run_command 限沙箱内执行 + 命令黑名单（rm -rf /、mkfs、dd 等）+ 超时；http_request 默认拦内网段与云元数据端点（SSRF 防护），可配置放行；全部走 05 策略分层可调。
4. **todo 数据**：会话作用域，入 SessionStateStore（06），随会话持久化跨实例续接——任务清单是「补失忆」事实载体，与 26 闭环同构。
5. **与 Skill 关系**：工具保持瘦 Schema（简短 description）；深度用法与最佳实践以内置 Skill 下发，模型按需 load_skill；工具与 Skill 正交。
