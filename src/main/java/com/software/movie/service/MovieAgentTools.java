package com.software.movie.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.software.movie.entity.Movie;
import com.software.movie.entity.Order;
import com.software.movie.entity.User;
import com.software.movie.entity.dto.MovieQueryDTO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 电影智能助手工具集（多实例，非单例）。
 * <p>每个请求创建独立实例，通过构造函数注入当前用户ID，彻底消除跨线程上下文泄漏风险。</p>
 */
public class MovieAgentTools {

    private final Long userId;
    private final MovieService movieService;
    private final OrderService orderService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 全参构造函数，由 Controller 在每次请求时动态装配。
     *
     * @param userId          当前登录用户ID
     * @param movieService    电影服务
     * @param orderService    订单服务
     * @param userService     用户服务
     * @param favoriteService 收藏服务
     * @param embeddingModel  文本向量化模型
     * @param embeddingStore  向量存储
     */
    public MovieAgentTools(Long userId, MovieService movieService,
                           OrderService orderService, UserService userService,
                           FavoriteService favoriteService,
                           EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore) {
        this.userId = userId;
        this.movieService = movieService;
        this.orderService = orderService;
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Tool("当用户需要根据地区、类型、是否免费、评分等条件查找或推荐电影时，调用此工具。" +
          "所有参数均可选，不传或传空字符串则不限制对应条件。返回符合条件的电影列表。")
    public String searchMovies(
            @P("电影类型，如：科幻、动作、喜剧、剧情，可选，不填传空字符串") String type,
            @P("电影地区，如：美国、中国大陆、日本，可选，不填传空字符串") String region,
            @P("关键词，模糊匹配电影标题和描述，可选，不填传空字符串") String keyword,
            @P("排序方式：hot=最热, top=最高分, new=最新，可选，不填传空字符串") String sort,
            @P("是否只查VIP电影，填1表示是，不填传空字符串") String isVip,
            @P("是否只查免费电影，填1表示是，不填传空字符串") String free) {

        MovieQueryDTO queryDTO = new MovieQueryDTO();
        queryDTO.setType(isBlank(type) ? null : type);
        queryDTO.setRegion(isBlank(region) ? null : region);
        queryDTO.setKeyword(isBlank(keyword) ? null : keyword);
        queryDTO.setSort(isBlank(sort) ? null : sort);
        queryDTO.setIsVip(isBlank(isVip) ? null : Integer.parseInt(isVip.trim()));
        queryDTO.setFree(isBlank(free) ? null : Integer.parseInt(free.trim()));
        queryDTO.setPageSize(10);

        IPage<Movie> page = movieService.getMoviePage(queryDTO);
        if (page == null || page.getRecords().isEmpty()) {
            return "未找到符合条件的电影。";
        }

        return page.getRecords().stream()
                .map(m -> String.format("【%d】%s | 类型：%s | 地区：%s | 评分：%.1f | %s",
                        m.getId(), m.getTitle(),
                        m.getType() != null ? m.getType() : "-",
                        m.getRegion() != null ? m.getRegion() : "-",
                        m.getScore() != null ? m.getScore() : 0.0,
                        m.getIsVip() == 1 ? "VIP专享" : "免费"))
                .collect(Collectors.joining("\n"));
    }

    @Tool("当用户明确表示要购买、下单或支付某部电影时，调用此工具。必须传入电影的ID（movieId）。" +
          "返回下单结果，如果余额充足则直接扣款成功，如果余额不足则返回支付宝支付链接。")
    public String purchaseMovie(@P("电影的ID") Long movieId) {
        try {
            Order order = orderService.createOrder(userId, movieId);
            User user = userService.getById(userId);
            double balance = user.getBalance() == null ? 0 : user.getBalance();
            double amount = order.getAmount();

            if (balance >= amount) {
                userService.deductBalance(userId, amount);
                orderService.payOrder(order.getOrderNo(), 0, "BALANCE");
                return String.format("下单成功！订单号：%s，金额：¥%.2f，已自动使用余额抵扣全款，您可以直接观看了。",
                        order.getOrderNo(), amount);
            }

            return String.format("订单已创建，订单号：%s，金额：¥%.2f，但您的余额（¥%.2f）不足以支付。" +
                            "请点击 <a href='/pay?orderNo=%s' target='_blank'>此处打开收银台</a> 完成支付。",
                    order.getOrderNo(), amount, balance, order.getOrderNo());
        } catch (Exception e) {
            return "下单失败：" + e.getMessage();
        }
    }

    @Tool("当用户想要查询自己的历史订单、购买记录，或者询问有哪些待支付/已取消的订单时，调用此工具。" +
          "status参数可选：0=待支付, 1=已支付, 2=已取消，不填或传空字符串则查询所有订单。")
    public String queryUserOrders(
            @P("订单状态，可选值为：0-待支付, 1-已支付, 2-已取消。如果用户未明确指定状态，请传空字符串查询所有") String status) {
        Integer statusInt = isBlank(status) ? null : Integer.parseInt(status.trim());
        List<Order> orders = orderService.getOrdersByUserId(userId, statusInt);

        if (orders.isEmpty()) {
            return "暂无订单记录。";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] statusNames = {"待支付", "已支付", "已取消"};

        return orders.stream()
                .map(o -> {
                    String statusText = (o.getStatus() >= 0 && o.getStatus() <= 2)
                            ? statusNames[o.getStatus()] : "未知";
                    return String.format("订单号：%s | 金额：¥%.2f | 状态：%s | 类型：%s | 时间：%s",
                            o.getOrderNo(),
                            o.getAmount() != null ? o.getAmount() : 0.0,
                            statusText,
                            o.getOrderType() == 1 ? "单片购买" : o.getOrderType() == 2 ? "VIP购买" : "秒杀",
                            o.getCreateTime() != null ? sdf.format(o.getCreateTime()) : "-");
                })
                .collect(Collectors.joining("\n"));
    }

    @Tool("当用户明确要求取消某笔未支付的订单时，调用此工具。必须从上下文中提取目标订单的orderNo传入。" +
          "仅待支付状态的订单可以取消。")
    public String cancelOrder(@P("需要取消的订单号，必须是精确的字符串") String orderNo) {
        if (isBlank(orderNo)) {
            return "请提供需要取消的订单号。";
        }
        return orderService.cancelOrder(userId, orderNo.trim());
    }

    /**
     * 继续支付待支付订单（余额优先扣款）。
     *
     * @param orderNo 待支付的订单号
     * @return 支付结果文本
     */
    @Tool("当用户要求继续支付、付款某笔待支付订单时，调用此工具。必须传入订单号orderNo。" +
          "优先使用余额支付，余额不足时返回补差链接。")
    public String payPendingOrder(@P("待支付的订单号，必须是精确的字符串") String orderNo) {
        if (isBlank(orderNo)) {
            return "请提供需要支付的订单号。";
        }
        return orderService.payOrderWithBalance(userId, orderNo.trim());
    }

    /**
     * 查询当前用户的账户余额。
     *
     * @return 余额信息文本
     */
    @Tool("当用户询问自己的余额、账户余额、还剩多少钱时，调用此工具。无需传参。")
    public String checkBalance() {
        User user = userService.getById(userId);
        double balance = user.getBalance() == null ? 0 : user.getBalance();
        return String.format("您的账户余额为：¥%.2f", balance);
    }

    /**
     * 为当前用户充值余额。
     *
     * @param amount 充值金额（正数）
     * @return 充值结果文本
     */
    @Tool("当用户要求充值、给账户加钱时，调用此工具。必须传入充值金额。充值直接到账，无需真实支付。")
    public String rechargeBalance(@P("充值金额，必须是大于0的数字") String amount) {
        if (isBlank(amount)) return "请提供充值金额。";
        double amountVal;
        try {
            amountVal = Double.parseDouble(amount.trim());
        } catch (NumberFormatException e) {
            return "金额格式不正确，请输入数字。";
        }
        if (amountVal <= 0) return "充值金额必须大于0。";

        boolean success = userService.recharge(userId, amountVal);
        if (success) {
            User user = userService.getById(userId);
            return String.format("✅ 充值成功！已充值 ¥%.2f，当前余额：¥%.2f", amountVal,
                    user.getBalance() == null ? 0 : user.getBalance());
        }
        return "❌ 充值失败，请稍后重试。";
    }

    /**
     * 查询当前用户的 VIP 状态。
     *
     * @return VIP 状态信息文本
     */
    @Tool("当用户询问自己是否是VIP、VIP什么时候到期、会员状态时，调用此工具。无需传参。")
    public String checkVipStatus() {
        User user = userService.getById(userId);
        if (user.getIsvip() == 1) {
            String expireStr = user.getVipExpireTime() != null
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(user.getVipExpireTime())
                    : "未知";
            return String.format("✅ 您是VIP会员，到期时间：%s", expireStr);
        }
        return "您当前不是VIP会员。如需升级，可以前往 VIP 升级页面，或告诉我您想开通的套餐（月度/季度/年度）。";
    }

    /**
     * 获取电影详情。
     *
     * @param movieId 电影ID
     * @return 电影详情文本
     */
    @Tool("当用户询问某部电影的详细信息、剧情简介、演员表时，调用此工具。必须传入电影ID。")
    public String getMovieDetail(@P("电影的ID") Long movieId) {
        Movie movie = movieService.getById(movieId);
        if (movie == null) {
            return "未找到该电影。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎬 %s\n", movie.getTitle()));
        sb.append(String.format("类型：%s | 地区：%s\n", movie.getType(), movie.getRegion()));
        sb.append(String.format("评分：%.1f | 时长：%d分钟\n", movie.getScore(), movie.getDuration()));
        sb.append(String.format("上映日期：%s\n", movie.getReleaseDate()));
        sb.append(String.format("简介：%s\n", movie.getDescription()));
        if (movie.getIsVip() == 1) {
            sb.append("⭐ VIP专享\n");
        }
        if (movie.getPrice() != null && movie.getPrice() > 0) {
            sb.append(String.format("💰 价格：¥%.2f", movie.getPrice()));
        }
        return sb.toString();
    }

    /**
     * 收藏电影。
     *
     * @param movieId 电影ID
     * @return 操作结果文本
     */
    @Tool("当用户要求收藏、保存某部电影时，调用此工具。必须传入电影ID。")
    public String addFavorite(@P("要收藏的电影ID") Long movieId) {
        boolean success = favoriteService.addFavorite(userId, movieId);
        if (success) {
            Movie movie = movieService.getById(movieId);
            String title = movie != null ? movie.getTitle() : "电影";
            return "✅ 已收藏《" + title + "》";
        }
        return "❌ 收藏失败，请稍后重试。";
    }

    /**
     * 查看收藏列表。
     *
     * @return 收藏的电影列表文本
     */
    @Tool("当用户询问自己收藏了哪些电影、收藏列表时，调用此工具。无需传参。")
    public String getFavorites() {
        List<Movie> movies = favoriteService.getFavoriteMovies(userId);
        if (movies.isEmpty()) {
            return "您还没有收藏任何电影。";
        }
        return movies.stream()
                .map(m -> String.format("【%d】%s | 评分：%.1f%s",
                        m.getId(), m.getTitle(),
                        m.getScore() != null ? m.getScore() : 0.0,
                        m.getIsVip() == 1 ? " | VIP专享" : ""))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 语义搜索电影（基于向量相似度）。
     * <p>当用户使用描述性、感受性或剧情向的语言找电影时调用此工具。</p>
     *
     * @param requirement 用户口语化的模糊需求
     * @return 匹配的电影列表文本
     */
    @Tool("当用户没有提供明确的电影名称、地区或类型，而是描述剧情、氛围、感受或类似某部电影的模糊需求时，调用此工具进行语义搜索。" +
          "例如：'想看主角在密室里互相猜忌的反转片'、'有没有催泪感人的动作片'、'类似盗梦空间的烧脑电影'。")
    public String semanticSearchMovies(
            @P("用户口语化的模糊需求、剧情描述或情感偏好") String requirement) {

        if (isBlank(requirement)) {
            return "请描述您想找的电影类型或剧情偏好。";
        }

        try {
            // 1. 将用户需求转换为向量
            Response<Embedding> response = embeddingModel.embed(TextSegment.from(requirement));
            Embedding embedding = response.content();

            // 2. 向量相似度检索（取 Top 3）
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(embedding, 3);

            if (matches == null || matches.isEmpty()) {
                return "暂未找到与您需求匹配的电影，试试换个描述方式？";
            }

            // 3. 从匹配结果的自定义 ID 中提取 movieId，查询完整电影信息
            StringBuilder result = new StringBuilder("为您找到以下匹配电影：\n\n");
            int index = 1;

            for (EmbeddingMatch<TextSegment> match : matches) {
                // 自定义 ID 就是 movieId（在 ingestAllMoviesToVectorStore 中设置）
                String movieIdStr = match.embeddingId();
                if (movieIdStr == null || movieIdStr.isEmpty()) continue;

                Long movieId;
                try {
                    movieId = Long.parseLong(movieIdStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                Movie movie = movieService.getById(movieId);
                if (movie == null) continue;

                result.append(String.format("**%d. %s**\n", index++, movie.getTitle()));
                result.append(String.format("类型：%s | 地区：%s | 评分：%.1f\n",
                        movie.getType() != null ? movie.getType() : "-",
                        movie.getRegion() != null ? movie.getRegion() : "-",
                        movie.getScore() != null ? movie.getScore() : 0.0));
                if (movie.getIsVip() == 1) {
                    result.append("⭐ VIP专享\n");
                }
                if (movie.getDescription() != null && !movie.getDescription().isEmpty()) {
                    String desc = movie.getDescription().length() > 80
                            ? movie.getDescription().substring(0, 80) + "..."
                            : movie.getDescription();
                    result.append(desc).append("\n");
                }
                result.append("\n");
            }

            return result.toString().trim();

        } catch (Exception e) {
            return "语义搜索失败：" + e.getMessage();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty() || s.trim().equals("\"\"") || s.trim().equals("null");
    }
}
