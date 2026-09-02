package com.fintechathon.silvershield.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "silvershield.agent")
public class AgentProperties {

    private int maxToolRounds = 5;
    private int maxToolCallsPerRound = 4;
    private int maxTotalToolCalls = 12;
    private Duration modelTimeout = Duration.ofSeconds(20);
    private Duration toolTimeout = Duration.ofSeconds(5);
    private int maxUserMessageChars = 4000;
    private int maxContextChars = 65536;
    private int maxAnswerChars = 8000;
    private Duration conversationTtl = Duration.ofMinutes(30);
    private int maxConversationMessages = 12;

    public int getMaxToolRounds() { return maxToolRounds; }
    public void setMaxToolRounds(int value) { this.maxToolRounds = value; }
    public int getMaxToolCallsPerRound() { return maxToolCallsPerRound; }
    public void setMaxToolCallsPerRound(int value) { this.maxToolCallsPerRound = value; }
    public int getMaxTotalToolCalls() { return maxTotalToolCalls; }
    public void setMaxTotalToolCalls(int value) { this.maxTotalToolCalls = value; }
    public Duration getModelTimeout() { return modelTimeout; }
    public void setModelTimeout(Duration value) { this.modelTimeout = value; }
    public Duration getToolTimeout() { return toolTimeout; }
    public void setToolTimeout(Duration value) { this.toolTimeout = value; }
    public int getMaxUserMessageChars() { return maxUserMessageChars; }
    public void setMaxUserMessageChars(int value) { this.maxUserMessageChars = value; }
    public int getMaxContextChars() { return maxContextChars; }
    public void setMaxContextChars(int value) { this.maxContextChars = value; }
    public int getMaxAnswerChars() { return maxAnswerChars; }
    public void setMaxAnswerChars(int value) { this.maxAnswerChars = value; }
    public Duration getConversationTtl() { return conversationTtl; }
    public void setConversationTtl(Duration value) { this.conversationTtl = value; }
    public int getMaxConversationMessages() { return maxConversationMessages; }
    public void setMaxConversationMessages(int value) { this.maxConversationMessages = value; }
}
