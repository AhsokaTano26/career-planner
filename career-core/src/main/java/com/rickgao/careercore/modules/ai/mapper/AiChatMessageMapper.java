package com.rickgao.careercore.modules.ai.mapper;

import com.rickgao.careercore.modules.ai.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 对话消息 Mapper。
 */
@Mapper
public interface AiChatMessageMapper {

    /** 批量插入（同 messageGroup 的 user + assistant 两条）。 */
    void insertBatch(@Param("list") List<AiChatMessage> list);

    /** 分页查询某用户全部消息（最新在前）。 */
    List<AiChatMessage> findByUserId(@Param("userId") String userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    /** 统计某用户消息总数。 */
    long countByUserId(@Param("userId") String userId);

    /** 判断 messageGroup 是否存在（用于反馈校验）。 */
    int existsByMessageGroupAndUserId(@Param("messageGroup") String messageGroup, @Param("userId") String userId);

    /** 取得某用户最新一条 assistant 消息（用于兜底 chatHistory 兼容）。 */
    AiChatMessage findLatestAssistant(@Param("userId") String userId);
}
