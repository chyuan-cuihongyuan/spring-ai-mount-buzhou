# 1128 — serial-groups yml 通道（M 系 R28）

**What to build:** fromYml serial-groups 解析 + configure yml 优先合并。

**Blocked by:** T2301 / T2302（同轮 shape+verify）。

**Status:** done

- [x] Builder.ymlSerialGroups + 实例字段中转 + configure 合并
- [x] ToolsModuleTest 三断言用例 + tools 118 用例零回归

## Done

验证：定向测试绿。commit 见本轮 feat 提交。
