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
public class Seed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String location;
    private int price;
    private String image;

    private int stockQuantity;
    private int soldQuantity = 0;

    public Seed() {
    }

    public Seed(String name, String type, String location, int price, String image, int stockQuantity, int soldQuantity) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.price = price;
        this.image = image;
        this.stockQuantity = stockQuantity;
        this.soldQuantity = soldQuantity;
    }
}
