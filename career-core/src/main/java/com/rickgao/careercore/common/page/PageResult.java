package com.rickgao.careercore.common.page;

import lombok.Data;

import java.util.List;

/**
 * 统一分页结果:对齐 openapi 示例 data 结构 { list, page, size, total, totalPages }。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list == null ? List.of() : list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(size <= 0 ? 0 : (int) ((total + size - 1L) / size));
        return result;
    }
}
