# 951 — write_file noclobber 防误覆盖模式

> 来源：I 会话第 50 轮 = effort #951（[T1337](../../.wayfinder/tickets/T1337-noclobber-shape.md) / [T1338](../../.wayfinder/tickets/T1338-noclobber-verify.md) / impl 698）。借鉴：csh `set -C` noclobber / `cp -n`——覆盖性写需要存在性守门（模型侧「误覆盖」不可恢复）。

## 背景

`write_file` 对已存在文件无条件覆盖（原子替换防截断，但**旧内容被替换**）——模型「想追加/想新建」的意图误写成覆盖时原内容丢失不可恢复。noclobber 是 Unix shell 数十年的防误覆盖守门语义。

## 目标

- `WriteFileTool` opt-in 实例开关 `setNoclobber(boolean)`（默认 false=既有覆盖语义逐位不变）：
  - noclobber 下 `Files.exists(target)`（`sandbox.resolveForWrite` 之后、创建目录之前）→ 返回失败消息「write_file 拒绝：目标已存在（noclobber 模式）——如需覆盖请先删除或关闭 noclobber」不写盘；
- 工具 description 追加 noclobber 行为说明（模型可感知；装配侧经既有 BuzhouTool 注册路径透出）；
- 判定位置在写盘之前——失败路径零副作用（不创建父目录、不留 tmp 文件）。

## 测试

三态：新建成功 / 存在拒绝且原内容不变 / 默认关闭覆盖成功。

## 兼容性

opt-in：默认 false 全行为逐位不变。
