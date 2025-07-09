package br.com.solarz.worker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

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
        return averageTime + 30 * errorRate + 2 * pending;
    }

    @Override
    public int compareTo(@NotNull ApiScore o) {
        return Double.compare(calculate(), o.calculate());
    }
}
