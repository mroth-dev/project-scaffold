package com.example.scaffold.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Column(name = "line1")
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city")
    private String city;

    @Column(name = "region")
    private String region;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "country")
    private String country;

    @Column(name = "phone")
    private String phone;

    public boolean isComplete() {
        return hasText(line1) && hasText(city) && hasText(postcode) && hasText(country);
    }

    public Address copy() {
        Address copy = new Address();
        copy.setLine1(line1);
        copy.setLine2(line2);
        copy.setCity(city);
        copy.setRegion(region);
        copy.setPostcode(postcode);
        copy.setCountry(country);
        copy.setPhone(phone);
        return copy;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
