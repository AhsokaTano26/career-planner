package com.rickgao.careercore.modules.advisor.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.advisor.dto.StudentListQuery;
import com.rickgao.careercore.modules.advisor.entity.AdvisorComment;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorCommentMapper;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentQueryMapper;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import com.rickgao.careercore.modules.advisor.query.ActiveGoalRow;
import com.rickgao.careercore.modules.advisor.query.CheckinRow;
import com.rickgao.careercore.modules.advisor.query.GoalChangeRow;
import com.rickgao.careercore.modules.advisor.query.LastReviewRow;
import com.rickgao.careercore.modules.advisor.query.PathCountRow;
import com.rickgao.careercore.modules.advisor.query.PlanRateRow;
import com.rickgao.careercore.modules.advisor.query.PlanRow;
import com.rickgao.careercore.modules.advisor.query.ProfileRow;
import com.rickgao.careercore.modules.advisor.query.ResultRow;
import com.rickgao.careercore.modules.advisor.query.ReviewRow;
import com.rickgao.careercore.modules.advisor.query.RunRow;
import com.rickgao.careercore.modules.advisor.query.SnapshotRow;
import com.rickgao.careercore.modules.advisor.query.TaskRow;
import com.rickgao.careercore.modules.advisor.service.AdvisorScopeService;
import com.rickgao.careercore.modules.advisor.service.AdvisorStudentService;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStatisticsVO;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStudentVO;
import com.rickgao.careercore.modules.advisor.vo.AttentionStudentVO;
import com.rickgao.careercore.modules.advisor.vo.StudentDetailViewVO;
import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.mapper.StudentExperienceMapper;
import com.rickgao.careercore.modules.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdvisorStudentServiceImplTest {

    private static final List<String> ALL_IDS = List.of("S1001", "S1002", "S1003", "S1004", "S1006");

    private final AdvisorScopeService scopeService = mock(AdvisorScopeService.class);
    private final AdvisorStudentRelationMapper relationMapper = mock(AdvisorStudentRelationMapper.class);
    private final AdvisorStudentQueryMapper queryMapper = mock(AdvisorStudentQueryMapper.class);
    private final AdvisorCommentMapper advisorCommentMapper = mock(AdvisorCommentMapper.class);
    private final StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
    private final StudentExperienceMapper studentExperienceMapper = mock(StudentExperienceMapper.class);

    private final AdvisorStudentService service = new AdvisorStudentServiceImpl(
            scopeService, relationMapper, queryMapper, advisorCommentMapper,
            studentProfileMapper, studentExperienceMapper);

    @Test
    void listStudents_computesStatusesAndFields() {
        when(queryMapper.selectFilteredStudentIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ALL_IDS);
        stubDefaultAggregates();

        PageResult<AdvisorStudentVO> result = service.listStudents("A1001", new StudentListQuery());

        assertEquals(5, result.getTotal());
        Map<String, AdvisorStudentVO> byId = result.getList().stream()
                .collect(Collectors.toMap(AdvisorStudentVO::getId, Function.identity()));
        assertEquals("good", byId.get("S1001").getStatus());
        assertEquals("review", byId.get("S1002").getStatus());
        assertEquals("todo", byId.get("S1003").getStatus());
        assertEquals("late", byId.get("S1004").getStatus());
        assertEquals("todo", byId.get("S1006").getStatus());
        assertEquals(67, byId.get("S1001").getPlanRate());
        assertEquals("后端开发工程师", byId.get("S1001").getDirection());
        assertEquals("employment", byId.get("S1001").getPath());
        assertNull(byId.get("S1003").getPrimaryGoal());
        assertEquals(Boolean.TRUE, byId.get("S1002").getAskGuidance());
        assertEquals(Boolean.TRUE, byId.get("S1001").getAssessed());
        assertEquals(Boolean.FALSE, byId.get("S1006").getAssessed());
    }

    @Test
    void listStudents_paginates() {
        when(queryMapper.selectFilteredStudentIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ALL_IDS);
        stubDefaultAggregates();
        StudentListQuery query = new StudentListQuery();
        query.setPage(1);
        query.setSize(2);

        PageResult<AdvisorStudentVO> result = service.listStudents("A1001", query);

        assertEquals(2, result.getList().size());
        assertEquals(5, result.getTotal());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
    }

    @Test
    void listStudents_sortsByPlanRateAsc() {
        when(queryMapper.selectFilteredStudentIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ALL_IDS);
        stubDefaultAggregates();
        StudentListQuery query = new StudentListQuery();
        query.setSort("planRate");

        PageResult<AdvisorStudentVO> result = service.listStudents("A1001", query);

        assertEquals("S1004", result.getList().get(0).getId());
        assertEquals("S1001", result.getList().get(1).getId());
    }

    @Test
    void listStudents_sortsByPlanRateDesc() {
        when(queryMapper.selectFilteredStudentIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ALL_IDS);
        stubDefaultAggregates();
        StudentListQuery query = new StudentListQuery();
        query.setSort("-planRate");

        PageResult<AdvisorStudentVO> result = service.listStudents("A1001", query);

        assertEquals("S1001", result.getList().get(0).getId());
    }

    @Test
    void listStudents_passesPathFilter() {
        when(queryMapper.selectFilteredStudentIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of("S1001", "S1004"));
        stubDefaultAggregates();
        StudentListQuery query = new StudentListQuery();
        query.setPath("employment");

        PageResult<AdvisorStudentVO> result = service.listStudents("A1001", query);

        assertEquals(2, result.getTotal());
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryMapper).selectFilteredStudentIds(eq("A1001"), pathCaptor.capture(), any(), any(), any(),
                any(), any(), any(), any(), any());
        assertEquals("employment", pathCaptor.getValue());
    }

    @Test
    void listStudents_invalidPath_throwsValidationError() {
        StudentListQuery query = new StudentListQuery();
        query.setPath("unknown");
        BizException ex = assertThrows(BizException.class, () -> service.listStudents("A1001", query));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void listAttention_returnsReasonsForConcernedStudents() {
        when(relationMapper.findStudentIdsByAdvisor("A1001")).thenReturn(ALL_IDS);
        stubDefaultAggregates();

        List<AttentionStudentVO> result = service.listAttention("A1001");

        assertEquals(2, result.size());
        assertEquals("S1002", result.get(0).getStudent().getId());
        assertEquals(List.of("已申请辅导员指导", "长期未复盘", "多次调整目标"), result.get(0).getReasons());
        assertEquals("S1004", result.get(1).getStudent().getId());
        assertEquals(List.of("长期未复盘"), result.get(1).getReasons());
    }

    @Test
    void getStatistics_aggregatesCounts() {
        when(queryMapper.countAssignedStudents("A1001")).thenReturn(5);
        when(queryMapper.countAssessedStudents("A1001")).thenReturn(4);
        when(queryMapper.countPlanMadeStudents("A1001")).thenReturn(2);
        when(queryMapper.countReviewedThisMonthStudents(eq("A1001"), any())).thenReturn(1);
        when(queryMapper.selectPathCounts("A1001")).thenReturn(List.of(
                pathCount("employment", 2L),
                pathCount("graduate", 1L),
                pathCount("undecided", 2L)));
        when(queryMapper.selectCompletionRates("A1001")).thenReturn(List.of(
                rate("S1001", 3L, 2L),
                rate("S1004", 2L, 1L)));

        AdvisorStatisticsVO vo = service.getStatistics("A1001");

        assertEquals(5, vo.getTotalStudents());
        assertEquals(4, vo.getAssessedCount());
        assertEquals(2, vo.getPlanMadeCount());
        assertEquals(1, vo.getReviewedCount());
        Map<String, Integer> dist = vo.getPathDistribution().stream()
                .collect(Collectors.toMap(AdvisorStatisticsVO.PathDistribution::getPath,
                        AdvisorStatisticsVO.PathDistribution::getCount));
        assertEquals(1, dist.get("graduate"));
        assertEquals(2, dist.get("employment"));
        assertEquals(0, dist.get("overseas"));
        assertEquals(2, dist.get("undecided"));
        assertEquals(58.3, vo.getTaskCompletionRate(), 0.01);
    }

    @Test
    void getDetail_studentNotFound_throws404() {
        when(studentProfileMapper.findByUserId("S9999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.getDetail("A1001", "S9999"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
        verify(scopeService).assertAssigned("A1001", "S9999");
    }

    @Test
    void getDetail_assemblesReadOnlyTimeline() {
        when(studentProfileMapper.findByUserId("S1001")).thenReturn(profileEntity());
        when(studentExperienceMapper.findAllByStudentId("S1001")).thenReturn(List.of());
        when(queryMapper.selectLatestSnapshot("S1001")).thenReturn(snapshotRow());
        when(queryMapper.selectLatestRun("S1001")).thenReturn(runRow());
        when(queryMapper.selectResultsByRunId("R1001")).thenReturn(List.of());
        when(queryMapper.selectActiveGoals(any())).thenReturn(List.of(goal("S1001", "PRIMARY",
                "employment_backend", "后端开发工程师", "后端开发", 2)));
        when(queryMapper.selectLatestConfirmedPlan("S1001")).thenReturn(planRow());
        when(queryMapper.selectTasksByPlanId("PLAN1001")).thenReturn(List.of(taskRow()));
        when(queryMapper.selectCheckinsByTaskIds(any())).thenReturn(List.of(checkinRow()));
        when(queryMapper.selectSubmittedReviews("S1001")).thenReturn(List.of(reviewRow()));
        when(advisorCommentMapper.findByStudentId("S1001")).thenReturn(List.of(comment()));

        StudentDetailViewVO vo = service.getDetail("A1001", "S1001");

        verify(scopeService).assertAssigned("A1001", "S1001");
        assertEquals("李明", vo.getProfile().getName());
        assertEquals("PS-1001", vo.getPortrait().getId());
        assertEquals("R1001", vo.getRecommendation().getRunId());
        assertEquals("PLAN1001", vo.getPlan().getId());
        assertEquals(1, vo.getTasks().size());
        assertEquals("已完成,掌握基础", vo.getTasks().get(0).getCheckin().getDoneDesc());
        assertEquals(1, vo.getReviews().size());
        assertEquals(1, vo.getGuidance().size());
    }

    // ---------- 测试数据 ----------

    private void stubDefaultAggregates() {
        LocalDateTime now = LocalDateTime.now();
        when(queryMapper.selectAssessedStudentIds(any())).thenReturn(List.of("S1001", "S1002", "S1003", "S1004"));
        when(queryMapper.selectActiveGoals(any())).thenReturn(List.of(
                goal("S1001", "PRIMARY", "employment_backend", "后端开发工程师", "后端开发", 2),
                goal("S1002", "PRIMARY", "graduate_software", "计算机技术考研", "考研上岸", 4),
                goal("S1004", "PRIMARY", "employment_backend", "后端开发工程师", "后端开发", 1)));
        when(queryMapper.selectPlanRate(any())).thenReturn(List.of(
                rate("S1001", 3L, 2L),
                rate("S1004", 2L, 1L)));
        when(queryMapper.selectLastReview(any())).thenReturn(List.of(
                review("S1001", now.minusDays(5)),
                review("S1002", now.minusDays(40)),
                review("S1003", now.minusDays(3)),
                review("S1004", now.minusDays(40)),
                review("S1006", now.minusDays(4))));
        when(queryMapper.selectPendingGuidanceStudentIds(any())).thenReturn(List.of("S1002"));
        when(queryMapper.selectGoalChangeCounts(any(), any())).thenReturn(List.of(change("S1002", 3L)));
        when(queryMapper.selectProfileBasics(any())).thenReturn(List.of(
                profile("S1001", "李明", "计科2601", 92, "employment"),
                profile("S1002", "张雨", "计科2602", 85, "graduate"),
                profile("S1003", "王芳", "软工2601", 70, "undecided"),
                profile("S1004", "赵磊", "计科2601", 88, "employment"),
                profile("S1006", "刘洋", "软工2601", 45, "undecided")));
    }

    private ActiveGoalRow goal(String studentId, String type, String directionId, String directionName,
                               String name, int versionNo) {
        ActiveGoalRow row = new ActiveGoalRow();
        row.setStudentId(studentId);
        row.setGoalType(type);
        row.setDirectionId(directionId);
        row.setDirectionName(directionName);
        row.setGoalName(name);
        row.setVersionNo(versionNo);
        row.setChosenAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        row.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return row;
    }

    private ProfileRow profile(String id, String name, String className, int completeness, String intention) {
        ProfileRow row = new ProfileRow();
        row.setStudentId(id);
        row.setName(name);
        row.setClassName(className);
        row.setCompleteness(completeness);
        row.setDevelopmentIntention(intention);
        row.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        return row;
    }

    private PlanRateRow rate(String studentId, long total, long done) {
        PlanRateRow row = new PlanRateRow();
        row.setStudentId(studentId);
        row.setTotalTasks(total);
        row.setDoneTasks(done);
        return row;
    }

    private LastReviewRow review(String studentId, LocalDateTime at) {
        LastReviewRow row = new LastReviewRow();
        row.setStudentId(studentId);
        row.setLastReviewAt(at);
        return row;
    }

    private GoalChangeRow change(String studentId, long count) {
        GoalChangeRow row = new GoalChangeRow();
        row.setStudentId(studentId);
        row.setChangeCount(count);
        return row;
    }

    private PathCountRow pathCount(String path, long count) {
        PathCountRow row = new PathCountRow();
        row.setPath(path);
        row.setCount(count);
        return row;
    }

    private StudentProfile profileEntity() {
        StudentProfile profile = new StudentProfile();
        profile.setId("P1001");
        profile.setUserId("S1001");
        profile.setName("李明");
        profile.setClassName("计科2601");
        profile.setGrade("2026级");
        profile.setMajorCategory("计算机类");
        profile.setCompleteness(92);
        profile.setDevelopmentIntention("employment");
        profile.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        return profile;
    }

    private SnapshotRow snapshotRow() {
        SnapshotRow row = new SnapshotRow();
        row.setId("PS-1001");
        row.setStudentId("S1001");
        row.setSourceVersion("Q v2");
        row.setVersionNo(2);
        row.setCompleteness(92);
        row.setDimensionJson("[{\"key\":\"interest\",\"name\":\"兴趣\",\"score\":78}]");
        row.setStrengthsJson("[\"数学基础较好\"]");
        row.setExploreJson("[]");
        row.setCreatedAt(LocalDateTime.of(2026, 9, 1, 9, 13));
        return row;
    }

    private RunRow runRow() {
        RunRow row = new RunRow();
        row.setId("R1001");
        row.setStudentId("S1001");
        row.setProfileSnapshotId("PS-1001");
        row.setProfileVersion(2);
        row.setRuleVersion("R1.0");
        row.setStatus("SUCCESS");
        row.setGeneratedAt(LocalDateTime.of(2026, 9, 1, 9, 13, 30));
        return row;
    }

    private PlanRow planRow() {
        PlanRow row = new PlanRow();
        row.setId("PLAN1001");
        row.setStudentId("S1001");
        row.setVersionNo(2);
        row.setStatus("CONFIRMED");
        row.setSource("AI");
        row.setGoalSummary("本学期完成 Java 基础");
        row.setSemesterGoalsJson("[{\"title\":\"掌握Java\",\"abilityTag\":\"programming_basic\"}]");
        row.setMonthlyTasksJson("[]");
        row.setNotesJson("[]");
        row.setConfirmedAt(LocalDateTime.of(2026, 9, 2, 10, 5));
        row.setUpdatedAt(LocalDateTime.of(2026, 9, 2, 10, 5));
        return row;
    }

    private TaskRow taskRow() {
        TaskRow row = new TaskRow();
        row.setId("T1001");
        row.setPlanId("PLAN1001");
        row.setStudentId("S1001");
        row.setMonth("2026-09");
        row.setTitle("完成 Java 基础");
        row.setTaskType("LEARNING");
        row.setEstHours(12.0);
        row.setStatus("DONE");
        row.setAbilityTagsJson("[\"programming_basic\"]");
        row.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        return row;
    }

    private CheckinRow checkinRow() {
        CheckinRow row = new CheckinRow();
        row.setId("TC-001");
        row.setTaskId("T1001");
        row.setDoneDesc("已完成,掌握基础");
        row.setGains("理解了面向对象");
        row.setCheckedInAt(LocalDateTime.of(2026, 9, 20, 10, 0));
        return row;
    }

    private ReviewRow reviewRow() {
        ReviewRow row = new ReviewRow();
        row.setId("R1");
        row.setStudentId("S1001");
        row.setCycle("2026-08");
        row.setStatus("SUBMITTED");
        row.setContentJson("{\"done\":\"完成 Java\"}");
        row.setAiSuggestJson("[]");
        row.setAdvisorRequested(Boolean.FALSE);
        row.setSubmittedAt(LocalDateTime.of(2026, 8, 2, 9, 0));
        return row;
    }

    private AdvisorComment comment() {
        AdvisorComment comment = new AdvisorComment();
        comment.setId("GC-001");
        comment.setStudentId("S1001");
        comment.setAdvisorId("A1001");
        comment.setContent("建议聚焦主线");
        comment.setAdviceType("COMMENT");
        comment.setCreatedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        return comment;
    }
}
