package com.rickgao.careercore.modules.ai.mapper;

import com.rickgao.careercore.modules.ai.entity.AiChatFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI 对话反馈 Mapper。
 * 写入策略：唯一键 uk_message_group，重复插入即覆盖（UPSERT 语义）。
 */
@Mapper
public interface AiChatFeedbackMapper {

    /** 写入或覆盖（同一 messageGroup 唯一）。 */
    void upsert(AiChatFeedback feedback);

    /** 兜底反馈：写入最新 assistant 消息（service 层先查最新 messageGroup 再调本方法）。 */
    void insert(AiChatFeedback feedback);
}
