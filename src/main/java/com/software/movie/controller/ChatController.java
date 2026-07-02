package com.software.movie.controller;

import com.software.movie.common.Result;
import com.software.movie.common.UserContext;
import com.software.movie.entity.User;
import com.software.movie.entity.dto.ChatRequest;
import com.software.movie.service.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能助手控制器。
 * <p>每次请求动态构建 MovieAssistant 实例，确保用户上下文隔离，消除跨线程泄漏风险。</p>
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired(required = false)
    private OpenAiStreamingChatModel streamingChatModel;

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    private MovieService movieService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FaqService faqService;

    @Autowired
    private FaqVectorIngestionService faqVectorIngestionService;

    @Autowired
    private MovieVectorIngestionService ingestionService;

    @Autowired(required = false)
    private dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @Autowired(required = false)
    private dev.langchain4j.store.embedding.EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore;

    /**
     * 同步对话（等待完整回答后一次性返回）
     */
    @PostMapping
    public Result chat(@RequestBody ChatRequest request) {
        User user = UserContext.getUser();
        if (user == null) return Result.error("请先登录");
        if (streamingChatModel == null) return Result.error("AI 服务未配置，请设置 LLM_API_KEY 环境变量");

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        try {
            MovieAssistant assistant = buildAssistant(user.getId());
            String reply = assistant.chat(user.getId(), request.getMessage());
            return Result.success(reply);
        } catch (Exception e) {
            log.error("AI 对话异常", e);
            return Result.error("智能助手暂时无法回答：" + e.getMessage());
        }
    }

    /**
     * 流式对话（SSE，逐 token 推送给前端）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        User user = UserContext.getUser();
        if (user == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("未登录"));
            return emitter;
        }
        if (streamingChatModel == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("AI 服务未配置，请设置 LLM_API_KEY 环境变量"));
            return emitter;
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("消息不能为空"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        try {
            MovieAssistant assistant = buildAssistant(user.getId());
            TokenStream tokenStream = assistant.chatStream(user.getId(), request.getMessage());

            tokenStream
                .onNext(token -> {
                    try {
                        emitter.send(token);
                    } catch (Exception e) {
                        log.error("SSE 发送 token 失败", e);
                    }
                })
                .onComplete(chatResponse -> {
                    try {
                        emitter.send("\n[DONE]");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 完成信号发送失败", e);
                    }
                })
                .onError(error -> {
                    log.error("AI 流式对话异常", error);
                    try {
                        emitter.send("data:[系统异常，请查看后台日志]\n\n");
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 错误信息发送失败", e);
                    }
                })
                .start();

        } catch (Exception e) {
            log.error("调用大模型 API 失败：{}", e.getMessage(), e);
            try {
                emitter.send("data: 系统当前并发量较大，请稍后再试。\n\n");
                emitter.send("\n[DONE]");
                emitter.complete();
            } catch (Exception ex) {
                log.error("SSE 兜底消息发送失败", ex);
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    /**
     * 手动触发历史电影数据向量化（一次性洗刷）。
     * <p>访问 http://localhost:8080/api/chat/init-vector 即可执行。</p>
     */
    @GetMapping("/init-vector")
    @ResponseBody
    public String initVector() {
        int count = ingestionService.ingestAllMoviesToVectorStore();
        return "向量化数据洗刷完成！共摄入 " + count + " 部电影。";
    }

    /**
     * 手动触发FAQ数据向量化（一次性洗刷）。
     * <p>访问 http://localhost:8080/api/chat/init-faq-vector 即可执行。</p>
     */
    @GetMapping("/init-faq-vector")
    @ResponseBody
    public String initFaqVector() {
        int count = faqVectorIngestionService.ingestAllFaqToVectorStore();
        return "FAQ向量化数据洗刷完成！共摄入 " + count + " 条FAQ。";
    }

    /**
     * 为当前用户构建专属 MovieAssistant 实例。
     * <p>每次请求独立创建，userId 通过构造函数注入工具集，无 ThreadLocal 依赖。</p>
     */
    private MovieAssistant buildAssistant(Long userId) {
        MovieAgentTools userTools = new MovieAgentTools(userId, movieService, orderService, userService, favoriteService, faqService, faqVectorIngestionService, embeddingModel, embeddingStore);
        return AiServices.builder(MovieAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(userTools)
                .build();
    }
}
