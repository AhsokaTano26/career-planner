package com.rickgao.careercore.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA 前端路由回退（Vue Router history 模式）。
 * <p>
 * 前端构建产物打包进 Spring Boot 静态资源后，浏览器直接访问深层路由
 * （如 {@code /student/profile}、{@code /advisor/students}）时没有对应的后端映射，
 * 这里将这类路径转发到 {@code index.html}，由 Vue Router 接管页面渲染与鉴权。
 * API（/api/v1/**）与静态资源（/assets/** 等）不受影响。
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/login", "/student/**", "/advisor/**", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
