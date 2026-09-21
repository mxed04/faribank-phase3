package ir.ac.kntu.ui;

import ir.ac.kntu.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationHelperTest {

    @Test
    void testEmptyListPagination() {
        PaginationHelper<String> pager = new PaginationHelper<>(List.of(), 5);
        assertEquals(0, pager.getTotalItems());
        assertEquals(1, pager.getTotalPages());
        assertEquals(1, pager.getCurrentPage());
        assertTrue(pager.getPageItems().isEmpty());
        assertFalse(pager.hasNextPage());
        assertFalse(pager.hasPrevPage());
    }

    @Test
    void testStandardPaginationNavigation() {
        List<Integer> numbers = new ArrayList<>();
        for (int idx = 1; idx <= 25; idx++) {
            numbers.add(idx);
        }

        PaginationHelper<Integer> pager = new PaginationHelper<>(numbers, 10);
        assertEquals(25, pager.getTotalItems());
        assertEquals(3, pager.getTotalPages());
        assertEquals(1, pager.getCurrentPage());

        List<Integer> page1 = pager.getPageItems();
        assertEquals(10, page1.size());
        assertEquals(1, page1.get(0));
        assertEquals(10, page1.get(9));

        assertTrue(pager.hasNextPage());
        assertTrue(pager.nextPage());
        assertEquals(2, pager.getCurrentPage());

        List<Integer> page2 = pager.getPageItems();
        assertEquals(10, page2.size());
        assertEquals(11, page2.get(0));

        assertTrue(pager.nextPage());
        assertEquals(3, pager.getCurrentPage());

        List<Integer> page3 = pager.getPageItems();
        assertEquals(5, page3.size());
        assertEquals(21, page3.get(0));
        assertEquals(25, page3.get(4));

        assertFalse(pager.nextPage());
        assertTrue(pager.previousPage());
        assertEquals(2, pager.getCurrentPage());

        assertTrue(pager.setPage(1));
        assertEquals(1, pager.getCurrentPage());
        assertFalse(pager.setPage(99));
    }

    @Test
    void testInvalidPageSizeThrowsException() {
        assertThrows(ValidationException.class, () -> new PaginationHelper<>(List.of(1, 2), 0));
        assertThrows(ValidationException.class, () -> new PaginationHelper<>(List.of(1, 2), -5));
    }
}