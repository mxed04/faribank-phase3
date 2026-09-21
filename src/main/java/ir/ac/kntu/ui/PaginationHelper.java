package ir.ac.kntu.ui;

import ir.ac.kntu.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic pagination utility for terminal and service-layer list splitting.
 *
 * @param <T> Element type contained in paginated list
 */
public class PaginationHelper<T> {
    public static final int DEFAULT_SIZE = 10;

    private final List<T> items;
    private final int pageSize;
    private int currentPage;

    public PaginationHelper(List<T> sourceItems, int pageSize) {
        if (pageSize <= 0) {
            throw new ValidationException("Page size must be greater than zero.");
        }
        this.items = new ArrayList<>(Objects.requireNonNull(sourceItems, "Items cannot be null."));
        this.pageSize = pageSize;
        this.currentPage = 1;
    }

    public PaginationHelper(List<T> sourceItems) {
        this(sourceItems, DEFAULT_SIZE);
    }

    public int getTotalItems() {
        return items.size();
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        if (items.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) items.size() / pageSize);
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages();
    }

    public boolean hasPrevPage() {
        return currentPage > 1;
    }

    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            return true;
        }
        return false;
    }

    public boolean previousPage() {
        if (hasPrevPage()) {
            currentPage--;
            return true;
        }
        return false;
    }

    public boolean setPage(int targetPage) {
        if (targetPage >= 1 && targetPage <= getTotalPages()) {
            this.currentPage = targetPage;
            return true;
        }
        return false;
    }

    public List<T> getPageItems() {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, items.size());
        if (startIndex >= items.size()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(items.subList(startIndex, endIndex));
    }
}