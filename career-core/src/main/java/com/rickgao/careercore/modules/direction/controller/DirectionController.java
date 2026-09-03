package com.rickgao.careercore.modules.direction.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.direction.service.DirectionService;
import com.rickgao.careercore.modules.direction.vo.DirectionDetailVO;
import com.rickgao.careercore.modules.direction.vo.DirectionItemVO;
import com.rickgao.careercore.security.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 方向学生端路由：浏览已发布方向 + 我的收藏。
 */
@RestController
@RequestMapping("/api/v1")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @GetMapping("/students/me/directions")
    public ApiResponse<List<DirectionItemVO>> listDirections(
            @RequestParam(required = false) String path) {
        return ApiResponse.ok(directionService.listDirections(SecurityUtils.currentUserId(), path));
    }

    @GetMapping("/students/me/directions/{directionId}")
    public ApiResponse<DirectionDetailVO> getDirection(@PathVariable String directionId) {
        return ApiResponse.ok(directionService.getDirection(SecurityUtils.currentUserId(), directionId));
    }

    @GetMapping("/students/me/favorites")
    public ApiResponse<List<DirectionDetailVO>> listFavorites() {
        return ApiResponse.ok(directionService.listFavorites(SecurityUtils.currentUserId()));
    }

    @PostMapping("/students/me/favorites/{directionId}")
    public ApiResponse<Map<String, Object>> addFavorite(@PathVariable String directionId) {
        directionService.addFavorite(SecurityUtils.currentUserId(), directionId);
        return ApiResponse.ok(Map.of());
    }

    @DeleteMapping("/students/me/favorites/{directionId}")
    public ApiResponse<Map<String, Object>> removeFavorite(@PathVariable String directionId) {
        directionService.removeFavorite(SecurityUtils.currentUserId(), directionId);
        return ApiResponse.ok(Map.of());
    }
}
