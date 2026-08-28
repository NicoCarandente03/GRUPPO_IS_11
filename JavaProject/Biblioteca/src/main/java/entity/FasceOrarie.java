package entity;

import eccezioni.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Elenco delle fasce orarie prenotabili e utilita' per il loro parsing.
 *
 * La specifica tratta la fascia oraria come una String ma non ne definisce i
 * valori ammessi. Qui si fissa l'elenco (sei blocchi da due ore, dalle 08:00
 * alle 20:00) e si concentra in un unico punto la conversione da testo a orario,
 * che serve a Prenotazione per calcolare check-in, annullamento e promemoria.
 */
public final class FasceOrarie {

    private static final String SEPARATORE = "-";

    private static final List<String> ELENCO = List.of(
            "08:00-10:00",
            "10:00-12:00",
            "12:00-14:00",
            "14:00-16:00",
            "16:00-18:00",
            "18:00-20:00");

    private FasceOrarie() {
        // classe di sole utilita', non va istanziata
    }

    public static List<String> getElenco() {
        return ELENCO;
    }

    public static boolean isValida(String fasciaOraria) {
        return fasciaOraria != null && ELENCO.contains(fasciaOraria.trim());
    }

    public static LocalTime oraInizio(String fasciaOraria) {
        return estrai(fasciaOraria, 0);
    }

    public static LocalTime oraFine(String fasciaOraria) {
        return estrai(fasciaOraria, 1);
    }

    /** Istante di inizio della fascia in una data specifica. */
    public static LocalDateTime inizio(LocalDate data, String fasciaOraria) {
        verificaData(data);
        return LocalDateTime.of(data, oraInizio(fasciaOraria));
    }

    /** Istante di fine della fascia in una data specifica. */
    public static LocalDateTime fine(LocalDate data, String fasciaOraria) {
        verificaData(data);
        return LocalDateTime.of(data, oraFine(fasciaOraria));
    }

    private static LocalTime estrai(String fasciaOraria, int posizione) {
        if (!isValida(fasciaOraria)) {
            throw new BusinessException("Fascia oraria non valida: " + fasciaOraria);
        }
        String[] parti = fasciaOraria.trim().split(SEPARATORE);
        try {
            return LocalTime.parse(parti[posizione]);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Fascia oraria non interpretabile: " + fasciaOraria, e);
        }
    }

    private static void verificaData(LocalDate data) {
        if (data == null) {
            throw new BusinessException("Data della prenotazione non specificata");
        }
    }
}
