package com.example.demo.Domain;

public class Original {
    private Integer id;
    private String name;
    private Integer condition;
    private String categoryName;
    private String brand;
    private double price;
    private Integer shipping;
    private String description;

    public Original() {
    }

    public Original(Integer id, String name, Integer condition, String categoryName, String brand, double price,
            Integer shipping, String description) {
        this.id = id;
        this.name = name;
        this.condition = condition;
        this.categoryName = categoryName;
        this.brand = brand;
        this.price = price;
        this.shipping = shipping;
        this.description = description;
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

    public Integer getCondition() {
        return condition;
    }

    public void setCondition(Integer condition) {
        this.condition = condition;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getShipping() {
        return shipping;
    }

    public void setShipping(Integer shipping) {
        this.shipping = shipping;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Original [id=" + id + ", name=" + name + ", condition=" + condition + ", categoryName="
                + categoryName + ", brand=" + brand + ", price=" + price + ", shipping=" + shipping + ", description="
                + description + "]";
    }

}
