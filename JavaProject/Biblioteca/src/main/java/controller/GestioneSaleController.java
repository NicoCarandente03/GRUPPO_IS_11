package controller;

import database.GestorePersistenza;
import dto.AreaDTO;
import dto.SalaStudioDTO;
import eccezioni.BusinessException;
import entity.Area;
import entity.Bibliotecario;
import entity.Postazione;
import entity.SalaStudio;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class GestioneSaleController {

    /** Tipi di area che il bibliotecario puo' scegliere, come li elenca il piano di test. */
    public static final List<String> TIPI_AREA_AMMESSI =
            List.of("silenziosa", "consultazione", "lavoro di gruppo");

    // Tipo dell'area creata quando la sala non viene suddivisa.
    private static final String TIPO_AREA_PREDEFINITO = "generica";


    private static final int LUNGHEZZA_MASSIMA_NOME = 40;
    private static final int LUNGHEZZA_MASSIMA_DESCRIZIONE = 200;

    // il nome puo' contenere lettere, cifre, spazi, apostrofi e trattini
    private static final String NOME_AMMESSO = "[\\p{L}\\p{N} '\\-]+";

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static GestioneSaleController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private GestioneSaleController() {
        this.gestoreDB = new GestorePersistenza();
    }


    /**
     * Costruttore usato dai test, che passano un finto GestorePersistenza al
     * posto di quello reale. Non e' pubblico proprio per non essere usato
     * altrove: l'applicazione passa sempre da getInstance().
     */
    GestioneSaleController(GestorePersistenza gestoreDB) {
        this.gestoreDB = gestoreDB;
    }

    public static GestioneSaleController getInstance() {
        if (instance == null) {
            instance = new GestioneSaleController();
        }
        return instance;
    }

    /**
     * Crea una nuova sala studio, con le sue aree e postazioni.
     *
     * I controlli seguono l'ordine dei casi di test del piano funzionale, e ogni
     * fallimento solleva una BusinessException con il messaggio previsto.
     *
     * Il codice del bibliotecario non compare nella firma del diagramma, ma
     * serve a collegare la sala a chi la gestisce. E' un parametro temporaneo (!!!):
     * quando ci sara' il Log-in verra' letto dalla sessione.
     *
     * Una sala ha sempre almeno un'area: se il bibliotecario non ne indica nessuna, 
     * ne viene creata una sola, di tipo generica, che copre l'intera sala.
     * 
     * Le postazioni vengono distribuite equamente fra le aree
     */
    public SalaStudioDTO creazioneAulaStudio(String nome, String descrizione,
                                             String numPostazioniTotali, String orariApertura,
                                             List<String> tipiArea, String codiceBibliotecario) {

        verificaDatiValidi(nome, descrizione, numPostazioniTotali, orariApertura, tipiArea);

        int numPostazioni = leggiNumeroPostazioni(numPostazioniTotali);

        if (gestoreDB.trovaSalaPerNome(nome) != null) {
            throw new BusinessException("Errore, esiste già una sala con questo nome!");
        }

        SalaStudio sala = new SalaStudio(generaIdSala(), nome, descrizione,
                numPostazioni, orariApertura);

        Bibliotecario bibliotecario = gestoreDB.trovaPerCodiceIdentificativo(codiceBibliotecario);
        if (bibliotecario == null) {
            throw new BusinessException("Errore, il bibliotecario indicato non esiste!");
        }

        // Si assegna il bibliotecario alla sala, non la sala al bibliotecario.
        // Il bibliotecario arriva staccato dalla sessione, quindi toccare la sua
        // lista di sale la farebbe caricare fuori sessione e solleverebbe una
        // LazyInitializationException. La sala e' comunque il lato che porta la
        // chiave esterna, quindi basta questo perche' il legame venga salvato.
        sala.setBibliotecario(bibliotecario);

        creaAreeConPostazioni(sala, tipiArea, numPostazioni);

        gestoreDB.salva(sala);

        return convertiInDTO(sala);
    }

    /**
     * Verifica i dati inseriti, nell'ordine in cui il piano di test elenca le
     * classi di equivalenza: nome, descrizione, numero di postazioni, orario,
     * aree.
     */
    private void verificaDatiValidi(String nome, String descrizione, String numPostazioniTotali,
                                    String orariApertura, List<String> tipiArea) {

        if (nome == null || nome.trim().isEmpty()) {
            throw new BusinessException("Errore, il nome della sala non può essere vuoto!");
        }

        if (nome.trim().length() > LUNGHEZZA_MASSIMA_NOME) {
            throw new BusinessException("Errore, il nome inserito è troppo lungo!");
        }

        if (!nome.trim().matches(NOME_AMMESSO)) {
            throw new BusinessException("Errore, il formato del nome inserito non è valido!");
        }

        if (descrizione != null && descrizione.length() > LUNGHEZZA_MASSIMA_DESCRIZIONE) {
            throw new BusinessException("Errore, la descrizione inserita è troppo lunga!");
        }

        int numPostazioni = leggiNumeroPostazioni(numPostazioniTotali);

        if (numPostazioni < 1) {
            throw new BusinessException("Errore, il numero di postazioni deve essere maggiore di zero!");
        }

        verificaOrari(orariApertura);

        if (tipiArea != null) {
            for (String tipo : tipiArea) {
                if (tipo == null || !TIPI_AREA_AMMESSI.contains(tipo.trim().toLowerCase())) {
                    throw new BusinessException("Errore, tipo di area non riconosciuto");
                }
            }

            // Il modello di dominio vuole almeno una postazione per area, quindi
            // le postazioni non possono essere meno delle aree richieste.
            if (!tipiArea.isEmpty() && numPostazioni < tipiArea.size()) {
                throw new BusinessException(
                        "Errore, le postazioni non bastano per il numero di aree indicate!");
            }
        }
    }

    /**
     * Converte in numero il valore digitato per le postazioni.
     *
     * Il diagramma delle classi non dichiara il tipo di questo parametro, a
     * differenza degli altri: il valore arriva come lo ha scritto l'utente, e
     * riconoscere che "quaranta" non e' un numero fa parte della validazione,
     * quindi sta qui insieme agli altri controlli e non nel Boundary.
     */
    private int leggiNumeroPostazioni(String numPostazioniTotali) {
        try {
            return Integer.parseInt(numPostazioniTotali.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BusinessException("Errore, formato numero postazioni non valido!");
        }
    }

    /** Controlla che l'orario sia nella forma "08:00 - 20:00" e che sia coerente. */
    private void verificaOrari(String orariApertura) {
        if (orariApertura == null || !orariApertura.contains("-")) {
            throw new BusinessException("Errore, orari di apertura non coerenti!");
        }

        String[] parti = orariApertura.split("-");
        if (parti.length != 2) {
            throw new BusinessException("Errore, orari di apertura non coerenti!");
        }

        try {
            LocalTime apertura = LocalTime.parse(parti[0].trim());
            LocalTime chiusura = LocalTime.parse(parti[1].trim());

            if (!apertura.isBefore(chiusura)) {
                throw new BusinessException("Errore, orari di apertura non coerenti!");
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException("Errore, orari di apertura non coerenti!");
        }
    }

    /**
     * Genera l'identificativo della nuova sala nel formato: 
     * una S seguita da tre cifre
     *
     * Il progressivo si ricava dal massimo gia' assegnato, cosi' l'id resta
     * leggibile e resta valido anche se una sala viene cancellata.
     *
     */
    public String generaIdSala() {
        int massimo = 0;

        for (SalaStudio sala : gestoreDB.trovaTutteLeSale()) {
            massimo = Math.max(massimo, progressivo(sala.getIdSala(), "S"));
        }

        return String.format("S%03d", massimo + 1);
    }

    /**
     * Genera l'identificativo di una nuova area, con la stessa regola delle
     * sale ma con la lettera A: dopo A003 viene A004.
     *
     * La numerazione delle aree e' unica su tutte le sale, come nei dati di
     * prova, quindi il massimo va cercato fra tutte le aree esistenti.
     */
    private int massimoProgressivoAree() {
        int massimo = 0;

        for (Area area : gestoreDB.trovaTutteLeAree()) {
            massimo = Math.max(massimo, progressivo(area.getIdArea(), "A"));
        }

        return massimo;
    }

    /**
     * Estrae il numero da un identificativo come S007 o A012, oppure zero se
     * non segue quel formato, come i vecchi id casuali.
     *
     * Gli identificativi delle postazioni, del tipo P-A001-01, non seguono il
     * formato e vengono percio' ignorati.
     */
    private int progressivo(String identificativo, String prefisso) {
        if (identificativo == null || !identificativo.matches(prefisso + "\\d+")) {
            return 0;
        }
        return Integer.parseInt(identificativo.substring(prefisso.length()));
    }

    /**
     * Crea le aree della sala e distribuisce fra loro le postazioni, dando il
     * resto alla prima.
     *
     * Se non e' stata indicata nessuna area ne viene creata una sola, di tipo
     * generica, che contiene tutte le postazioni della sala.
     */
    private void creaAreeConPostazioni(SalaStudio sala, List<String> tipiArea,
                                       int numPostazioniTotali) {

        List<String> daCreare = new ArrayList<>();
        if (tipiArea == null || tipiArea.isEmpty()) {
            daCreare.add(TIPO_AREA_PREDEFINITO);
        } else {
            for (String tipo : tipiArea) {
                daCreare.add(tipo.trim().toLowerCase());
            }
        }

        int perArea = numPostazioniTotali / daCreare.size();
        int resto = numPostazioniTotali % daCreare.size();

        // Le aree proseguono la numerazione globale, le postazioni ripartono
        // da uno dentro ciascuna area: A004 avra' P-A004-01, P-A004-02 e cosi' via.
        int numeroArea = massimoProgressivoAree();

        for (int i = 0; i < daCreare.size(); i++) {
            numeroArea++;
            String idArea = String.format("A%03d", numeroArea);

            Area area = new Area(idArea, daCreare.get(i));
            sala.aggiungiArea(area);

            int quante = perArea + (i == 0 ? resto : 0);
            for (int j = 0; j < quante; j++) {
                area.aggiungiPostazione(new Postazione(String.format("P-%s-%02d", idArea, j + 1)));
            }
        }
    }

    /** Traduce la sala nel DTO che il Boundary sa mostrare. */
    private SalaStudioDTO convertiInDTO(SalaStudio sala) {
        List<AreaDTO> aree = new ArrayList<>();

        for (Area area : sala.getAree()) {
            aree.add(new AreaDTO(area.getIdArea(), area.getTipo(), area.getNumPostazioni()));
        }

        return new SalaStudioDTO(sala.getIdSala(), sala.getNome(), sala.getDescrizione(),
                sala.getNumPostazioniTotali(), sala.getOrariApertura(), aree);
    }
}
