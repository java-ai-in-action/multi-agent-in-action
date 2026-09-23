package com.javaai.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.javaai.agent.state.WorkflowKeys;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executor Agent：调用工具执行子任务。
 *
 * <p><b>踩坑点②</b>：子任务必须能<b>并行</b>执行。串行跑纯属浪费 Token 和时间——
 * 「查订单」和「改地址」是两个独立任务，没理由排队。
 */
@Component
public class ExecutorNode implements NodeAction {

    private final ChatClient chatClient;

    /** 虚拟线程池：IO 密集的 LLM 调用用它最省资源（JDK 21+） */
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ExecutorNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是电商客服的执行助手。根据子任务调用对应工具（订单查询、地址修改、退款等），
                        只输出执行结果本身，不要客套话，不要编造数据。
                        """)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        // 注意：用 data() + 显式转型取列表，避免 value(key, default) 的泛型 checkcast 陷阱
        List<String> tasks = (List<String>) state.data()
                .getOrDefault(WorkflowKeys.TASKS, List.of());

        Map<String, String> results = new LinkedHashMap<>();

        // 每个子任务并行执行
        var futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> Map.entry(task, exec(task)), pool))
                .toList();

        for (var future : futures) {
            var entry = future.join();   // 生产环境请加超时，见踩坑清单第 2 条
            results.put(entry.getKey(), entry.getValue());
        }
        return Map.of(WorkflowKeys.RESULTS, results);
    }

    private String exec(String task) {
        return chatClient.prompt()
                .user(u -> u.text("执行子任务：{task}").param("task", task))
                .call()
                .content();
    }
}
