package model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ApiScore implements Comparable<ApiScore> {

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

    public double calculate() {
        return averageTime * errorRate;
    }

    @Override
    public int compareTo(ApiScore o) {
        return Double.compare(calculate(), o.calculate());
    }
}
