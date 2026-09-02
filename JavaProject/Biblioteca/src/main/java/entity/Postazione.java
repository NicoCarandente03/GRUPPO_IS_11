package entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singola postazione prenotabile, contenuta in un'Area.
 *
 * Il campo isDisponibile indica se la postazione e' agibile (una postazione
 * guasta o fuori servizio non e' prenotabile), non se e' occupata in una certa
 * fascia oraria: quella informazione si ricava dalle prenotazioni collegate. Per
 * questo il costruttore di Prenotazione non lo tocca.
 */
@Entity
@Table(name = "Postazione")
public class Postazione {

    // L'id non e' generato dal database: lo assegna il chiamante, come previsto
    // dal costruttore del diagramma delle classi.
    @Id
    private String idPostazione;

    private boolean isDisponibile;

    @OneToMany(mappedBy = "postazione", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "idArea")
    private Area area;

    public Postazione() {
    }

    public Postazione(String idPostazione) {
        this.idPostazione = idPostazione;
        this.isDisponibile = true;
        // prenotazioni resta una lista vuota (molteplicita' 0..*)
    }

    public String getIdPostazione() {
        return idPostazione;
    }

    public Area getArea() {
        return area;
    }

    void setArea(Area area) {
        this.area = area;
    }

    public boolean isDisponibile() {
        return isDisponibile;
    }

    public void setDisponibile(boolean stato) {
        this.isDisponibile = stato;
    }

    public void rendiDisponibile() {
        this.isDisponibile = true;
    }

    /**
     * Verifica se la postazione e' prenotabile nella data e nella fascia oraria
     * indicate: deve essere agibile e non avere gia' una prenotazione attiva o
     * confermata sullo stesso slot.
     *
     */
    public boolean isLibera(LocalDate data, String fasciaOraria) {
        if (!isDisponibile) {
            return false;
        }
        for (Prenotazione prenotazione : prenotazioni) {
            if (prenotazione.occupa(data, fasciaOraria)) {
                return false;
            }
        }
        return true;
    }

    public List<Prenotazione> getPrenotazioni() {
        return Collections.unmodifiableList(prenotazioni);
    }

    void aggiungiPrenotazione(Prenotazione prenotazione) {
        if (prenotazione == null || prenotazioni.contains(prenotazione)) {
            return;
        }
        prenotazioni.add(prenotazione);
    }

    /** Prenotazioni non annullate ne' scadute a partire dalla data indicata. */
    public List<Prenotazione> getPrenotazioniAttive(LocalDate dataCorrente) {
        List<Prenotazione> attive = new ArrayList<>();
        for (Prenotazione prenotazione : prenotazioni) {
            if (prenotazione.isOccupante() && !prenotazione.getData().isBefore(dataCorrente)) {
                attive.add(prenotazione);
            }
        }
        return attive;
    }

    @Override
    public String toString() {
        return "Postazione " + idPostazione;
    }
}
