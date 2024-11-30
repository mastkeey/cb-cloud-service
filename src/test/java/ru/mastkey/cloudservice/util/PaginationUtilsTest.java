package ru.mastkey.cloudservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationUtilsTest {

    @Test
    void classTest() {
        var paginationUtils = new PaginationUtils();
        assertThat(paginationUtils).isNotNull();
    }

    @Test
    void buildPageRequest_ShouldReturnDefaultPageRequest_WhenNullValuesProvided() {
        int defaultSize = 10;

        PageRequest pageRequest = PaginationUtils.buildPageRequest(null, null, defaultSize);

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(defaultSize);
    }

    @Test
    void buildPageRequest_ShouldReturnProvidedValues_WhenValidValuesProvided() {
        int page = 2;
        int size = 15;

        PageRequest pageRequest = PaginationUtils.buildPageRequest(page, size, 10);

        assertThat(pageRequest.getPageNumber()).isEqualTo(page);
        assertThat(pageRequest.getPageSize()).isEqualTo(size);
    }

    @Test
    void buildPageRequest_ShouldReturnDefaultValues_WhenNegativeValuesProvided() {
        int defaultSize = 10;

        PageRequest pageRequest = PaginationUtils.buildPageRequest(-1, -5, defaultSize);

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(defaultSize);
    }

    @Test
    void buildPageRequest_ShouldReturnProvidedSize_WhenValidSizeAndDefaultValuesProvided() {
        int size = 20;

        PageRequest pageRequest = PaginationUtils.buildPageRequest(null, size, 10);

        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(size);
    }
}