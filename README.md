# nacos-spring-ai-alibaba-example

该工程项目主要是一个使用[Spring AI Alibaba](https://java2ai.com/)和[Nacos](https://nacos.io/)快速开发AI Agent的样例Example工程。

该工程在[Spring AI Alibaba Example](https://github.com/springaialibaba/spring-ai-alibaba-examples)的基础上，将数个与Nacos相关的example用例进行组合使用，形成一个完整的AI Agent的样例工程。

目前已经支持的样例场景有：

- 动态Prompt模版
- MCP服务注册与发现
- 存量HTTP Rest API转为MCP服务

正在进行支持的样例场景：

- A2A（Agent2Agent）协议支持
- Agent的自动注册与发现

## 0. 环境依赖

该样例工程需要一定的环境依赖，包括：JDK 版本，Nacos版本等：

- JDK版本：JDK 17+
- Nacos版本：Nacos 3.0.3+
- Maven版本：3.6.3+
- 阿里云百炼的API-KEY

> JDK及Maven的安装，请自行查找资料进行安装。
> 
> Nacos的安装与启动，请参考[Nacos快速开始](https://nacos.io/docs/latest/quickstart/quick-start/?spm=5238cd80.2ef5001f.0.0.3f613b7c7dovyI)或[Nacos Docker 快速开始](https://nacos.io/docs/latest/quickstart/quick-start-docker/?spm=5238cd80.2ef5001f.0.0.3f613b7c7dovyI)进行安装。

> 阿里云百炼的API-KEY可参考[文档](https://bailian.console.aliyun.com/?spm=5176.30371578.J_wilqAZEFYRJvCsnM5_P7j.1.e939154a5W1LzI&tab=api&scm=20140722.M_10875430.P_126.MO_3931-ID_10875430-MID_10875430-CID_34338-ST_14391-V_1#/api/?type=model&url=2712195)获取，若已获取，则忽略此步骤。
> 
> 首次开通阿里云百炼时会提供100万Token的免费额度。

## 1. 启动MCP样例

> 在启动用例前，请确认已经安装好了JDK，Nacos，Maven，以及获取了阿里云百炼的API-KEY。

### 1.1. 配置环境变量

在启动样例之前，需要配置好环境变量，包括Nacos的地址和百炼的API-KEY。

```shell
# 必填，否则无法访问百炼大模型
export BAILIAN_API_KEY=${your_bailian_api_key}
# 可选，默认为127.0.0.1:8848，如果您部署的Nacos地址不在本机，需设置此变量为正确的Nacos地址
export NACOS_ADDRESS_ENV=127.0.0.1:8848
```

### 1.2. 启动Mock数据服务

```shell
cd mcp-example/mock-data-example
mvn spring-boot:run
```

### 1.3. 启动MCP服务

```shell
cd mcp-example/mcp-provider-example 
mvn spring-boot:run
```

### 1.4. 导入HTTP Rest API作为MCP服务

打开[Nacos控制台](http://127.0.0.1:8080/index.html)，选择`MCP管理 > MCP列表`，然后点击`创建 MCP Server` 按钮，进入`创建 MCP Server`页面。

在MCP`创建 MCP Server`页面中，输入如下内容：

- MCP 服务名：`nacos-http-mcp-server`
- 协议类型： `sse`
- HTTP转MCP服务： `开启`
- 后端服务： `新建服务`
- 新建服务-address： ${your_nacos_ip} 
- 新建服务-port： ${your_nacos_http_port} (默认应为8848)
- 描述： `nacos http api transfer to mcp server. Provide namespace query, serivice query and subscribe query.`
- Tools： 点击`从 OpenAPI 文件导入工具`按钮，选择`mcp-example/mcp-trans-from-http-example/src/main/resources/nacos-http-swagger.json`文件后，点击`确定`按钮。

全部完成后，点击页面右上角的`发布`按钮即可。

### 1.5. 启动HTTP Rest API转换为MCP服务的Gateway程序

```shell
cd mcp-example/mcp-trans-from-http-example
mvn spring-boot:run
```

### 1.6. 发布动态Prompt到Nacos配置中心

打开[Nacos控制台](http://127.0.0.1:8080/index.html)，选择`配置管理 > 配置列表`，然后点击`创建配置` 按钮，进入`创建配置`页面。

在`创建配置`页面中，输入如下内容：

- DataId： `spring.ai.alibaba.configurable.prompt`
- Group： `DEFAULT_GROUP`
- 描述： `动态Prompt模版`
- 配置格式： `JSON`
- 配置内容：
  - ```JSON
        [{
            "name": "nacos-prompt",
            "template": "# Role\n\nAn assistant or maintainer for nacos clusters. \n\n## Role Profile\n\n- Language: Chinese\n- description: You are an assistant or maintainer for nacos clusters. Users will register and login some nacos clusters to you, and maybe ask you to query some datum or ask you some question about registered Nacos clusters. You should use tools to query Nacos cluster information and datum in target Nacos cluster, and then analyze datum to gain a result or answer for users' question or request.\n\n## Goals\n\n- Query datum in Nacos cluster by users' request.\n- Answer users' question about Nacos cluster.\n\n## Constrains\n\n1. For information that is not in your knowledge base, clearly tell the user that you don’t know it.\n2. You can call the tools you can found in sessions.\n3. You can call the content of official document in the knowledge base.\n\n## Skills\n\n- Find Nacos clusters basic information such as host, accessToken by using tools.\n- Register and Login Nacos clusters.\n- Query Nacos datum by using tools.\n- Understand mirco service relationship and dynamic configurations by analyzing datum from Nacos clusters.\n- Have a good sense of typography and use serial numbers, indents, separators, line breaks, etc. to beautify information layout.\n\n## Workflows\n\nYou will help users to maintain or query Nacos clusters according to the following framework and answer user's requests or questions:\n\n- Understand users' input question or datum requests about Nacos clusters.\n- Do query Nacos clusters basic informations and query datum, should follow these sub workflows:\n  1. Understand and extract the `name` or `alias` users request target Nacos cluster.\n  2. Get target Nacos cluster basic information by tools. If not found from tools, you should feedback to users and guide user login nacos cluster by tools.\n  3. According to the Nacos cluster basic information, using tools to query actual datum relative users' request or answers, It might be query multiple times or using many different tools. And the previous tools result might be the next tools input parameters, you should plan the order in which the tools will be called and call them in that order.\n- After gain the datums about Nacos, you should analyze the result datum and assemble to the answers for users. The answer should have a good sense of typography and use serial numbers, indents, separators, line breaks, etc. to beautify information layout.\n\n## Users' question or request \n\n{query}\n"
        },
        {
            "name": "nacos-prompt-cn",
            "template": "# 角色\n\nNacos集群的维护人员和助理。\n\n## 角色简介\n\n- 回复和思考的语言：中文\n- 角色描述：您是 Nacos 集群的助理或维护人员。用户可能会注册并登录一些 Nacos 集群，并可能要求您查询一些数据或询问有关已注册 Nacos 集群的问题。您需要使用工具在目标 Nacos 集群中查询 Nacos 集群信息和数据，然后分析这些数据以获得针对用户问题或请求的结果或答案。\n\n## 目标\n\n- 根据用户请求查询Nacos集群中的数据。\n- 解答用户关于Nacos集群的问题。\n\n## 限制\n\n1. 对于知识库中没有的信息，要明确告诉用户你不知道。\n2. 可以调用会话中可以找到的工具。\n3. 可以调用知识库中官方文档的内容。\n\n## 技能\n\n- 使用工具查找 Nacos 集群基本信息，例如 host、accessToken。\n- 注册并登录 Nacos 集群。\n- 使用工具查询 Nacos 数据。\n- 通过分析 Nacos 集群数据，了解微服务关系和动态配置。\n- 熟悉字体排版，并能使用序列号、缩进、分隔符、换行符等来美化信息布局。\n\n## 工作流程\n\n您将根据以下框架帮助用户维护或查询 Nacos 集群，并解答用户的请求或问题：\n\n- 理解用户关于 Nacos 集群的输入问题或数据请求。\n- 查询 Nacos 集群基本信息并查询数据，应遵循以下子工作流程：\n1. 理解并提取用户请求目标 Nacos 集群的 `name` 或 `alias`。\n2. 使用工具获取目标 Nacos 集群基本信息。如果工具未找到，则应反馈给用户并引导用户通过工具登录 Nacos 集群。\n3. 根据 Nacos 集群基本信息，使用工具查询与用户请求或答案相关的实际数据。这可能会多次查询或使用多个不同的工具。前一个工具的结果可能是下一个工具的输入参数，您应该规划工具的调用顺序，并按该顺序调用它们。\n- 获取Nacos相关数据后，需要对数据进行分析，并整理成用户所需的答案。答案需具备良好的排版感，并使用序号、缩进、分隔符、换行符等美化信息布局。\n\n## 用户的问题或请求\n\n{query}\n"
        }]
    ```

全部完成后，点击页面右下角的`发布`按钮即可。

### 1.7. 启动AI Agent样例

```shell
cd mcp-example/mcp-consumer-example/
mvn spring-boot:run
```

> 至此，样例工程已经启动完成， 在[Nacos控制台](http://127.0.0.1:8080/index.html)中，您现在可以看到：
> - 3个MCP服务：`nacos-cluster-provider`、`nacos-mcp-server-gateway` 和 `nacos-http-mcp-server`
> - 至少一个配置`spring.ai.alibaba.configurable.prompt`
> - 至少3个服务：`nacos-cluster-provider::1.0.1`、`nacos-mcp-server-gateway::1.0.0`、`random.service.name.{id}`（该服务可能有多个，id可能为0～1）

## 2. 测试样例

### 2.1. 询问有那些Nacos集群

打开浏览器，输入地址`http://localhost:18080/nacos/stream/chat`, 即可查看输出结果。

### 2.2. 注册并登陆Nacos集群

打开浏览器，输入地址`http://localhost:18080/nacos/stream/chat?query=我要登录一个Nacos集群local,ip为127.0.0.1端口为8848，用户名密码均为nacos`, 即可查看输出结果。

> 问题的内容奇怪根据您实际环境进行修改，例如您的Nacos地址为192.168.0.100， 用户密码为其他自定义密码，则问题为：我要登录一个Nacos集群local,ip为192.168.0.100端口为8848，用户名密码均为nacos-test。

> 完成此操作之后，可以再次使用[#2.1.]()所对应的问题查看新的回答。

### 2.3. 询问订阅指定服务的客户端有哪些，并同时获取其所属服务

打开浏览器，输入地址`http://localhost:18080/nacos/stream/chat?query=local集群中，订阅com.test.SyncCallbackService这个服务的客户端有哪些？并且想知道这些客户端注册了哪些服务`, 即可查看输出结果。

