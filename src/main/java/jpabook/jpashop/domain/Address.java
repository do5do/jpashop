package jpabook.jpashop.domain;

import lombok.Getter;

import javax.persistence.Embeddable;

@Embeddable // (어딘가에) 내장 가능한
@Getter // 값 타입(int, String, Integer처럼 단순히 값으로 사용하는 자바 기본타입이나 객체)은 변경 불가능하게 설계해야 한다. -> @Setter가 없음.
public class Address {
    private String city;
    private String street;
    private String zipcode;

    protected Address() { // jpa 스펙 상 entity나 임베디드 타입(@Embeddable) 기본 생성자를 생성해야 하고, protected로 설정하는 것이 안전하다.
    }

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
