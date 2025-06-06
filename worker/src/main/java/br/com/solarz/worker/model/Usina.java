package br.com.solarz.worker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class Usina {

    public enum Priority {
        HIGH,
        NORMAL,
    };

    @Id
    private Long id;

    @ManyToOne
    private Credencial credencial;

    private Priority priority = Priority.NORMAL;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Usina usina)) return false;
        return Objects.equals(id, usina.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
