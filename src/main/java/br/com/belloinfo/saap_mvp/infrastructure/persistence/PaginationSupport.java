package br.com.belloinfo.saap_mvp.infrastructure.persistence;

import br.com.belloinfo.saap_mvp.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

public final class PaginationSupport {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PaginationSupport() {
    }

    public static Pageable of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    public static <E, D> PageResult<D> toPageResult(Page<E> page, Function<E, D> mapper) {
        return new PageResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
