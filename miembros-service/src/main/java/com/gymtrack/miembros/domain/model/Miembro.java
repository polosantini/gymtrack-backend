package com.gymtrack.miembros.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="miembro")
public class Miembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    public Miembro(){}

    @PrePersist
    public void inicializarFechaRegistro() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() { return this.id; }

    public String getNombre() { return this.nombre; }

    public String getEmail() { return this.email; }

    public LocalDateTime getFechaRegistro() { return this.fechaRegistro; }

    public Membresia getMembresia() { return this.membresia; }


    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }

    public void setEmail(String email) { this.email = email; }

    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public void setMembresia(Membresia membresia) { this.membresia = membresia; }

}
