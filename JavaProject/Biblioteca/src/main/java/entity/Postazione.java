package entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Postazione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idPostazione;

    private boolean isDisponibile;

    @OneToMany(mappedBy = "postazione", cascade = CascadeType.ALL)
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_area")
    private Area area;

    public Postazione() {
    }

    public Postazione(boolean isDisponibile) {
        this.isDisponibile=isDisponibile;
        //this.prenotazioni è un ArrayList vuoto (associazione con molteplicità 0..*)
    }

    public String getIdPostazione() {
        return idPostazione;
    }

    public void setIdPostazione(String idPostazione) {
        this.idPostazione = idPostazione;
    }

    public boolean isDisponibile() {
        return isDisponibile;
    }

    public void setDisponibile(boolean disponibile) {
        isDisponibile = disponibile;
    }

    public List<Prenotazione> getPrenotazioni() {
        return prenotazioni;
    }

    public void setPrenotazioni(List<Prenotazione> prenotazioni) {
        this.prenotazioni = prenotazioni;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }
}
