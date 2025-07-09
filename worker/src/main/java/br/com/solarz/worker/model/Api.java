package br.com.solarz.worker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Api {

    @Id
    private Long id;

    private String name;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Api api)) return false;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
