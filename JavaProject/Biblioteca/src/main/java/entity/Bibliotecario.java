package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Bibliotecario {

    @Id
    private String codiceIdentificativo;

    private String nome;
    private String cognome;
    private String email;
    private String password;

    @OneToMany(mappedBy = "bibliotecario", cascade = CascadeType.ALL)
    private List<SalaStudio> sale = new ArrayList<>();

    public Bibliotecario() {
    }

    public Bibliotecario(String nome, String cognome, String email, String password, String codiceIdentificativo) {
        this.nome=nome;
        this.cognome=cognome;
        this.email=email;
        this.password=password;
        this.codiceIdentificativo=codiceIdentificativo;
        //this.sale è un ArrayList vuoto (associazione con molteplicità 0..*)
    }

    public String getCodiceIdentificativo() {
        return codiceIdentificativo;
    }

    public void setCodiceIdentificativo(String codiceIdentificativo) {
        this.codiceIdentificativo = codiceIdentificativo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<SalaStudio> getSale() {
        return sale;
    }

    public void setSale(List<SalaStudio> sale) {
        this.sale = sale;
    }
}
