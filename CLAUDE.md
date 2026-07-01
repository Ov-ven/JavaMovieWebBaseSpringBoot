# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

电影综合服务平台 — Spring Boot 2.7.12 + MyBatis-Plus + Redis + Thymeleaf 的全栈 Web 应用。支持电影浏览、评论收藏、VIP 会员、支付宝沙箱支付、Redis+MQ 秒杀系统。

## Build & Run

```bash
# 编译（JDK 17+，实际环境 JDK 21）
mvn clean compile

# 启动（端口 8080，依赖 MySQL + Redis）
mvn spring-boot:run

# 打包
mvn clean package -DskipTests
```

**外部依赖**：
- MySQL `movie_db`（建表 SQL：`src/main/resources/movie.sql`）
- Redis（Session 存储 + 秒杀库存）
- RocketMQ（秒杀异步下单，可选 — 未安装时注释掉 `application.properties` 中的 rocketmq 配置）
- natapp 内网穿透（支付宝异步回调 notifyUrl，`D:\natapp\`）

**数据库密码**：`application.properties` 中 `spring.datasource.password=123456`

## Architecture

```
基础包：com.software.movie

common/          Result 统一响应体、ResultCode 枚举、UserContext (ThreadLocal)、全局异常处理
config/          AlipayConfig、RedisCacheConfig (Jackson2Json)、WebMvcConfig (拦截器)、MyMetaObjectHandler
controller/      13 个 Controller（页面用 @Controller + Thymeleaf，API 用 @RestController）
entity/          7 个实体 + 1 个 DTO，全部 @TableId(type=IdType.AUTO)
mapper/          继承 BaseMapper<T>，@Mapper 注解
service/         继承 IService<T>，实现类继承 ServiceImpl<M, T>
interceptor/     LoginInterceptor（Session → UserContext）
```

### 关键设计决策

- **鉴权**：Spring Session + Redis，Controller 通过 `UserContext.getUser()` 获取当前用户（ThreadLocal，由 LoginInterceptor 注入）
- **VIP 机制**：User 表 `is_vip` 字段 + `vip_expire_time`，Movie 表 `is_vip` 标记 VIP 专享内容
- **缓存**：RedisCacheConfig 配置 Jackson2Json 序列化，部分 Service 使用 `@Cacheable`，部分手动操作 RedisTemplate
- **支付**：支付宝电脑网站支付（alipay.trade.page.pay），沙箱环境，notifyUrl 需要公网域名（natapp）
- **秒杀**：Redis Lua 脚本原子扣减库存 → RocketMQ 事务消息 → 消费者异步落库

### 拦截器白名单

`WebMvcConfig` 中 `LoginInterceptor` 排除了以下路径（无需登录）：
- 静态资源：`/css/**`、`/js/**`、`/image/**`
- 公开页面：`/`、`/login`、`/register`、`/rank/**`、`/person/**`、`/detail/**`、`/vip/**`、`/charts`
- 公开 API：`/api/user/login`、`/api/user/register`、`/api/user/checkUsername`、`/api/movie/**`、`/api/comment/list`
- 外部回调：`/payment/notify`

### 订单类型

| orderType | 含义 |
|---|---|
| 1 | 单片购买 |
| 2 | VIP 购买 |
| 3 | 秒杀订单（代码中与 VIP 购买存在冲突，见已知问题） |

## Known Issues

- `User.isvip` 字段命名不符合驼峰规范（应为 `isVip`），与 `Movie.isVip` 不一致
- `Movie`/`User` 实体同时存在 `@Data` 和手写 getter/setter，可能冲突
- `Result.error(String msg)` 未设置 code 字段，前端拆箱可能 NPE
- `MovieServiceImpl.getMovieDetail()` 会缓存 null 值，导致新电影永远查不到
- `SecKillOrderConsumer` 的 orderType=2 与 VIP 购买冲突，应改为 3
- `MovieController`/`MovieDetailController` 中 Integer 使用 `==` 比较，超出 -128~127 范围会逻辑错误
- `AlipayServiceImpl` 中 VIP 升级不在事务内，可能数据不一致
- 多处残留 `System.out.println` 调试代码

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **JavaMovieWebBaseSpringBoot** (2525 symbols, 5632 relationships, 218 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/JavaMovieWebBaseSpringBoot/context` | Codebase overview, check index freshness |
| `gitnexus://repo/JavaMovieWebBaseSpringBoot/clusters` | All functional areas |
| `gitnexus://repo/JavaMovieWebBaseSpringBoot/processes` | All execution flows |
| `gitnexus://repo/JavaMovieWebBaseSpringBoot/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
