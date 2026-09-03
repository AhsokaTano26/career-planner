package com.rickgao.careercore.modules.direction.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.direction.entity.Favorite;
import com.rickgao.careercore.modules.direction.mapper.FavoriteMapper;
import com.rickgao.careercore.modules.direction.service.DirectionService;
import com.rickgao.careercore.modules.direction.vo.DirectionDetailVO;
import com.rickgao.careercore.modules.direction.vo.DirectionItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 方向学生端业务实现：复用 AdminDirectionMapper 读取已发布方向，FavoriteMapper 维护收藏。
 */
@Service
public class DirectionServiceImpl implements DirectionService {

    private static final Set<String> PATH_ENUM = Set.of("graduate", "employment", "overseas");

    private final AdminDirectionMapper directionMapper;
    private final FavoriteMapper favoriteMapper;
    private final IdGenerator idGenerator;

    public DirectionServiceImpl(AdminDirectionMapper directionMapper,
                                FavoriteMapper favoriteMapper,
                                IdGenerator idGenerator) {
        this.directionMapper = directionMapper;
        this.favoriteMapper = favoriteMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<DirectionItemVO> listDirections(String studentId, String path) {
        List<CareerDirection> directions = directionMapper.selectAllPublished();
        Set<String> favorited = favoriteDirectionIds(studentId);
        return directions.stream()
                .filter(d -> !StringUtils.hasText(path) || path.equals(d.getPath()))
                .map(d -> {
                    DirectionItemVO vo = new DirectionItemVO();
                    vo.setId(d.getId());
                    vo.setName(d.getName());
                    vo.setPath(d.getPath());
                    vo.setIcon(d.getIcon());
                    vo.setIntro(d.getIntro());
                    vo.setFavorited(favorited.contains(d.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DirectionDetailVO getDirection(String studentId, String directionId) {
        CareerDirection d = directionMapper.findById(directionId);
        if (d == null || !"PUBLISHED".equals(d.getStatus())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "方向不存在或未发布");
        }
        DirectionDetailVO vo = new DirectionDetailVO();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setPath(d.getPath());
        vo.setIcon(d.getIcon());
        vo.setIntro(d.getIntro());
        vo.setTarget(parseMap(d.getTargetJson()));
        vo.setLearning(parseStringList(d.getLearningJson()));
        vo.setAbilities(parseStringList(d.getAbilitiesJson()));
        vo.setCourses(parseStringList(d.getCoursesJson()));
        vo.setActivities(parseStringList(d.getActivitiesJson()));
        vo.setPathDesc(parseStringList(d.getPathDescJson()));
        vo.setMisconceptions(parseStringList(d.getMisconceptionsJson()));
        vo.setApplicableMajors(parseStringList(d.getApplicableMajorsJson()));
        vo.setFavorited(favoriteMapper.selectByStudentAndDirection(studentId, directionId) != null);
        return vo;
    }

    @Override
    public List<DirectionDetailVO> listFavorites(String studentId) {
        List<Favorite> favorites = favoriteMapper.selectByStudent(studentId);
        return favorites.stream()
                .map(f -> {
                    CareerDirection d = directionMapper.findById(f.getDirectionId());
                    if (d == null) {
                        return null;
                    }
                    DirectionDetailVO vo = new DirectionDetailVO();
                    vo.setId(d.getId());
                    vo.setName(d.getName());
                    vo.setPath(d.getPath());
                    vo.setIcon(d.getIcon());
                    vo.setIntro(d.getIntro());
                    vo.setTarget(parseMap(d.getTargetJson()));
                    vo.setLearning(parseStringList(d.getLearningJson()));
                    vo.setAbilities(parseStringList(d.getAbilitiesJson()));
                    vo.setCourses(parseStringList(d.getCoursesJson()));
                    vo.setActivities(parseStringList(d.getActivitiesJson()));
                    vo.setPathDesc(parseStringList(d.getPathDescJson()));
                    vo.setMisconceptions(parseStringList(d.getMisconceptionsJson()));
                    vo.setApplicableMajors(parseStringList(d.getApplicableMajorsJson()));
                    vo.setFavorited(true);
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addFavorite(String studentId, String directionId) {
        CareerDirection d = directionMapper.findById(directionId);
        if (d == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "方向不存在");
        }
        if (favoriteMapper.selectByStudentAndDirection(studentId, directionId) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "已收藏该方向");
        }
        Favorite favorite = new Favorite();
        favorite.setId(idGenerator.favoriteId());
        favorite.setStudentId(studentId);
        favorite.setDirectionId(directionId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(String studentId, String directionId) {
        favoriteMapper.deleteByStudentAndDirection(studentId, directionId);
    }

    private Set<String> favoriteDirectionIds(String studentId) {
        return favoriteMapper.selectByStudent(studentId).stream()
                .map(Favorite::getDirectionId)
                .collect(Collectors.toSet());
    }

    private List<String> parseStringList(String json) {
        return StringUtils.hasText(json)
                ? JsonUtil.parse(json, new TypeReference<List<String>>() {
        })
                : List.of();
    }

    private Map<String, Object> parseMap(String json) {
        return StringUtils.hasText(json)
                ? JsonUtil.parse(json, new TypeReference<Map<String, Object>>() {
        })
                : null;
    }
}
