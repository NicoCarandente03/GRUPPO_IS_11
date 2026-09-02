package entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bibliotecario che gestisce le sale studio. Estende Utente e aggiunge il codice
 * identificativo, che e' anche la chiave primaria della tabella.
 *
 */
@Entity
@Table(name = "Bibliotecario")
public class Bibliotecario extends Utente {

    @Id
    private String codiceIdentificativo;

    // Associazione 1 - 0..* con SalaStudio
    @OneToMany(mappedBy = "bibliotecario", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<SalaStudio> sale = new ArrayList<>();

    public Bibliotecario() {
        super(RUOLO_BIBLIOTECARIO);
    }

    public Bibliotecario(String nome, String cognome, String email, String password,
                         String codiceIdentificativo) {
        super(nome, cognome, email, password, RUOLO_BIBLIOTECARIO);
        this.codiceIdentificativo = codiceIdentificativo;
        // sale resta una lista vuota (molteplicita' 0..*)
    }

    public String getCodiceIdentificativo() {
        return codiceIdentificativo;
    }

    /**
     * Sale gestite dal bibliotecario. Serve a
     * GestioneSaleController.getElencoSaleGestite(), che nel flusso
     * EliminazioneSalaStudio parte dal codice identificativo.
     */
    public List<SalaStudio> getSaleGestite() {
        return Collections.unmodifiableList(sale);
    }

    /** Mantiene allineati i due lati dell'associazione. */
    public void aggiungiSala(SalaStudio sala) {
        if (sala == null || sale.contains(sala)) {
            return;
        }
        sale.add(sala);
        sala.setBibliotecario(this);
    }

    public void rimuoviSala(SalaStudio sala) {
        if (sala != null && sale.remove(sala)) {
            sala.setBibliotecario(null);
        }
    }
}
