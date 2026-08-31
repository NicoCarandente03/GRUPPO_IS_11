package dto;

import java.util.Collections;
import java.util.List;

/**
 * Dati di una sala studio nella forma che serve alle finestre.
 *
 * Porta con se' anche l'elenco delle aree, gia' pronto da mostrare, cosi' la
 * finestra non deve navigare la composizione fra sala e aree per contarne le
 * postazioni.
 */
public class SalaStudioDTO {

    private final String idSala;
    private final String nome;
    private final String descrizione;
    private final int numPostazioniTotali;
    private final String orariApertura;
    private final List<AreaDTO> aree;

    public SalaStudioDTO(String idSala, String nome, String descrizione, int numPostazioniTotali,
                         String orariApertura, List<AreaDTO> aree) {
        this.idSala = idSala;
        this.nome = nome;
        this.descrizione = descrizione;
        this.numPostazioniTotali = numPostazioniTotali;
        this.orariApertura = orariApertura;
        this.aree = aree;
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

    public List<AreaDTO> getAree() {
        return Collections.unmodifiableList(aree);
    }
}
