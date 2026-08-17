package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.RelationRequest;
import com.rickgao.careercore.modules.admin.mapper.AdminRelationMapper;
import com.rickgao.careercore.modules.admin.service.AdminRelationService;
import com.rickgao.careercore.modules.admin.vo.AdvisorRelationVO;
import com.rickgao.careercore.modules.advisor.entity.AdvisorStudentRelation;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminRelationServiceImplTest {

    private final AdminRelationMapper adminRelationMapper = mock(AdminRelationMapper.class);
    private final AdvisorStudentRelationMapper advisorRelationMapper = mock(AdvisorStudentRelationMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminRelationService service = new AdminRelationServiceImpl(
            adminRelationMapper, advisorRelationMapper, sysUserMapper, idGenerator, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idGenerator.adminRelationId()).thenReturn("REL-100");
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listRelations_mapsPage() {
        AdvisorRelationVO vo = new AdvisorRelationVO();
        vo.setId("AR-001");
        vo.setAdvisorId("A1001");
        vo.setStudentId("S1001");
        when(adminRelationMapper.countRelations("A1001")).thenReturn(6L);
        when(adminRelationMapper.selectRelationPage("A1001", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));

        PageResult<AdvisorRelationVO> result = service.listRelations("A1001", 1, 20, null);

        assertEquals(6, result.getTotal());
        assertEquals("AR-001", result.getList().get(0).getId());
    }

    @Test
    void createRelations_advisorNotFound_throws404() {
        when(sysUserMapper.findById("A9999")).thenReturn(null);
        RelationRequest request = request("A9999", "S1001");
        BizException ex = assertThrows(BizException.class,
                () -> service.createRelations("ADMIN1", "/relations", "k1", request));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void createRelations_advisorRoleMismatch_throwsValidation() {
        when(sysUserMapper.findById("A1001")).thenReturn(user("A1001", "ADMIN", "管理员"));
        BizException ex = assertThrows(BizException.class,
                () -> service.createRelations("ADMIN1", "/relations", "k1", request("A1001", "S1001")));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createRelations_invalidStudent_throwsValidation() {
        when(sysUserMapper.findById("A1001")).thenReturn(user("A1001", "ADVISOR", "张老师"));
        when(sysUserMapper.findById("S9999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.createRelations("ADMIN1", "/relations", "k1", request("A1001", "S9999")));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createRelations_skipsExistingAndCreatesNew() {
        when(sysUserMapper.findById("A1001")).thenReturn(user("A1001", "ADVISOR", "张老师"));
        when(sysUserMapper.findById("S1001")).thenReturn(user("S1001", "STUDENT", "李明"));
        when(sysUserMapper.findById("S1002")).thenReturn(user("S1002", "STUDENT", "张雨"));
        when(advisorRelationMapper.findStudentIdsByAdvisor("A1001"))
                .thenReturn(List.of("S1001"));

        List<AdvisorRelationVO> created = service.createRelations(
                "ADMIN1", "/relations", "k1", request("A1001", "S1001", "S1002"));

        assertEquals(1, created.size());
        assertEquals("S1002", created.get(0).getStudentId());
        assertEquals("张雨", created.get(0).getStudentName());
        ArgumentCaptor<AdvisorStudentRelation> captor = ArgumentCaptor.forClass(AdvisorStudentRelation.class);
        verify(adminRelationMapper).insert(captor.capture());
        assertEquals("REL-100", captor.getValue().getId());
        assertEquals("S1002", captor.getValue().getStudentId());
    }

    @Test
    void createRelations_restoresSoftDeletedRelation() {
        when(sysUserMapper.findById("A1001")).thenReturn(user("A1001", "ADVISOR", "张老师"));
        when(sysUserMapper.findById("S1005")).thenReturn(user("S1005", "STUDENT", "陈晨"));
        when(advisorRelationMapper.findStudentIdsByAdvisor("A1001"))
                .thenReturn(List.of("S1001", "S1002", "S1003", "S1004", "S1006"));
        AdvisorStudentRelation deleted = new AdvisorStudentRelation();
        deleted.setId("REL-100");
        deleted.setAdvisorId("A1001");
        deleted.setStudentId("S1005");
        deleted.setDeleted(1);
        when(adminRelationMapper.findByAdvisorAndStudentIncludingDeleted("A1001", "S1005"))
                .thenReturn(deleted);

        List<AdvisorRelationVO> created = service.createRelations(
                "ADMIN1", "/relations", "k1", request("A1001", "S1005"));

        assertEquals(1, created.size());
        assertEquals("REL-100", created.get(0).getId());
        verify(adminRelationMapper).restoreById("REL-100");
        verify(adminRelationMapper, never()).insert(any(AdvisorStudentRelation.class));
    }

    @Test
    void createRelations_overLimit_throwsValidation() {
        when(sysUserMapper.findById("A1001")).thenReturn(user("A1001", "ADVISOR", "张老师"));
        when(advisorRelationMapper.findStudentIdsByAdvisor("A1001")).thenReturn(Collections.emptyList());
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) {
            ids.add("S" + i);
        }
        BizException ex = assertThrows(BizException.class,
                () -> service.createRelations("ADMIN1", "/relations", "k1", request("A1001", ids)));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void deleteRelation_notFound_throws404() {
        when(adminRelationMapper.findById("REL-999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.deleteRelation("ADMIN1", "/relations/{relationId}", "k1", "REL-999"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
        verify(adminRelationMapper, never()).softDeleteById(anyString());
    }

    @Test
    void deleteRelation_softDeletes() {
        AdvisorStudentRelation relation = new AdvisorStudentRelation();
        relation.setId("REL-100");
        when(adminRelationMapper.findById("REL-100")).thenReturn(relation);
        service.deleteRelation("ADMIN1", "/relations/{relationId}", "k1", "REL-100");
        verify(adminRelationMapper).softDeleteById("REL-100");
    }

    private RelationRequest request(String advisorId, String... studentIds) {
        RelationRequest request = new RelationRequest();
        request.setAdvisorId(advisorId);
        request.setStudentIds(List.of(studentIds));
        return request;
    }

    private RelationRequest request(String advisorId, List<String> studentIds) {
        RelationRequest request = new RelationRequest();
        request.setAdvisorId(advisorId);
        request.setStudentIds(studentIds);
        return request;
    }

    private SysUser user(String id, String role, String name) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setName(name);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
