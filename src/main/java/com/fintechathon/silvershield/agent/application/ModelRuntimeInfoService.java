package com.fintechathon.silvershield.agent.application;

import com.fintechathon.silvershield.agent.provider.BankingModelClient;
import com.fintechathon.silvershield.agent.provider.OfflineDemoBankingModelClient;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ModelRuntimeInfoService {
    private static final Set<String> SELF_QUESTIONS = Set.of(
            "你接入deepseek了吗", "接入deepseek了吗", "你接入了deepseek吗", "你接入deepseek了么",
            "你现在接入deepseek了吗", "你已经接入deepseek了吗", "你接入deepseek了没", "是否接入deepseek",
            "你接入deepseek了吗现在", "你是deepseek吗", "你用的是deepseek吗", "现在用的是deepseek吗",
            "你是什么模型", "你用的什么模型", "你用的是什么模型", "你现在用的是什么模型",
            "当前模型", "当前使用什么模型", "当前接入的模型", "模型接入状态", "查看模型接入状态",
            "你接入大模型了吗", "你接入了大模型吗", "whatmodelareyouusing");

    private final Environment environment;
    private final BankingModelClient client;
    private final ObjectProvider<ChatModel> chatModels;

    public ModelRuntimeInfoService(Environment environment, BankingModelClient client, ObjectProvider<ChatModel> chatModels) {
        this.environment = environment;
        this.client = client;
        this.chatModels = chatModels;
    }

    public boolean isSelfQuestion(String message) {
        return message != null && SELF_QUESTIONS.contains(message.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s？?。！!,，：:]", "").replaceFirst("^(?:请问|想问一下|告诉我)", ""));
    }

    public RuntimeInfo current() {
        if (client instanceof OfflineDemoBankingModelClient) {
            return new RuntimeInfo("OFFLINE_DEMO", "离线演示", "deterministic", false);
        }
        String host;
        try { host = URI.create(environment.getProperty("spring.ai.openai.base-url", "")).getHost(); }
        catch (IllegalArgumentException exception) { host = null; }
        String provider = "api.deepseek.com".equalsIgnoreCase(host) ? "DeepSeek"
                : "api.openai.com".equalsIgnoreCase(host) ? "OpenAI" : "OpenAI-compatible";
        String model = environment.getProperty("spring.ai.openai.chat.options.model", "未配置");
        // Runtime metadata is a public allowlist, never a dump of environment/configuration.
        if (!model.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,79}") || model.startsWith("sk-")) model = "未配置";
        boolean configured = "openai".equals(environment.getProperty("spring.ai.model.chat"))
                && !environment.getProperty("spring.ai.openai.api-key", "").isBlank()
                && chatModels.getIfAvailable() != null && !model.equals("未配置");
        return new RuntimeInfo(configured ? "LIVE_MODEL" : "UNAVAILABLE", provider, model, configured);
    }

    public String answer() {
        RuntimeInfo info = current();
        if (info.mode().equals("OFFLINE_DEMO")) return "当前运行离线演示模式，没有调用外部大模型；银行业务使用虚构沙箱数据。";
        if (!info.configured()) return "当前尚未配置可用的模型服务，不能确认已接入真实大模型。";
        return "当前服务端已配置接入 %s（%s），使用真实模型模式。银行数据来自虚构沙箱，转账及状态变更仍需独立确认。"
                .formatted(info.provider(), info.model());
    }

    public record RuntimeInfo(String mode, String provider, String model, boolean configured) {}
}
