package org.exemodel.util;

/**
 * Created by zp on 16/9/2.
 */
public class Pagination {
    private int page = 1;
    private int size = 10;
    private int total = -1;
    private boolean needTotal = true;


    public int getPage() {
        return page;
    }

    public Pagination() {
    }

    public Pagination(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public Pagination(int page, int size, boolean needTotal) {
        this.page = page;
        this.size = size;
        this.needTotal = needTotal;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOffset() {
        return (page - 1) * size;
    }

    public boolean isNeedTotal() {
        return needTotal;
    }

    public void setNeedTotal(boolean needTotal) {
        this.needTotal = needTotal;
    }
}
