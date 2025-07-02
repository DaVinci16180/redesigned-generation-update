package br.com.solarz.master.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private int updateAttempts = 0;
    private boolean updated = false;
}
