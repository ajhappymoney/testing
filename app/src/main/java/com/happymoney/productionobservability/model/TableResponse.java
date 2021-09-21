package com.happymoney.productionobservability.model;

public class TableResponse {
    private Integer rank;

    private PageData pageDataValue;

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public PageData getPageDataValue() {
        return pageDataValue;
    }

    public void setPageDataValue(PageData pageDataValue) {
        this.pageDataValue = pageDataValue;
    }

    public TableResponse(int rank, PageData pageDataValue) {
        this.rank = rank;
        this.pageDataValue = pageDataValue;
    }

    public TableResponse() {
    }

    @Override
    public String toString() {
        return "TableResponse{" +
                "rank=" + rank +
                ", pageDataValue=" + pageDataValue +
                '}';
    }
}
