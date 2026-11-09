package com.gymtrack.clases.domain.model;

import com.gymtrack.clases.infrastructure.persistence.DiasSemanaConverter;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Clase del gimnasio entendida como PLANTILLA semanal: se dicta ciertos días
 * a una hora fija (por ejemplo, Spinning los martes y jueves a las 18:00).
 *
 * <p>La clase no tiene fecha. Cada {@link Reserva} lleva la fecha concreta de la
 * sesión, y el aforo se cuenta por la combinación (clase, fecha), no por la
 * clase sola.</p>
 */
@Entity
@Table(name = "clase")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String instructor;

    @Convert(converter = DiasSemanaConverter.class)
    @Column(nullable = false)
    private Set<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false)
    private Integer aforo;

    @Column(nullable = false)
    private String sala;

    public Clase() {
    }

    /** Indica si la clase se dicta en la fecha dada (según su día de la semana). */
    public boolean seDictaEl(LocalDate fecha) {
        return fecha != null && dias.contains(DiaSemana.de(fecha));
    }

    /**
     * Primera fecha, desde {@code desde} (incluida), en la que se dicta la clase.
     * Devuelve null solo si la clase no tiene días configurados.
     */
    public LocalDate proximaSesionDesde(LocalDate desde) {
        for (int i = 0; i < 7; i++) {
            LocalDate candidata = desde.plusDays(i);
            if (seDictaEl(candidata)) {
                return candidata;
            }
        }
        return null;
    }

    // Getters
    public Long getId() { return id; }

    public String getNombre() { return nombre; }

    public String getInstructor() { return instructor; }

    public Set<DiaSemana> getDias() { return dias; }

    public LocalTime getHora() { return hora; }

    public Integer getDuracionMinutos() { return duracionMinutos; }

    public Integer getAforo() { return aforo; }

    public String getSala() { return sala; }

    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }

    public void setInstructor(String instructor) { this.instructor = instructor; }

    public void setDias(Set<DiaSemana> dias) {
        this.dias = (dias == null || dias.isEmpty()) ? EnumSet.noneOf(DiaSemana.class) : EnumSet.copyOf(dias);
    }

    public void setHora(LocalTime hora) { this.hora = hora; }

    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public void setAforo(Integer aforo) { this.aforo = aforo; }

    public void setSala(String sala) { this.sala = sala; }
}
