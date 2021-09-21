package com.happymoney.productionobservability.model;


import java.util.ArrayList;

public class PageData {
    private Integer count;
    private String pageName;
    private ArrayList<ResultAttributes> resultAttributesArrayList;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public ArrayList<ResultAttributes> getResultAttributesArrayList() {
        return resultAttributesArrayList;
    }

    public void setResultAttributesArrayList(ArrayList<ResultAttributes> resultAttributesArrayList) {
        this.resultAttributesArrayList = resultAttributesArrayList;
    }

    public PageData(Integer count, String pageName, ArrayList<ResultAttributes> resultAttributesArrayList) {
        this.count = count;
        this.pageName = pageName;
        this.resultAttributesArrayList = resultAttributesArrayList;
    }

    public PageData() {
    }

    @Override
    public String toString() {
        return "PageData{" +
                "count=" + count +
                ", pageName='" + pageName + '\'' +
                ", resultAttributesArrayList=" + resultAttributesArrayList +
                '}';
    }
}
