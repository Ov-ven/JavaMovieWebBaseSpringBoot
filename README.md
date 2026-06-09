<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.12-green?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/LangChain4j-0.29.0-blue?style=flat-square" alt="LangChain4j"/>
  <img src="https://img.shields.io/badge/Redis%20Stack-7.x-red?style=flat-square&logo=redis&logoColor=white" alt="Redis Stack"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Docker-Compose-blue?style=flat-square&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.3-blue?style=flat-square" alt="MyBatis-Plus"/>
  <img src="https://img.shields.io/badge/RocketMQ-2.2.3-red?style=flat-square" alt="RocketMQ"/>
</p>

<h1 align="center">懋懋电影 · 智能体平台</h1>

<p align="center">
  <b>基于大语言模型与 RAG 架构的垂直领域智能体平台</b><br/>
  <sub>不只是电影查询 —— 而是一个能理解模糊语义、维持多轮记忆、执行业务操作的 AI Agent</sub>
</p>

---

## 项目简介

**懋懋电影**是一个将大语言模型（LLM）工程化落地到垂直业务场景的全栈实践项目。它不是简单的"套壳 ChatBot"，而是一个具备 **Function Calling 工具调用**、**RAG 检索增强生成**、**多轮长线记忆持久化** 和 **流式网络传输** 能力的完整智能体系统。

用户可以用自然语言完成端到端的业务闭环：

> *"有没有类似盗梦空间那种烧脑的反转电影？"* → 语义检索（Embedding 向量匹配）→ 推荐结果
>
> *"帮我买第一部"* → Function Calling 调用下单工具 → 余额/支付宝混合支付 → 凭证落库
>
> *"我之前买了哪些？"* → 订单查询工具 → 跨会话记忆回溯

**核心能力矩阵：**

| 能力 | 实现方案 | 关键类 |
|---|---|---|
| 自然语言理解 | LangChain4j `@SystemMessage` + `@UserMessage` | `MovieAssistant` |
| 工具调用（12 个） | `@Tool` 注解 + 非单例实例化 | `MovieAgentTools` |
| 语义检索（RAG） | DashScope Embedding + RediSearch 向量索引 | `MovieVectorIngestionService` |
| 多轮记忆 | `ChatMemoryStore` 接口 + Redis 持久化 | `RedisChatMemoryStore` |
| 流式输出 | SSE `ReadableStream` + UTF-8 字节流缓冲区 | `ChatController.chatStream()` |
| 增量同步 | `@TransactionalEventListener` + `@Async` | `MovieVectorSyncListener` |

---

## 核心技术栈

### 后端架构

| 组件 | 版本 | 职责 |
|---|---|---|
| Spring Boot | 2.7.12 | 应用框架、IoC、AOP |
| MyBatis-Plus | 3.5.3.1 | ORM、分页、自动填充 |
| MySQL | 8.0.28 | 业务数据持久化 |
| Redis Stack | 7.x | Session 存储 + RediSearch 向量引擎 |
| RocketMQ | 2.2.3 | 秒杀系统异步下单（事务消息） |

### AI 编排层

| 组件 | 版本 | 职责 |
|---|---|---|
| LangChain4j | 0.29.0 | Agent 编排、AiServices 代理、Function Calling |
| 小米 mimo-v2.5-pro | OpenAI 兼容协议 | 主推理模型（对话 + 工具决策） |
| 通义千问 text-embedding-v4 | 1024 维 | 文本向量化（语义检索） |
| RedisEmbeddingStore | langchain4j-redis | 基于 RediSearch 的向量存储与 ANN 检索 |

### 前端交互

| 组件 | 用途 |
|---|---|
| 原生 JavaScript + jQuery | 页面交互（无框架依赖） |
| Server-Sent Events (SSE) | 流式传输 AI 逐 token 响应 |
| marked.js | Markdown 实时渲染 |
| GSAP 3.12.5 | 粒子背景动画、页面过渡 |

---

## 核心架构亮点

### 一、动态 Agent 实例化与状态隔离

> 传统 LangChain4j 用 `AiServices.create()` 构建单例 Agent，所有用户共享同一份 `ChatMemory`——这在多用户并发场景下会导致记忆串扰。

本项目采用 **按请求动态构建** 策略：`ChatController.buildAssistant()` 在每次 HTTP 请求中独立实例化 `MovieAgentTools`（非 `@Component`，通过构造函数注入 `userId` 和业务 Service），再通过 `AiServices.builder().tools(tools)` 绑定到 `MovieAssistant` 代理。

```
HTTP Request → ChatController.buildAssistant(userId)
  ├─ new MovieAgentTools(userId, movieService, orderService, ...)  // 实例级隔离
  ├─ AiServices.builder().tools(tools).build(MovieAssistant.class) // 动态代理
  └─ assistant.chatStream(userId, message)                          // TokenStream 回调
```

**关键设计**：`MovieAgentTools` 不是 Spring Bean，而是普通 Java 对象——每个请求持有独立的 `userId`，彻底消除 ThreadLocal 依赖和并发状态污染。

### 二、多轮对话上下文持久化

> LangChain4j 默认的 `InMemoryChatMemoryStore` 在应用重启后丢失全部对话历史，且单机内存无法水平扩展。

本项目实现 `RedisChatMemoryStore`，将 LangChain4j 的多态消息体（`SystemMessage` / `UserMessage` / `AiMessage` / `ToolExecutionResultMessage`）通过 `ChatMessageSerializer` 序列化为 JSON，以 `movie:agent:memory:{userId}` 为 Key 存入 Redis，TTL 7 天。

```
用户发送消息 → ChatMemoryProvider.get(userId)
  ├─ Redis GET movie:agent:memory:{userId}    → 反序列化为 List<ChatMessage>
  ├─ MessageWindowChatMemory（保留最近 20 条） → 注入 LLM 上下文
  └─ LLM 响应后 → Redis SET 序列化后的完整消息列表
```

**效果**：用户关闭浏览器后重新打开，AI 依然记得之前的对话内容——跨 HTTP 请求、跨服务器节点的无缝记忆流转。

### 三、RAG 双轨制同步机制

> MySQL 中的电影数据变更后，Redis 中的向量索引如何保持一致？直接在事务内调 Embedding API 会阻塞主线程，异步调用又可能丢失。

本项目采用 **领域事件 + 异步监听器** 的双轨制：

```
电影数据变更 → Spring 发布 MovieDataChangeEvent
  └─ @TransactionalEventListener(AFTER_COMMIT)  ← 确保 MySQL 事务已提交
      └─ @Async                                  ← 异步线程池执行，不阻塞主线程
          ├─ UPSERT → MovieVectorIngestionService.ingestSingleMovie()
          │           └─ DashScope API 生成 Embedding → RedisEmbeddingStore.add()
          └─ DELETE → MovieVectorIngestionService.removeSingleMovie()
                      └─ RedisTemplate.delete(movie_semantic_index:{movieId})
```

**关键设计**：
- `AFTER_COMMIT` 保证只有 MySQL 真正落盘后才触发向量同步，避免脏读
- `@Async` 将网络 I/O（DashScope API）剥离到独立线程池，不阻塞业务请求
- 增量同步（`ingestSingleMovie`）使用电影 ID 作为向量 Store 的自定义 Key，支持精确覆盖和删除
- 全量重建通过 `/api/chat/init-vector` 端点触发，作为兜底补偿手段

### 四、企业级高并发防护

> 大模型 Function Calling 的响应延迟（3-10 秒）远高于普通 HTTP 接口，用户在等待期间多次点击会导致重复下单。

本项目采用 **分布式锁 + 联合唯一索引** 的双重防护：

```java
// 应用层：Redis setIfAbsent 分布式锁（10 秒 TTL）
Boolean locked = stringRedisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

// 数据层：MySQL 联合唯一索引 uk_user_movie_status
// + DuplicateKeyException 幂等捕获
```

**防护链路**：
1. **第一层**：Redis `setIfAbsent` —— 挡住 99% 的并发重复请求
2. **第二层**：MySQL `uk_user_movie_status` 联合唯一索引 —— 兜底极端并发穿透
3. **第三层**：`DuplicateKeyException` 捕获 —— 优雅降级为用户友好提示
4. **凭证表**：`user_movie_entitlement` 独立于订单表，支付成功后写入，作为最终所有权证明

### 五、前端 SSE 流式网络解析优化

> 原生 SSE 在传输中文时，TCP 分包可能将一个 UTF-8 多字节字符（如"懋" = 3 字节）截断到两个 chunk，直接拼接会导致乱码或"吞字"。

本项目重构了前端的 `ReadableStream` 缓冲区逻辑：

```javascript
// 使用 TextDecoder 的 stream 模式，自动处理 UTF-8 跨 chunk 截断
const reader = response.body.getReader();
const decoder = new TextDecoder('utf-8', { stream: true });

while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    // stream: true 模式下，TextDecoder 会缓存不完整的字节序列
    // 在下一个 chunk 到达时自动拼接解码
    const chunk = decoder.decode(value, { stream: true });
    processSSEChunk(chunk);  // 解析 SSE 协议格式 → marked.js 渲染
}
```

**效果**：中文输出不再出现乱码、吞字、Markdown 表格渲染断裂等问题。

---

## 系统架构全景

```
┌─────────────────────────────────────────────────────────────────┐
│                        浏览器 (Thymeleaf + 原生 JS)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │ 电影浏览  │  │ 订单支付  │  │ VIP 升级  │  │ AI 助手 (SSE 流式)│ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬──────────┘ │
└───────┼──────────────┼──────────────┼────────────────┼──────────┘
        │              │              │                │
        ▼              ▼              ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Controller 层 (13 个 Controller)                        │   │
│  │  ├─ ChatController      → /api/chat, /api/chat/stream   │   │
│  │  ├─ OrderController     → /api/order/*                  │   │
│  │  ├─ MovieController     → /api/movie/*                  │   │
│  │  └─ PaymentController   → /payment/* (支付宝回调)        │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Service 层                                              │   │
│  │  ├─ MovieAssistant (LangChain4j AiServices 代理)         │   │
│  │  ├─ MovieAgentTools (12 个 @Tool 方法)                   │   │
│  │  ├─ MovieVectorIngestionService (向量全量/增量同步)       │   │
│  │  ├─ OrderServiceImpl (分布式锁 + 幂等下单)               │   │
│  │  └─ AlipayServiceImpl (支付创建 + 异步回调 + 签名验证)    │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  基础设施层                                               │   │
│  │  ├─ RedisChatMemoryStore   (对话记忆持久化)              │   │
│  │  ├─ MovieVectorSyncListener (@Async + @TransactionalEL)  │   │
│  │  ├─ LoginInterceptor       (Session → UserContext)       │   │
│  │  └─ OrderScheduler         (定时取消超时订单)             │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────┬───────────────────┘
                       │                      │
              ┌────────▼────────┐    ┌────────▼────────┐
              │   MySQL 8.0     │    │  Redis Stack    │
              │  ├─ movie       │    │  ├─ Session     │
              │  ├─ user        │    │  ├─ ChatMemory  │
              │  ├─ order_info  │    │  ├─ 分布式锁     │
              │  ├─ comment     │    │  └─ RediSearch  │
              │  ├─ favorite    │    │     向量索引     │
              │  └─ user_movie  │    │     (1024 维)   │
              │    _entitlement │    └─────────────────┘
              └─────────────────┘
                       │
              ┌────────▼────────────────────┐
              │      外部 API                │
              │  ├─ 小米 mimo-v2.5-pro      │
              │  │  (对话 + 工具决策)         │
              │  ├─ 通义千问 text-embedding  │
              │  │  (文本向量化)              │
              │  └─ 支付宝沙箱               │
              │     (电脑网站支付)           │
              └─────────────────────────────┘
```

---

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.6+
- Docker（用于 Redis Stack）

### 1. 启动 Redis Stack

```bash
docker run -d --name redis-stack \
  -p 6379:6379 \
  -p 8001:8001 \
  redis/redis-stack:latest
```

> Redis Stack 内置 RediSearch 模块，提供向量存储与 ANN 检索能力。`8001` 端口为 Redis Insight 可视化界面。

### 2. 初始化 MySQL 数据库

```sql
CREATE DATABASE movie_db DEFAULT CHARACTER SET utf8mb4;
```

执行建表脚本：`src/main/resources/movie.sql`

### 3. 配置应用参数

编辑 `src/main/resources/application.properties`：

```properties
# 数据库
spring.datasource.url=jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.password=123456

# 大模型 API Key（必需）
llm.xiaomi.api-key=${MIMO_API_KEY:}
llm.embedding.api-key=your-dashscope-api-key
```

> **MIMO_API_KEY**：小米大模型 API Key，从环境变量注入。
> **Embedding API Key**：阿里云 DashScope API Key，用于文本向量化。

### 4. 编译启动

```bash
mvn clean compile
mvn spring-boot:run
```

### 5. 初始化向量索引

**首次启动后必须执行**，将数据库中已有电影数据全量向量化：

```bash
curl http://localhost:8080/api/chat/init-vector
```

> 后续电影数据的增删改会通过 `@TransactionalEventListener` 自动增量同步到向量库。

### 6. 访问系统

| 页面 | 地址 | 说明 |
|---|---|---|
| 首页 | `http://localhost:8080/` | 电影浏览、搜索、筛选 |
| AI 助手 | `http://localhost:8080/chat` | 自然语言对话、语义推荐 |
| 排行榜 | `http://localhost:8080/rank/hot` | 热门 / 好评排行 |
| 个人中心 | `http://localhost:8080/profile` | 余额充值、订单管理 |
| VIP 会员 | `http://localhost:8080/vip` | 会员升级 |

---

## 项目结构

```
src/main/java/com/software/movie/
├── common/
│   ├── Result.java                    # 统一响应体
│   ├── ResultCode.java                # 错误码枚举
│   ├── UserContext.java               # ThreadLocal 用户上下文
│   └── event/
│       ├── MovieDataChangeEvent.java  # 领域事件
│       └── MovieVectorSyncListener.java  # 异步向量同步监听器
├── config/
│   ├── LangChainConfig.java           # LLM + Embedding + 向量存储配置
│   ├── RedisChatMemoryStore.java      # 对话记忆 Redis 持久化
│   ├── RedisCacheConfig.java          # Spring Cache + Jackson2Json 序列化
│   ├── WebMvcConfig.java              # 拦截器注册
│   ├── AlipayConfig.java              # 支付宝沙箱配置
│   └── OrderScheduler.java            # 超时订单定时取消
├── controller/
│   ├── ChatController.java            # AI 对话（同步 + SSE 流式）
│   ├── OrderController.java           # 订单 CRUD + 余额/支付宝支付
│   ├── MovieController.java           # 电影查询、筛选
│   ├── PaymentController.java         # 支付宝异步回调
│   └── ...                            # 共 13 个 Controller
├── entity/                            # 7 个实体 + DTO
├── mapper/                            # MyBatis-Plus BaseMapper
├── service/
│   ├── MovieAssistant.java            # LangChain4j AiServices 接口
│   ├── MovieAgentTools.java           # 12 个 @Tool 工具方法
│   ├── MovieVectorIngestionService.java  # 向量全量/增量同步
│   └── impl/                          # Service 实现类
└── interceptor/
    └── LoginInterceptor.java          # Session 鉴权 + UserContext 注入
```

---

## 架构演进规划

> 以下为当前项目主动保留的技术债及解决方向，体现从 MVP 到生产级的演进路径。

### 线程池治理

**现状**：`@Async` 使用 Spring Boot 默认的 `SimpleAsyncTaskExecutor`（每次创建新线程，无上限）。

**规划**：替换为有界队列的 `ThreadPoolTaskExecutor`，配置核心线程数、最大线程数、拒绝策略，并接入 Prometheus 指标监控，防范高并发下的 OOM 风险。

### 跨存储一致性补偿

**现状**：`@TransactionalEventListener(AFTER_COMMIT)` 触发的向量同步采用 fire-and-forget 模式，失败仅打日志。

**规划**：引入 Transactional Outbox（本地消息表）模式，在 MySQL 事务内原子写入 outbox 记录，由独立定时任务扫描执行，支持指数退避重试和死信队列兜底。

### 分布式锁进阶

**现状**：手写 `setIfAbsent` + 手动 `delete`，存在 TTL 过期后锁误删的竞态窗口。

**规划**：升级至 Redisson，利用 WatchDog 看门狗机制自动续期，Lua 脚本原子校验删除，彻底消除锁误删问题。

### 安全防线

**现状**：用户输入直接透传到 LLM，无 Prompt Injection 防护；第三方 API 无熔断降级。

**规划**：
- 入参层增设输入长度限制、敏感关键词过滤
- 引入 Resilience4j CircuitBreaker 为大模型 API 配置熔断降级，避免上游宕机导致全站雪崩
- 关键操作（下单、充值）增加二次确认机制

---

## 技术亮点总结

| 维度 | 传统做法 | 本项目做法 |
|---|---|---|
| Agent 构建 | 单例 + ThreadLocal | 按请求实例化，构造函数注入 userId |
| 对话记忆 | 内存 / 无持久化 | Redis 序列化存储，TTL 7 天 |
| 向量同步 | 同步阻塞 / 定时全量 | `@TransactionalEventListener` + `@Async` 增量 |
| 防重下单 | 仅数据库唯一索引 | Redis 分布式锁 + 唯一索引 + 异常捕获三重防护 |
| SSE 传输 | 直接拼接字符串 | `TextDecoder({stream: true})` 处理 UTF-8 截断 |
| 支付 | 单一支付方式 | 余额 + 支付宝混合支付，支持部分余额抵扣 |

---

## License

MIT License © 2026
