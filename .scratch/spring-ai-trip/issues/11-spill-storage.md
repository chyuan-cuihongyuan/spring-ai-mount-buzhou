# Spill 存储抽象与生命周期

Type: grilling
Status: open
Blocked by: 06

## Question

Spill 的持久化设计：存储介质抽象（本地磁盘为默认，对象存储/分布式文件系统为可选 SPI？）；`spill://agentName/sessionId/toolId` 路径方案的跨实例问题（A 实例落的盘，B 实例如何回读——开源分布式场景必须回答）；会话生命周期绑定与清理（成功/失败/取消/超时都会释放）的实现机制；并发 spill 的文件命名不冲突规则；阈值（默认 32000 字符）与预览（默认 2048 字符）的按工具策略模型。
