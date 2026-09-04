package com.rickgao.careercore.modules.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.config.DatabaseSchemaMigration;
import com.rickgao.careercore.config.DataInitializer;
import com.rickgao.careercore.modules.ai.dto.AiChatFeedbackRequest;
import com.rickgao.careercore.modules.ai.dto.AiChatRequest;
import com.rickgao.careercore.modules.ai.service.AiService;
import com.rickgao.careercore.modules.ai.service.LlmGateway;
import com.rickgao.careercore.modules.ai.vo.AiChatHistoryVO;
import com.rickgao.careercore.modules.ai.vo.AiChatVO;
import com.rickgao.careercore.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LlmGateway llmGateway;

    @MockBean
    private DatabaseSchemaMigration databaseSchemaMigration;

    @MockBean
    private DataInitializer dataInitializer;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedUserCannotReadOrFeedbackAnotherUsersChat() {
        when(llmGateway.generate(any(), anyDouble(), anyInt(), anyString(), anyString(), isNull(), anyString()))
                .thenReturn("仅属于用户 B 的回答");

        authenticate("student-b");
        AiChatRequest request = new AiChatRequest();
        request.setSessionId("session-b");
        request.setQuestion("帮我规划本学期学习");
        AiChatVO chat = aiService.chat(request);

        AiChatHistoryVO ownerHistory = aiService.chatHistory(1, 20, null);
        assertEquals(2, ownerHistory.getTotal());
        assertEquals(List.of("student-b"), jdbcTemplate.queryForList(
                "SELECT DISTINCT user_id FROM ai_chat_message WHERE message_group = ?",
                String.class, chat.getMessageId()));

        authenticate("student-a");
        AiChatHistoryVO otherHistory = aiService.chatHistory(1, 20, null);
        assertEquals(0, otherHistory.getTotal());
        assertEquals(List.of(), otherHistory.getList());

        AiChatFeedbackRequest feedback = new AiChatFeedbackRequest();
        feedback.setFeedbackType("HELPFUL");
        feedback.setComment("不应写入");
        BizException error = assertThrows(BizException.class,
                () -> aiService.chatFeedback(chat.getMessageId(), feedback));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, error.getResultCode());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_chat_feedback WHERE message_group = ?",
                Integer.class, chat.getMessageId()));
    }

    @Test
    void gatewayRejectsMissingInternalTokenBeforeCallingHttpClient() {
        LlmGateway gateway = new LlmGateway(
                "", "http://career-ai:8000", "default", 30, new ObjectMapper(), new com.rickgao.careercore.modules.ai.service.AiCallLogWriter());

        BizException error = assertThrows(BizException.class,
                () -> gateway.generate(List.of(
                        java.util.Map.of("role", "user", "content", "hello")),
                        0.7, 500));

        assertEquals(ResultCode.INTERNAL_ERROR, error.getResultCode());
        assertEquals("AI 网关内部令牌未配置", error.getMessage());
        verify(llmGateway, never()).generate(any(), anyDouble(), anyInt());
    }

    private void authenticate(String userId) {
        LoginUser principal = new LoginUser(
                userId, userId, "STUDENT", "test-jti", LocalDateTime.now().plusHours(1));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
