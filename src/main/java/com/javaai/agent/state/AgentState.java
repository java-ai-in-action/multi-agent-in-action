package com.javaai.agent.state;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图状态载体 —— <b>严格分层</b>，这是长任务不炸上下文的关键。
 *
 * <pre>
 * 短期：currentMessages  —— 当前这一轮对话（原始、完整）
 * 长期：historySummary   —— LLM 压缩后的历史摘要（只喂摘要，不喂全量历史）
 * 中间：tasks / results / retryCount / reviewPassed
 * </pre>
 *
 * <p>注意：把它直接全量塞进 Prompt，就是「Token 爆炸 + 延迟飙升 + 噪声干扰」三连。
 */
public class AgentState {

    // ---- 状态 key（图编排时用于读写状态）----
    public static final String CURRENT_MESSAGES = "currentMessages";
    public static final String HISTORY_SUMMARY  = "historySummary";
    public static final String TASKS            = "tasks";
    public static final String RESULTS          = "results";
    public static final String RETRY_COUNT      = "retryCount";
    public static final String REVIEW_PASSED    = "reviewPassed";
    public static final String FINAL_ANSWER     = "finalAnswer";

    /** 短期：当前轮对话 */
    private List<String> currentMessages = new ArrayList<>();

    /** 长期：历史摘要（由 Summarizer 或独立流程定期压缩生成）*/
    private String historySummary = "";

    /** 中间：Planner 产出的任务清单 */
    private List<String> tasks = new ArrayList<>();

    /** 中间：Executor 产出的「任务 → 结果」 */
    private Map<String, String> results = new LinkedHashMap<>();

    /** 中间：Reviewer 已打回次数（重试硬上限的计数依据）*/
    private int retryCount = 0;

    /** 中间：本轮审核是否通过 */
    private boolean reviewPassed = false;

    /** 最终面向用户的答复 */
    private String finalAnswer = "";

    /** 只把 historySummary + 当前轮 喂给模型，而不是全部历史 */
    public List<String> toLlmContext() {
        List<String> ctx = new ArrayList<>();
        if (!historySummary.isBlank()) {
            ctx.add("【历史摘要】" + historySummary);
        }
        ctx.addAll(currentMessages);
        return ctx;
    }

    public String latestUserMessage() {
        return currentMessages.isEmpty() ? "" : currentMessages.get(currentMessages.size() - 1);
    }

    // ---------- getters / setters ----------

    public List<String> getCurrentMessages() { return currentMessages; }
    public void setCurrentMessages(List<String> currentMessages) { this.currentMessages = currentMessages; }

    public String getHistorySummary() { return historySummary; }
    public void setHistorySummary(String historySummary) { this.historySummary = historySummary; }

    public List<String> getTasks() { return tasks; }
    public void setTasks(List<String> tasks) { this.tasks = tasks; }

    public Map<String, String> getResults() { return results; }
    public void setResults(Map<String, String> results) { this.results = results; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public boolean isReviewPassed() { return reviewPassed; }
    public void setReviewPassed(boolean reviewPassed) { this.reviewPassed = reviewPassed; }

    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
}
