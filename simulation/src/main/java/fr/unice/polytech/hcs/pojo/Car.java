package fr.unice.polytech.hcs.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Car implements Serializable {

    @JsonProperty public String company;
    @JsonProperty public String city;
    @JsonProperty public String model;
    @JsonProperty public String numberPlate;
    @JsonProperty public Double price;

    public Car() {
        // empty
    }

    public Car(String company, String city, String model, String numberPlate, Double price) {
        this.company = company;
        this.city = city;
        this.model = model;
        this.price = price;
        this.numberPlate = numberPlate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        if (company != null ? !company.equals(car.company) : car.company != null) return false;
        if (city != null ? !city.equals(car.city) : car.city != null) return false;
        if (model != null ? !model.equals(car.model) : car.model != null) return false;
        if (numberPlate != null ? !numberPlate.equals(car.numberPlate) : car.numberPlate != null) return false;
        return price != null ? price.equals(car.price) : car.price == null;
    }

    @Override
    public int hashCode() {
        int result = company != null ? company.hashCode() : 0;
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (numberPlate != null ? numberPlate.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Car{" +
                "company='" + company + '\'' +
                ", city='" + city + '\'' +
                ", model='" + model + '\'' +
                ", numberPlate='" + numberPlate + '\'' +
                ", price=" + price +
                '}';
    }
}
