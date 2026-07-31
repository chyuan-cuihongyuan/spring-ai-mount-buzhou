# HITL 危险操作守卫设计

Type: grilling
Status: open
Blocked by: 05, 22, 23

## Question

敏感/不可逆操作的人工审核机制：危险工具清单的配置驱动模型（参照 DECO 的 yaml：name + required-state + hint + confirmation 多选项/带输入控件）；授权标记（state key）的会话级存储与放行语义（LLM 重调同工具时守卫识别已授权）；暂停/恢复工程形态——阻断后发确认事件给前端，用户选择经 REST 写回并发起续跑（依赖 22 调研：Spring AI 2.x 能否原生暂停/恢复，还是走 DECO 式「阻断 + state + 重试」）；确认交互的通道协议（SSE 自定义事件子类型？）；变更预览（COMMIT_PREVIEW 类富确认）的抽象边界——框架给通用 yes/no + 表单，业务级预览留给扩展？多实例部署下续跑请求打到另一台实例能否正确放行（state 必须持久化——依赖 06）？
