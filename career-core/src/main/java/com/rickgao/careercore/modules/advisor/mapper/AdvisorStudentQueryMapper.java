package com.rickgao.careercore.modules.advisor.mapper;

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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * advisor 聚合查询 Mapper(只读)。
 */
@Mapper
public interface AdvisorStudentQueryMapper {

    /** 按组合条件筛选辅导员名下学生 ID(不含分页,由服务层组装排序分页) */
    List<String> selectFilteredStudentIds(@Param("advisorId") String advisorId,
                                          @Param("path") String path,
                                          @Param("directionId") String directionId,
                                          @Param("hasGoal") Boolean hasGoal,
                                          @Param("reviewedThisMonth") Boolean reviewedThisMonth,
                                          @Param("longNoReview") Boolean longNoReview,
                                          @Param("guidanceRequested") Boolean guidanceRequested,
                                          @Param("keyword") String keyword,
                                          @Param("monthStart") LocalDateTime monthStart,
                                          @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    List<String> selectAssessedStudentIds(@Param("studentIds") Collection<String> studentIds);

    List<ActiveGoalRow> selectActiveGoals(@Param("studentIds") Collection<String> studentIds);

    List<PlanRateRow> selectPlanRate(@Param("studentIds") Collection<String> studentIds);

    List<LastReviewRow> selectLastReview(@Param("studentIds") Collection<String> studentIds);

    List<String> selectPendingGuidanceStudentIds(@Param("studentIds") Collection<String> studentIds);

    List<GoalChangeRow> selectGoalChangeCounts(@Param("studentIds") Collection<String> studentIds,
                                               @Param("since") LocalDateTime since);

    List<ProfileRow> selectProfileBasics(@Param("studentIds") Collection<String> studentIds);

    SnapshotRow selectLatestSnapshot(@Param("studentId") String studentId);

    RunRow selectLatestRun(@Param("studentId") String studentId);

    List<ResultRow> selectResultsByRunId(@Param("runId") String runId);

    PlanRow selectLatestConfirmedPlan(@Param("studentId") String studentId);

    List<TaskRow> selectTasksByPlanId(@Param("planId") String planId);

    List<CheckinRow> selectCheckinsByTaskIds(@Param("taskIds") Collection<String> taskIds);

    List<ReviewRow> selectSubmittedReviews(@Param("studentId") String studentId);

    int countAssignedStudents(@Param("advisorId") String advisorId);

    int countAssessedStudents(@Param("advisorId") String advisorId);

    int countPlanMadeStudents(@Param("advisorId") String advisorId);

    int countReviewedThisMonthStudents(@Param("advisorId") String advisorId,
                                       @Param("monthStart") LocalDateTime monthStart);

    List<PathCountRow> selectPathCounts(@Param("advisorId") String advisorId);

    List<PlanRateRow> selectCompletionRates(@Param("advisorId") String advisorId);
}
