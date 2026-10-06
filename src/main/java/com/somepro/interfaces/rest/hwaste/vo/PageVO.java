package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层）—— 不可变 record。
 * 比领域层 PageResult 多带一个 totalPages，方便前端直接渲染分页器。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}
