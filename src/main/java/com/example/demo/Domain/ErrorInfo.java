package com.example.demo.Domain;

public class ErrorInfo {
    // エラーになるcategory_name
    private String categoryName;
    // エラーコード
    private String errorCode;

    public ErrorInfo(String categoryName, String errorCode) {
        this.categoryName = categoryName;
        this.errorCode = errorCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

}
