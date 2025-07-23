package model;

import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NORMAL;
    private int updateAttempts = 0;
    private boolean updated = false;

    public void incrementUpdateAttempts() {
        updateAttempts++;
    }

    public void reset() {
        updateAttempts = 0;
        updated = false;
    }

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
