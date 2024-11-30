package ru.mastkey.cloudservice.util;

import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public class PaginationUtils {
    public static PageRequest buildPageRequest(@Nullable Integer page, @Nullable Integer size, int defaultSize) {
        return PageRequest.of((Integer) Optional.ofNullable(page).filter(PaginationUtils::zeroOrPositive).orElse(0), (Integer)Optional.ofNullable(size).filter(PaginationUtils::zeroOrPositive).orElse(defaultSize));
    }

    private static boolean zeroOrPositive(int x) {
        return x >= 0;
    }
}
