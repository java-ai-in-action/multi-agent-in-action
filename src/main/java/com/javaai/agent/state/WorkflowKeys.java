package com.javaai.agent.state;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流状态键定义 + 键策略工厂。
 *
 * <p><b>⚠️ 重要认知（也是本仓库踩过的坑）</b>：Spring AI Alibaba Graph 的状态载体是
 * {@link com.alibaba.cloud.ai.graph.OverAllState} —— 一个 <b>Map 的包装</b>，而不是泛型参数。因此：
 * <ul>
 *   <li>{@code StateGraph} / {@code CompiledGraph} 都<b>不是泛型类</b>，不能写 {@code StateGraph<MyState>}</li>
 *   <li>状态读写通过「键」完成：读 {@code state.value(KEY, default)}，写 {@code Map.of(KEY, value)}</li>
 * </ul>
 *
 * <p>这里统一定义所有状态键，避免散落的字符串字面量。
 */
public final class WorkflowKeys {

    /** 用户当前输入 */
    public static final String MESSAGE = "message";
    /** Planner 产出的任务清单 */
    public static final String TASKS = "tasks";
    /** Executor 产出的「任务 → 结果」 */
    public static final String RESULTS = "results";
    /** Reviewer 本轮是否通过 */
    public static final String REVIEW_PASSED = "reviewPassed";
    /** Reviewer 已打回次数 */
    public static final String RETRY_COUNT = "retryCount";
    /** 最终答复 */
    public static final String FINAL_ANSWER = "finalAnswer";

    private WorkflowKeys() {
    }

    /**
     * 键策略工厂：决定同一个键被多次写入时如何合并。
     * <ul>
     *   <li>{@code REPLACE} —— 后写覆盖先写（标量键的常规选择）</li>
     *   <li>{@code APPEND} —— 追加（列表累积场景）</li>
     * </ul>
     */
    public static KeyStrategyFactory keyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(MESSAGE, KeyStrategy.REPLACE);
            strategies.put(TASKS, KeyStrategy.REPLACE);
            strategies.put(RESULTS, KeyStrategy.REPLACE);
            strategies.put(REVIEW_PASSED, KeyStrategy.REPLACE);
            strategies.put(RETRY_COUNT, KeyStrategy.REPLACE);
            strategies.put(FINAL_ANSWER, KeyStrategy.REPLACE);
            return strategies;
        };
    }
}
