# nacos-spring-ai-alibaba-example for A2A

该工程项目主要是一个使用[Spring AI Alibaba](https://java2ai.com/)和[Nacos](https://nacos.io/)快速开发AI Agent的样例Example工程。

该工程在[Spring AI Alibaba Example](https://github.com/springaialibaba/spring-ai-alibaba-examples)的基础上，使用Nacos + Spring AI Alibaba，快速开发简单的Agent并暴露A2A协议服务，同时注册到Nacos上并让上有Agent应用发现且能够进行远程调用，实现Agent的分布式部署。

该样例主要覆盖的场景有：

- A2A（Agent2Agent）协议支持
- Agent的自动注册与发现

## 0. 环境依赖

该样例工程需要一定的环境依赖，包括：JDK 版本，Nacos版本等：

- JDK版本：JDK 17+
- Nacos版本：Nacos 3.1.0+
- Maven版本：3.6.3+
- 阿里云百炼的API-KEY

> JDK及Maven的安装，请自行查找资料进行安装。
> 
> Nacos的安装与启动，请参考[Nacos快速开始](https://nacos.io/docs/latest/quickstart/quick-start/?spm=5238cd80.2ef5001f.0.0.3f613b7c7dovyI)或[Nacos Docker 快速开始](https://nacos.io/docs/latest/quickstart/quick-start-docker/?spm=5238cd80.2ef5001f.0.0.3f613b7c7dovyI)进行安装。

> 阿里云百炼的API-KEY可参考[文档](https://bailian.console.aliyun.com/?spm=5176.30371578.J_wilqAZEFYRJvCsnM5_P7j.1.e939154a5W1LzI&tab=api&scm=20140722.M_10875430.P_126.MO_3931-ID_10875430-MID_10875430-CID_34338-ST_14391-V_1#/api/?type=model&url=2712195)获取，若已获取，则忽略此步骤。
> 
> 首次开通阿里云百炼时会提供100万Token的免费额度。

## 1. 启动A2A样例

> 在启动用例前，请确认已经安装好了JDK，Nacos，Maven，以及获取了阿里云百炼的API-KEY。

### 1.1. 配置环境变量

在启动样例之前，需要配置好环境变量，包括Nacos的地址和百炼的API-KEY。

```shell
# 必填，否则无法访问百炼大模型
export BAILIAN_API_KEY=${your_bailian_api_key}
# 可选，默认为nacos，如果您部署的Nacos集群有自定义用户，需设置此环境变量为正确的Nacos访问用户
export NACOS_USERNAME=nacos
# 可选，默认为nacos，如果您部署的Nacos集群有自定义密码，需设置此环境变量为正确的Nacos访问用户密码
export NACOS_PASSWORD=nacos
# 可选，默认为127.0.0.1:8848，如果您部署的Nacos地址不在本机，需设置此变量为正确的Nacos地址
export NACOS_ADDRESS=127.0.0.1:8848
```

### 1.2. 启动样例 A2A Server

```shell
cd a2a-example/a2a-server-example
mvn spring-boot:run
```

### 1.3. 启动样例 A2A Client

```shell
cd a2a-example/a2a-client-example
mvn spring-boot:run
```

> 至此，样例工程已经启动完成， 在[Nacos控制台](http://127.0.0.1:8080/index.html)中，您现在可以看到：
> Agent管理中有一个名为`Nacos Agent`的Agent。

## 2. 测试样例

测试样例，打开浏览器访问[http://127.0.0.1:8888/index.html](http://127.0.0.1:8888/index.html)，在页面中，是一个简单的聊天框，您可以输入问题，并点击发送按钮，查看Agent的回答。

以下问题的样例和可能的回答：

> 什么是Nacos？

```
Nacos（全称：Dynamic Naming and Configuration Service，中文名“纳管”）是阿里巴巴开源的一个动态服务发现、配置管理和服务管理平台。它旨在帮助开发者更方便地构建云原生应用，特别是在微服务架构中实现服务注册与发现、配置中心、服务治理等功能。

🌟 Nacos 的核心功能：
服务发现与注册

支持服务的自动注册与发现。
提供基于 DNS 或 API 的服务查找。
支持多种协议（如 HTTP、gRPC、Dubbo、Spring Cloud 等）。
集成 Kubernetes 和主流微服务框架（如 Spring Cloud Alibaba、Dubbo）。
动态配置管理

统一管理所有环境下的应用配置。
配置更改后可实时推送到客户端，无需重启服务。
支持配置版本管理、灰度发布、回滚等操作。
提供权限控制和审计日志。
服务元数据管理与健康检查

自动检测服务实例的健康状态（心跳机制），剔除不健康的节点。
支持权重路由、标签路由等高级流量管理能力。
动态 DNS 服务

可以基于服务名动态解析到具体的 IP 地址和端口。
支持负载均衡策略。
服务治理（限流、降级、熔断等）

结合 Sentinel 实现流量控制和服务容错。
多环境、多租户支持

支持 dev / test / prod 等不同环境隔离。
命名空间（Namespace）、分组（Group）、Data ID 三级结构实现配置和服务的精细划分。
✅ 使用场景
微服务架构中的服务注册中心（替代 Eureka、Consul 等）。
配置集中化管理（替代 Apollo、Spring Config Server）。
多语言混合架构下的统一服务治理。
云原生环境下与 Kubernetes 深度集成。
🔧 技术特点
特性	描述
开源免费	Apache License 2.0 协议
高可用	支持集群部署，CP（一致性）+ AP（可用性）模式（基于 Raft）
易用性	提供可视化控制台、RESTful API
扩展性强	插件化设计，易于二次开发
社区活跃	国内广泛使用，文档丰富
📦 官方地址
GitHub: https://github.com/alibaba/nacos
官网文档: https://nacos.io
💡 举个简单例子
比如你在 Spring Cloud 中使用 Nacos：

spring:
cloud:
nacos:
discovery:
server-addr: 127.0.0.1:8848  # Nacos 服务地址
config:
server-addr: 127.0.0.1:8848
file-extension: yaml
这样你的服务会自动注册到 Nacos，并能从 Nacos 获取配置。

如果你正在做微服务项目，尤其是基于 Spring Cloud Alibaba 或 Dubbo 构建，Nacos 是一个非常推荐使用的基础设施组件。

需要我帮你搭建一个 Nacos 示例吗？😊
```