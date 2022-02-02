# BotDefender

基于 IP 信誉的恶意攻击识别和阻止工具

## 🚧 正在施工中

当前项目正在施工，部分功能可能不可用，请勿在生产环境中使用。

## 客制化工具

此项目为设计为在 KarNetwork 下使用的客制化工具，可能无法满足所有人的需求，请谨慎使用。

## 特点

* 调用系统 iptables 和 ipset 防火墙进行 IP 屏蔽
* 限速器
  * Handshake Flood
  * Ping Flood
  * Tab-Complete Flood
  * ~~数据包速率控制器~~  暂时不需要，有 ViaVersion
* 基于 IP 地址地理位置和 ASN 的信誉评分的反机器人系统
* 使用 gRPC 调用特权应用程序进行 IP 屏蔽
  * 系统级 iptables 性能更好，而且无需运行在 root 模式下，避免漏洞带来的风险

## 环境

* WaterFall 及其分支
* Java 8+ 版本 JRE
* 在 root 权限下运行的 BotDefender-Native

