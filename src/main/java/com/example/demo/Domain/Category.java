package com.example.demo.Domain;

public class Category {
    private Integer id;
    private String name;
    private Integer parentId;
    private String nameAll;

    public Category() {
    }

    public Category(String name, Integer parentId, String nameAll) {
        this.name = name;
        this.parentId = parentId;
        this.nameAll = nameAll;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getNameAll() {
        return nameAll;
    }

    public void setNameAll(String nameAll) {
        this.nameAll = nameAll;
    }

    @Override
    public String toString() {
        return "Category [id=" + id + ", name=" + name + ", parentId=" + parentId + ", nameAll=" + nameAll + "]";
    }


}
