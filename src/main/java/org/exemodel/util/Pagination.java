package org.exemodel.util;

/**
 * @author zp [15951818230@163.com]
 */
public class Pagination {
    private int page = 1;
    private int size = 10;
    private long total = -1;
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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
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
