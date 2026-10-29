package com.fintechathon.silvershield.agent.planning;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative demo intent parsing; authorization still belongs to the policy and gateway. */
public final class SubscriptionIntent {
    private static final Pattern ACTION = Pattern.compile(
            "^(?:(?:请|帮我|麻烦|我想|我要|现在|立即|顺便|再|同时|把|希望|准备|please)\\s*)*"
                    + "(?:取消|退订|关闭|停止|cancel|unsubscribe|disable).+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVICE_OR_NEGATION = Pattern.compile(
            "找出|哪些|哪个|是否|能否|可以|可取消|建议|推荐|不要|别取消|不想|不需要|暂不|不取消|"
                    + "do not|don't|which|recommend|can i|could i", Pattern.CASE_INSENSITIVE);

    private SubscriptionIntent() {}

    public static boolean relatesToSubscription(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        return Pattern.compile("订阅|代扣|会员|套餐|云盘|月卡|subscription|direct debit").matcher(text).find();
    }

    public static boolean requestsCancellation(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        if (!relatesToSubscription(text)) return false;
        for (String clause : text.split("[。！？!?；;，,]|然后|并且|并(?=取消|关闭|停止|退订)|再(?=取消|关闭|停止|退订)|and then")) {
            String command = clause.trim();
            if (ACTION.matcher(command).matches() && !ADVICE_OR_NEGATION.matcher(command).find()) return true;
        }
        return false;
    }
}
