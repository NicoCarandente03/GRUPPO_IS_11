package entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class SalaStudio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String idSala;

    private String nome;
    private String descrizione;
    private String orariApertura;
    private int numPostazioniTotali;

    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL)
    private List<Area> aree = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "codice_bibliotecario")
    private Bibliotecario bibliotecario;

    public SalaStudio() {
    }

    //contenimento stretto tra SalaStudio e Area: la creazione degli oggetti contenuti (Area) avviene all'interno del contenitore (SalaStudio)

    public SalaStudio(String nome, String descrizione, String orariApertura, int numPostazioniTotali, Bibliotecario bibliotecario, String tipo) {
        this.nome=nome;
        this.descrizione=descrizione;
        this.orariApertura=orariApertura;
        this.numPostazioniTotali=numPostazioniTotali;
        this.bibliotecario=bibliotecario;

        //creazione interna con passaggio del riferimento 'this' per la chiave esterna
        Area area=new Area(tipo, this);
        //aggiunta dell'oggetto alla lista
        this.aree.add(area);
    }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getOrariApertura() {
        return orariApertura;
    }

    public void setOrariApertura(String orariApertura) {
        this.orariApertura = orariApertura;
    }

    public int getNumPostazioniTotali() {
        return numPostazioniTotali;
    }

    public void setNumPostazioniTotali(int numPostazioniTotali) {
        this.numPostazioniTotali = numPostazioniTotali;
    }

    public List<Area> getAree() {
        return aree;
    }

    public void setAree(List<Area> aree) {
        this.aree = aree;
    }

    public Bibliotecario getBibliotecario() {
        return bibliotecario;
    }

    public void setBibliotecario(Bibliotecario bibliotecario) {
        this.bibliotecario = bibliotecario;
    }
}
