package external;

import java.time.LocalDate;

/**
 * Servizio esterno di invio notifiche, stereotipo external del diagramma di
 * design.
 *
 * E' un'interfaccia per isolare il sistema dal canale reale di invio e per
 * poterlo sostituire con un mock nei test, come chiedono i vincoli
 * architetturali della specifica.
 *
 * Nel diagramma i parametri idSala e data di invioPromemoria sono uniti in un
 * unico argomento per un refuso: qui sono separati.
 */
public interface ServizioDiNotifiche {

    void invioPromemoria(String destinatario, String idSala, LocalDate data, String fasciaOraria);

    void invioNotifica(String destinatario, String testo);
}
