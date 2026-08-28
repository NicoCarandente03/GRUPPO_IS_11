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
import java.util.UUID;

/**
 * Area di una sala studio (silenziosa, consultazione, lavoro di gruppo).
 * Contiene le postazioni.
 *
 * Contenimento lasco fra Area e Postazione, cioe' aggregazione: eliminando
 * l'area le postazioni non vengono distrutte, quindi il cascade e' limitato a
 * PERSIST e MERGE e non c'e' orphanRemoval.
 */
@Entity
@Table(name = "Area")
public class Area {

    // L'id lo assegna il chiamante, come nel costruttore del diagramma.
    @Id
    private String idArea;

    private String tipo;

    @OneToMany(mappedBy = "area", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Postazione> postazioni = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "idSala")
    private SalaStudio sala;

    public Area() {
    }

    public Area(String idArea, String tipo) {
        this.idArea = idArea;
        this.tipo = tipo;
        // postazioni resta vuota, viene riempita con aggiungiPostazione
    }

    public String getIdArea() {
        return idArea;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public SalaStudio getSalaStudio() {
        return sala;
    }

    void setSalaStudio(SalaStudio sala) {
        this.sala = sala;
    }

    /**
     * Aggiunge una postazione all'area e mantiene allineato il riferimento
     * inverso. Nel diagramma delle classi il metodo si chiama cosi'.
     */
    public void aggiungiPostazione(Postazione postazione) {
        if (postazione == null || postazioni.contains(postazione)) {
            return;
        }
        postazioni.add(postazione);
        postazione.setArea(this);
    }

    public List<Postazione> getPostazioni() {
        return Collections.unmodifiableList(postazioni);
    }

    public int getNumPostazioni() {
        return postazioni.size();
    }

    /** Postazioni dell'area prenotabili nella data e nella fascia indicate. */
    public List<Postazione> getPostazioniDisponibili(LocalDate data, String fasciaOraria) {
        List<Postazione> disponibili = new ArrayList<>();
        for (Postazione postazione : postazioni) {
            if (postazione.isLibera(data, fasciaOraria)) {
                disponibili.add(postazione);
            }
        }
        return disponibili;
    }

    public Postazione trovaPostazione(String idPostazione) {
        for (Postazione postazione : postazioni) {
            if (postazione.getIdPostazione().equals(idPostazione)) {
                return postazione;
            }
        }
        return null;
    }

    /**
     * Porta il numero di postazioni al valore richiesto, come nel flusso
     * ModificaAulaStudio.
     *
     * In aggiunta crea nuove postazioni con un identificativo generato qui; in
     * riduzione stacca solo le postazioni prive di prenotazioni, per non perdere
     * lo storico. La cancellazione delle righe resta a PostazioneDAO, perche'
     * l'aggregazione non prevede orphanRemoval.
     */
    public void sincronizzaPostazioni(int numPostazioni) {
        while (postazioni.size() < numPostazioni) {
            aggiungiPostazione(new Postazione(UUID.randomUUID().toString()));
        }
        for (int i = postazioni.size() - 1; i >= 0 && postazioni.size() > numPostazioni; i--) {
            Postazione postazione = postazioni.get(i);
            if (postazione.getPrenotazioni().isEmpty()) {
                postazioni.remove(i);
                postazione.setArea(null);
            }
        }
    }

    /** Vero se resta almeno una postazione libera nello slot indicato. */
    public boolean isDisponibile(LocalDate data, String fasciaOraria) {
        return !getPostazioniDisponibili(data, fasciaOraria).isEmpty();
    }

    @Override
    public String toString() {
        return "Area " + idArea + " (" + tipo + ")";
    }
}
