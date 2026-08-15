---
Type: task
Status: closed
---
## Question

examples 演示：加密开关开/关行为/单飞并发快速失败/读降级续聊三用例（FakeChatModel/ScriptedChatModel 驱动）。

## Resolution

AFK 自决：三用例（examples 接缝文档）——加密开关开/关行为对照（明文 vs 密文+透明读+旧文件兼容）、
单飞并发快速失败（结构化错误码 + 终结后可续）、读降级续聊（OFF 上抛不变 / EMPTY 空历史保活 + 写路径
不受影响）。产 impl-135（spec 45 已覆盖对应机制面）。
