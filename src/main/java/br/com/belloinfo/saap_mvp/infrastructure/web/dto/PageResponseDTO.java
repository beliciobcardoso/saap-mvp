package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponseDTO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PageResponseDTO<T> from(PageResult<D> pageResult, Function<D, T> mapper) {
        return new PageResponseDTO<>(
                pageResult.content().stream().map(mapper).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}
