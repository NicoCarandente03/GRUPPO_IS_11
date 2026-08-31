package eseguibile;

import database.DBManager;
import database.GestorePersistenza;
import entity.Area;
import entity.Bibliotecario;
import entity.Postazione;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;

import java.time.LocalDate;

/**
 * Popola il database con i dati di prova usati dal piano di test.
 *
 * Gli identificativi non sono inventati: sono gli stessi che compaiono nelle
 * tabelle dei casi di test della documentazione, cosi' i test si eseguono su
 * questi dati senza doverne definire altri.
 *
 * Va lanciato su un database vuoto, dopo MainCreaTabelle.
 */
public class DatiTestBiblioteca {

    private DatiTestBiblioteca() {
        // classe di sole utilita', non va istanziata
    }

    public static void popola(GestorePersistenza gestore) {

        // --- utenti ---
        Bibliotecario bibliotecario = new Bibliotecario(
                "Marco", "Esposito", "marco.esposito@unina.it", "biblio2026", "B1234");

        Studente primoStudente = new Studente(
                "Giulia", "Romano", "giulia.romano@studenti.unina.it", "studente1", "N46001234");

        Studente secondoStudente = new Studente(
                "Luca", "De Simone", "luca.desimone@studenti.unina.it", "studente2", "N46004321");

        // --- prima sala, con due aree ---
        SalaStudio salaLettura = new SalaStudio(
                "S001", "Sala Lettura A", "Sala silenziosa al Piano 1", 5, "08:00 - 20:00");
        bibliotecario.aggiungiSala(salaLettura);

        Area areaSilenziosa = new Area("A001", "silenziosa");
        salaLettura.aggiungiArea(areaSilenziosa);
        Postazione postazione1 = new Postazione("P-A001-01");
        Postazione postazione2 = new Postazione("P-A001-02");
        Postazione postazione3 = new Postazione("P-A001-03");
        areaSilenziosa.aggiungiPostazione(postazione1);
        areaSilenziosa.aggiungiPostazione(postazione2);
        areaSilenziosa.aggiungiPostazione(postazione3);

        Area areaConsultazione = new Area("A002", "consultazione");
        salaLettura.aggiungiArea(areaConsultazione);
        Postazione postazione4 = new Postazione("P-A002-01");
        Postazione postazione5 = new Postazione("P-A002-02");
        areaConsultazione.aggiungiPostazione(postazione4);
        areaConsultazione.aggiungiPostazione(postazione5);

        // --- seconda sala, serve a provare il filtro per sala nello storico ---
        SalaStudio salaInformatica = new SalaStudio(
                "S002", "Sala Informatica", "Postazioni con computer", 2, "09:00 - 18:00");
        bibliotecario.aggiungiSala(salaInformatica);

        Area areaComputer = new Area("A003", "lavoro di gruppo");
        salaInformatica.aggiungiArea(areaComputer);
        Postazione postazione6 = new Postazione("P-A003-01");
        Postazione postazione7 = new Postazione("P-A003-02");
        areaComputer.aggiungiPostazione(postazione6);
        areaComputer.aggiungiPostazione(postazione7);

        // --- prenotazioni, una per ogni stato che serve ai casi di test ---
        LocalDate fraUnaSettimana = LocalDate.now().plusDays(7);
        LocalDate oggi = LocalDate.now();
        LocalDate ieri = LocalDate.now().minusDays(1);

        // P001: attiva e lontana nel tempo, quindi annullabile
        Prenotazione attiva = new Prenotazione(
                "P001", fraUnaSettimana, "14:00-16:00", primoStudente, postazione1);

        // P002: gia' annullata
        Prenotazione annullata = new Prenotazione(
                "P002", fraUnaSettimana, "10:00-12:00", primoStudente, postazione2);
        annullata.aggiornaStatoPrenotazione(Prenotazione.ANNULLATA);

        // P003: scaduta perche' il check-in non e' stato fatto
        Prenotazione scaduta = new Prenotazione(
                "P003", ieri, "08:00-10:00", primoStudente, postazione3);
        scaduta.aggiornaStatoPrenotazione(Prenotazione.SCADUTA);

        // P004: attiva ma per oggi, quindi il limite di annullamento e' superato
        Prenotazione imminente = new Prenotazione(
                "P004", oggi, "08:00-10:00", primoStudente, postazione4);

        // P005: di un altro studente e in un'altra sala, serve allo storico filtrato
        Prenotazione altroStudente = new Prenotazione(
                "P005", ieri, "10:00-12:00", secondoStudente, postazione6);
        altroStudente.aggiornaStatoPrenotazione(Prenotazione.CONFERMATA);

        // L'ordine conta: prima gli oggetti a cui gli altri fanno riferimento.
        // Le aree e le postazioni arrivano in cascata dalle rispettive sale.
        gestore.salvaTutti(
                bibliotecario,
                salaLettura,
                salaInformatica,
                primoStudente,
                secondoStudente,
                attiva,
                annullata,
                scaduta,
                imminente,
                altroStudente);
    }

    /** Vero se i dati di prova sono gia' stati inseriti. */
    private static boolean datiGiaPresenti() {
        return DBManager.getInstance()
                .esegui(em -> em.find(Bibliotecario.class, "B1234")) != null;
    }

    public static void main(String[] args) {
        if (datiGiaPresenti()) {
            System.out.println("I dati di prova sono gia' presenti, non li reinserisco.");
            DBManager.getInstance().closeConnection();
            return;
        }

        System.out.println("Inserimento dei dati di prova in corso...");

        try {
            popola(new GestorePersistenza());
            System.out.println("Dati di prova inseriti.");
            System.out.println("  bibliotecario B1234");
            System.out.println("  studenti N46001234 e N46004321");
            System.out.println("  sale S001 Sala Lettura A e S002 Sala Informatica");
            System.out.println("  prenotazioni da P001 a P005");
        } catch (RuntimeException e) {
            System.out.println("Inserimento non riuscito: " + e.getMessage());
            System.out.println("Se i dati sono gia' presenti, svuota le tabelle e rilancia.");
        }

        DBManager.getInstance().closeConnection();
    }
}
