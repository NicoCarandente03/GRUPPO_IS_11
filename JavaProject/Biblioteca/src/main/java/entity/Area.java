package entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idArea;

    private String tipo;

    @OneToMany(mappedBy = "area", cascade = {CascadeType.PERSIST, CascadeType.MERGE}) //eliminando l'Area, le Postazioni non vengono distrutte
    private List<Postazione> postazioni = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_sala")
    private SalaStudio sala;

    public Area() {
    }

    //contenimento lasco tra Area e Postazione: nel costruttore del contenitore (Areaa) c'è un riferimento all'oggetto contenuto (Postazione)

    public Area(String tipo, SalaStudio sala) {
        this.tipo=tipo;
        this.sala=sala;
    }

    public void aggregazione(Postazione postazione) {
        //aggiunta della postazione alla lista
        this.postazioni.add(postazione);
        postazione.setArea(this); //per dire alla postazione in quale area si trova
    }

    public String getIdArea() {
        return idArea;
    }

    public void setIdArea(String idArea) {
        this.idArea = idArea;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<Postazione> getPostazioni() {
        return postazioni;
    }

    public void setPostazioni(List<Postazione> postazioni) {
        this.postazioni = postazioni;
    }

    public SalaStudio getSala() {
        return sala;
    }

    public void setSala(SalaStudio sala) {
        this.sala = sala;
    }
}
