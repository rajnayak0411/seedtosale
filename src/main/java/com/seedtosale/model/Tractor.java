package com.seedtosale.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Tractor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private int price;
    private String description;
    private String image;
    private int stockQuantity;
    private int soldQuantity = 0;

    private int hours; // New field to store rental hours

    public Tractor() {
    }
    public Tractor(String name, String location, int price, String description, String image, int stockQuantity, int soldQuantity, int hours){
        this.name = name;
        this.location = location;
        this.price = price;
        this.description = description;
        this.image = image;
        this.stockQuantity = stockQuantity;
        this.soldQuantity = soldQuantity;
        this.hours = hours;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }
}
