package entity;

import eccezioni.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
 * Sala studio della biblioteca. E' il contenitore stretto delle aree, cioe' una
 * composizione: eliminando la sala vengono eliminate anche le sue aree, quindi
 * cascade ALL con orphanRemoval.
 *
 * Le interrogazioni sulla disponibilita' sono risolte navigando le associazioni
 * fra le entity, senza passare dai DAO, come richiede la regola per cui le
 * Entity non conoscono il livello di persistenza. Perche' funzionino, il grafo
 * deve essere gia' caricato: se ne occupa SalaStudioDAO.
 */
@Entity
@Table(name = "SalaStudio")
public class SalaStudio {

    // L'id lo assegna GestioneSaleController.generaIdSala, come da diagramma.
    @Id
    private String idSala;

    @Column(unique = true)
    private String nome;

    private String descrizione;
    private int numPostazioniTotali;
    private String orariApertura;

    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Area> aree = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "codiceIdentificativo")
    private Bibliotecario bibliotecario;

    public SalaStudio() {
    }

    public SalaStudio(String idSala, String nome, String descrizione, int numPostazioni,
                      String orari) {
        this.idSala = idSala;
        this.nome = nome;
        this.descrizione = descrizione;
        this.numPostazioniTotali = numPostazioni;
        this.orariApertura = orari;
        // le aree vengono aggiunte dal controller con aggiungiArea
    }

    public String getIdSala() {
        return idSala;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public int getNumPostazioniTotali() {
        return numPostazioniTotali;
    }

    public String getOrariApertura() {
        return orariApertura;
    }

    public Bibliotecario getBibliotecario() {
        return bibliotecario;
    }

    /**
     * Assegna il bibliotecario che gestisce la sala.
     *
     * E' questo il lato che possiede l'associazione, cioe' quello che porta la
     * chiave esterna: assegnare qui e' sufficiente perche' il legame venga
     * salvato. La lista sul bibliotecario e' il lato inverso, di sola lettura.
     */
    public void setBibliotecario(Bibliotecario bibliotecario) {
        this.bibliotecario = bibliotecario;
    }

    public List<Area> getAree() {
        return Collections.unmodifiableList(aree);
    }

    public void aggiungiArea(Area area) {
        if (area == null || aree.contains(area)) {
            return;
        }
        aree.add(area);
        area.setSalaStudio(this);
    }

    public void rimuoviArea(String idArea) {
        Area area = trovaArea(idArea);
        if (area == null) {
            throw new BusinessException("Area non presente nella sala: " + idArea);
        }
        aree.remove(area);
        area.setSalaStudio(null);
    }

    public Area trovaArea(String idArea) {
        for (Area area : aree) {
            if (area.getIdArea().equals(idArea)) {
                return area;
            }
        }
        return null;
    }

    public void aggiornaDati(String nome, String descrizione, int numPostazioniTotali,
                             String orariApertura) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.numPostazioniTotali = numPostazioniTotali;
        this.orariApertura = orariApertura;
    }

    /**
     * Postazioni della sala prenotabili nella data e nella fascia indicate.
     * Il diagramma non indica il tipo degli elementi della lista: sono Postazione
     * perche' e' il dato che serve al flusso di prenotazione.
     */
    public List<Postazione> getDisponibilitaSalaStudio(LocalDate data, String fasciaOraria) {
        List<Postazione> disponibili = new ArrayList<>();
        for (Area area : aree) {
            disponibili.addAll(area.getPostazioniDisponibili(data, fasciaOraria));
        }
        return disponibili;
    }

    /**
     * Vero se resta almeno una postazione libera. Corrisponde al messaggio
     * isDisponibile del flusso ConsultazioneDisponibilitaSaleStudio.
     */
    public boolean isDisponibile(LocalDate data, String fasciaOraria) {
        return !getDisponibilitaSalaStudio(data, fasciaOraria).isEmpty();
    }

    /**
     * Fasce orarie in cui la sala ha ancora postazioni libere nella data
     * indicata.
     */
    public List<String> getFasceOrarieDisponibili(LocalDate data) {
        List<String> disponibili = new ArrayList<>();
        for (String fasciaOraria : FasceOrarie.getElenco()) {
            if (isDisponibile(data, fasciaOraria)) {
                disponibili.add(fasciaOraria);
            }
        }
        return disponibili;
    }

    public List<Area> getAreeDisponibili(LocalDate data, String fasciaOraria) {
        List<Area> disponibili = new ArrayList<>();
        for (Area area : aree) {
            if (area.isDisponibile(data, fasciaOraria)) {
                disponibili.add(area);
            }
        }
        return disponibili;
    }

    public List<Postazione> getPostazioniDisponibili(LocalDate data, String fasciaOraria,
                                                     String idArea) {
        Area area = trovaArea(idArea);
        if (area == null) {
            return new ArrayList<>();
        }
        return area.getPostazioniDisponibili(data, fasciaOraria);
    }

    /** Vero se l'area indicata ha ancora almeno una postazione libera. */
    public boolean verificaDisponibilitaArea(String idArea, LocalDate data, String fasciaOraria) {
        Area area = trovaArea(idArea);
        return area != null && area.isDisponibile(data, fasciaOraria);
    }

    /** Vero se la postazione indicata e' libera nello slot richiesto. */
    public boolean verificaDisponibilitaPostazione(String idPostazione, LocalDate data,
                                                   String fasciaOraria) {
        Postazione postazione = trovaPostazione(idPostazione);
        return postazione != null && postazione.isLibera(data, fasciaOraria);
    }

    /**
     * Controllo complessivo prima di creare una prenotazione: fascia ammessa,
     * area della sala, postazione dell'area e ancora libera.
     */
    public boolean validaDisponibilita(LocalDate data, String fasciaOraria, String idArea,
                                       String idPostazione) {
        if (!FasceOrarie.isValida(fasciaOraria)) {
            return false;
        }
        Area area = trovaArea(idArea);
        if (area == null) {
            return false;
        }
        Postazione postazione = area.trovaPostazione(idPostazione);
        return postazione != null && postazione.isLibera(data, fasciaOraria);
    }

    /** Prenotazioni non annullate ne' scadute a partire dalla data corrente. */
    public List<Prenotazione> getPrenotazioniAttive(LocalDate dataCorrente) {
        List<Prenotazione> attive = new ArrayList<>();
        for (Area area : aree) {
            for (Postazione postazione : area.getPostazioni()) {
                attive.addAll(postazione.getPrenotazioniAttive(dataCorrente));
            }
        }
        return attive;
    }

    public Postazione trovaPostazione(String idPostazione) {
        for (Area area : aree) {
            Postazione postazione = area.trovaPostazione(idPostazione);
            if (postazione != null) {
                return postazione;
            }
        }
        return null;
    }

    /**
     * Numero di postazioni effettivamente registrate nelle aree.
     *
     * numPostazioniTotali e' il valore dichiarato dal bibliotecario alla
     * creazione e puo' non coincidere con le postazioni gia' inserite. Serve a
     * MonitoraggioSaleController per il tasso di occupazione.
     */
    public int getNumPostazioniEffettive() {
        int totale = 0;
        for (Area area : aree) {
            totale += area.getNumPostazioni();
        }
        return totale;
    }

    @Override
    public String toString() {
        return nome + " (" + idSala + ")";
    }
}
