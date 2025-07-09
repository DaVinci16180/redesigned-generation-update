package br.com.solarz.master.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ApiScore {

    @Id
    Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private Api api;

    private double averageTime = .0;
    private double errorRate = .0;
    private double pending = .0; // percentual

    public ApiScore(Api api) {
        this.id = api.getId();
        this.api = api;
    }

    public ApiScore() {

    }
}
