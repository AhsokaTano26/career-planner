package com.rickgao.careercore.modules.direction.service;

import com.rickgao.careercore.modules.direction.vo.DirectionDetailVO;
import com.rickgao.careercore.modules.direction.vo.DirectionItemVO;

import java.util.List;

/**
 * 方向学生端业务：浏览已发布方向 + 我的收藏。
 */
public interface DirectionService {

    List<DirectionItemVO> listDirections(String studentId, String path);

    DirectionDetailVO getDirection(String studentId, String directionId);

    List<DirectionDetailVO> listFavorites(String studentId);

    void addFavorite(String studentId, String directionId);

    void removeFavorite(String studentId, String directionId);
}

