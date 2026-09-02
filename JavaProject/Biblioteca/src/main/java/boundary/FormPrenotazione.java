package boundary;

import controller.GestionePrenotazioneController;
import dto.AreaDTO;
import entity.FasceOrarie;
import entity.SalaStudio;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Finestra principale per permettere allo studente di consultare le sale
 * ed effettuare una nuova prenotazione in biblioteca.
 *
 * Pattern BCED: Questa classe appartiene al livello Boundary (Presentazione).
 * Il suo unico scopo è raccogliere input dall'utente e mostrare output.
 * NON contiene logica di business e NON interroga il database direttamente:
 * delega ogni operazione al GestionePrenotazioneController.
 */
public class FormPrenotazione implements BoundaryPrenotazione {

    // Riferimento al Singleton del Controller per applicare il pattern architetturale
    private final GestionePrenotazioneController controller = GestionePrenotazioneController.getInstance();

    // Dati di sessione (ricevuti al momento dell'apertura della finestra)
    private final String matricolaStudente;

    // Voci fisse delle tendine, riconosciute anche in fase di lettura
    private static final String NESSUNA_SALA = "--- Cerca prima le sale ---";
    private static final String NESSUN_ORARIO = "--- Verifica prima gli orari ---";
    private static final String NESSUNA_FASCIA = "Nessuna fascia disponibile";
    private static final String POSTAZIONE_AUTOMATICA = "Assegnazione automatica";
    private static final String AREA_QUALSIASI = "Tutte le aree";

    // Componenti grafici (Swing)
    private JPanel pannelloPrincipale;

    // Campi di input
    private JTextField campoData;
    private JComboBox<String> sceltaFasciaRicerca; // Per cercare le sale disponibili
    private JComboBox<String> sceltaIdSala;
    private JComboBox<String> sceltaIdArea;
    private JComboBox<String> sceltaIdPostazione;
    private JComboBox<String> sceltaFasciaOraria; // Per scegliere l'orario della prenotazione

    // Identificativi delle sale caricate, nello stesso ordine della tendina:
    // la tendina mostra "S001 - Sala Lettura A" ma al Controller va il solo id.
    private final List<String> idSaleCaricate = new ArrayList<>();

    // Stessa cosa per le aree, che nella tendina hanno "Tutte le aree" in testa
    private final List<String> idAreeCaricate = new ArrayList<>();

    // Vero mentre una tendina viene ricaricata da codice, per non far scattare
    // i listener sulle voci inserite a una a una.
    private boolean caricamentoTendine;

    // Bottoni per i 3 flussi operativi
    private JButton bottoneCercaSale;
    private JButton bottoneVerificaOrari;
    private JButton bottonePrenota;
    private JLabel etichettaEsito;

    /**
     * Costruttore: riceve la matricola dello studente loggato.
     */
    public FormPrenotazione(String matricolaStudente) {
        this.matricolaStudente = matricolaStudente;

        // Metodo che costruisce fisicamente i pannelli e i bottoni
        costruisciInterfaccia();

        // Associazione dei "Listener" ai bottoni (quando l'utente clicca, scatta il metodo corrispondente)

        bottoneCercaSale.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consultazioneDisponibilitaSaleStudio();
            }
        });

        bottoneVerificaOrari.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verificaFasceOrarie();
            }
        });

        bottonePrenota.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                effettuaPrenotazione();
            }
        });

        // Cambiando l'orario cambiano sia le aree con posti liberi sia le
        // postazioni, quindi si ricaricano entrambe le tendine.
        sceltaFasciaOraria.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!caricamentoTendine) {
                    aggiornaAreeEPostazioni();
                }
            }
        });

        // Cambiando l'area basta rifiltrare le postazioni
        sceltaIdArea.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!caricamentoTendine) {
                    aggiornaPostazioniLibere();
                }
            }
        });
    }


    private void costruisciInterfaccia() {
        // La data e' l'unico dato che resta da scrivere a mano: parte compilata
        // con il giorno successivo, cosi' il formato atteso e' sotto gli occhi.
        campoData = new JTextField(LocalDate.now().plusDays(1).toString(), 14);

        // Le fasce ammesse sono sei e fisse, quindi si scelgono e non si digitano
        sceltaFasciaRicerca = new JComboBox<>();
        for (String fascia : FasceOrarie.getElenco()) {
            sceltaFasciaRicerca.addItem(fascia);
        }

        // Le tendine che dipendono dal database partono con un messaggio di default
        sceltaIdSala = new JComboBox<>();
        sceltaIdSala.addItem(NESSUNA_SALA);

        sceltaFasciaOraria = new JComboBox<>();
        sceltaFasciaOraria.addItem(NESSUN_ORARIO);

        sceltaIdArea = new JComboBox<>();
        sceltaIdArea.addItem(AREA_QUALSIASI);

        sceltaIdPostazione = new JComboBox<>();
        sceltaIdPostazione.addItem(POSTAZIONE_AUTOMATICA);

        bottoneCercaSale = new JButton("Cerca Sale Disponibili");
        bottoneVerificaOrari = new JButton("Verifica Orari Sala");
        bottonePrenota = new JButton("Conferma Prenotazione");
        etichettaEsito = new JLabel(" ");

        // Creazione di una griglia a 2 colonne per allineare le etichette (Label) e i campi
        JPanel campi = new JPanel(new GridLayout(6, 2, 8, 12));

        // - SEZIONE 1: Consultazione
        campi.add(new JLabel("Data (YYYY-MM-DD):"));
        campi.add(campoData);

        campi.add(new JLabel("Fascia Oraria (per cercare sale):"));
        JPanel pannelloCercaSale = new JPanel(new BorderLayout(5, 0));
        pannelloCercaSale.add(sceltaFasciaRicerca, BorderLayout.CENTER);
        pannelloCercaSale.add(bottoneCercaSale, BorderLayout.EAST);
        campi.add(pannelloCercaSale);

        // - SEZIONE 2: Dettagli Prenotazione
        campi.add(new JLabel("Sala:"));
        campi.add(sceltaIdSala);

        campi.add(new JLabel("Seleziona Orario:"));
        JPanel pannelloOrari = new JPanel(new BorderLayout(5, 0));
        pannelloOrari.add(sceltaFasciaOraria, BorderLayout.CENTER);
        pannelloOrari.add(bottoneVerificaOrari, BorderLayout.EAST);
        campi.add(pannelloOrari);

        campi.add(new JLabel("Area:"));
        campi.add(sceltaIdArea);

        campi.add(new JLabel("Postazione:"));
        campi.add(sceltaIdPostazione);

        // Header della finestra
        JPanel alto = new JPanel(new GridLayout(2, 1, 0, 4));
        alto.add(new JLabel("Area Prenotazioni - Studente: " + matricolaStudente));
        alto.add(new JLabel("Scegli data e fascia, cerca le sale, verifica gli orari, poi conferma. "
                + "Puoi restringere a un'area; con Assegnazione automatica il posto lo sceglie il sistema."));

        // Footer della finestra
        JPanel basso = new JPanel(new BorderLayout(8, 4));
        basso.add(bottonePrenota, BorderLayout.WEST);
        basso.add(etichettaEsito, BorderLayout.CENTER);

        // Assemblaggio finale nel pannello principale
        pannelloPrincipale = new JPanel(new BorderLayout(10, 15));
        pannelloPrincipale.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Padding
        pannelloPrincipale.add(alto, BorderLayout.NORTH);
        pannelloPrincipale.add(campi, BorderLayout.CENTER);
        pannelloPrincipale.add(basso, BorderLayout.SOUTH);
    }


    // IMPLEMENTAZIONE METODI DELL'INTERFACCIA BOUNDARY

    @Override
    public void consultazioneDisponibilitaSaleStudio() {
        String dataTesto = campoData.getText().trim();
        String fascia = String.valueOf(sceltaFasciaRicerca.getSelectedItem());

        if (dataTesto.isEmpty()) {
            mostraErrore("Inserisci la Data per vedere le sale disponibili.");
            return;
        }

        try {
            // Conversione della stringa nel tipo LocalDate richiesto dal backend
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Chiamata al Controller
            List<SalaStudio> sale = controller.consultazioneDisponibilitaSaleStudio(dataScelta, fascia);

            // Il risultato non finisce piu' in un pop-up: riempie la tendina
            // delle sale, cosi' l'id non va ricopiato a mano.
            caricamentoTendine = true;
            sceltaIdSala.removeAllItems();
            idSaleCaricate.clear();

            if (sale == null || sale.isEmpty()) {
                sceltaIdSala.addItem(NESSUNA_SALA);
                caricamentoTendine = false;
                mostraMessaggio("Non ci sono sale registrate o disponibili nel sistema.");
                return;
            }

            for (SalaStudio sala : sale) {
                sceltaIdSala.addItem(sala.getIdSala() + " - " + sala.getNome());
                idSaleCaricate.add(sala.getIdSala());
            }
            caricamentoTendine = false;

            etichettaEsito.setText(sale.size() + " sale trovate, scegline una e verifica gli orari.");
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD (es. 2026-06-15).");
        }
    }

    /**
     * Identificativo della sala scelta nella tendina, oppure null se non ne e'
     * ancora stata caricata nessuna.
     */
    private String idSalaSelezionata() {
        int scelta = sceltaIdSala.getSelectedIndex();

        if (scelta < 0 || scelta >= idSaleCaricate.size()) {
            return null;
        }
        return idSaleCaricate.get(scelta);
    }

    /**
     * Identificativo dell'area scelta nella tendina, oppure null se e' ancora
     * selezionata la voce "Tutte le aree".
     */
    private String idAreaSelezionata() {
        int scelta = sceltaIdArea.getSelectedIndex();

        // la prima voce non e' un'area, e' la scelta di non filtrare
        if (scelta <= 0 || scelta > idAreeCaricate.size()) {
            return null;
        }
        return idAreeCaricate.get(scelta - 1);
    }

    /**
     * Vero se sala, data e fascia sono state scelte tutte e tre, cioe' se ha
     * senso interrogare il Controller sulle disponibilita'.
     */
    private boolean sceltaCompleta() {
        String fascia = String.valueOf(sceltaFasciaOraria.getSelectedItem());
        return idSalaSelezionata() != null
                && !fascia.startsWith("---") && !fascia.equals(NESSUNA_FASCIA);
    }

    /**
     * Ricarica la tendina delle aree con quelle che hanno ancora posti liberi,
     * e a seguire quella delle postazioni.
     *
     * Le aree al completo non compaiono: sceglierle darebbe un elenco vuoto.
     */
    private void aggiornaAreeEPostazioni() {
        caricamentoTendine = true;
        sceltaIdArea.removeAllItems();
        sceltaIdArea.addItem(AREA_QUALSIASI);
        idAreeCaricate.clear();

        if (sceltaCompleta()) {
            try {
                LocalDate dataScelta = LocalDate.parse(campoData.getText().trim());
                String fascia = String.valueOf(sceltaFasciaOraria.getSelectedItem());

                for (AreaDTO area
                        : controller.visualizzazioneAreeDisponibili(idSalaSelezionata(), dataScelta, fascia)) {
                    sceltaIdArea.addItem(area.getIdArea() + " - " + area.getTipo()
                            + " (" + area.getNumPostazioni() + " liberi)");
                    idAreeCaricate.add(area.getIdArea());
                }
            } catch (DateTimeParseException e) {
                // la data sbagliata viene gia' segnalata dagli altri bottoni
            }
        }

        caricamentoTendine = false;
        aggiornaPostazioniLibere();
    }

    /**
     * Ricarica la tendina delle postazioni con quelle ancora libere nella sala,
     * nella data e nella fascia scelte, ristrette all'area selezionata. La
     * prima voce lascia la scelta al sistema.
     */
    private void aggiornaPostazioniLibere() {
        caricamentoTendine = true;
        sceltaIdPostazione.removeAllItems();
        sceltaIdPostazione.addItem(POSTAZIONE_AUTOMATICA);

        if (sceltaCompleta()) {
            try {
                LocalDate dataScelta = LocalDate.parse(campoData.getText().trim());
                String fascia = String.valueOf(sceltaFasciaOraria.getSelectedItem());

                for (String idPostazione : controller.visualizzazionePostazioniLibere(
                        idSalaSelezionata(), dataScelta, fascia, idAreaSelezionata())) {
                    sceltaIdPostazione.addItem(idPostazione);
                }
            } catch (DateTimeParseException e) {
                // la data sbagliata viene gia' segnalata dagli altri bottoni
            }
        }

        caricamentoTendine = false;
    }

    @Override
    public void verificaFasceOrarie() {
        String dataTesto = campoData.getText().trim();
        String idSala = idSalaSelezionata();

        if (dataTesto.isEmpty() || idSala == null) {
            mostraErrore("Inserisci la Data e cerca le sale, poi scegline una dalla tendina.");
            return;
        }

        try {
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Chiamata al Controller per estrarre le fasce non esaurite
            List<String> fasce = controller.visualizzazioneFasceOrarieDisponibili(idSala, dataScelta);

            // Svuota la tendina e la ripopola con i dati freschi dal DB
            caricamentoTendine = true;
            sceltaFasciaOraria.removeAllItems();

            if (fasce.isEmpty()) {
                sceltaFasciaOraria.addItem(NESSUNA_FASCIA);
                caricamentoTendine = false;
                aggiornaAreeEPostazioni();
                mostraErrore("La sala selezionata è al completo (sold-out) per la data indicata.");
                return;
            }

            for (String f : fasce) {
                sceltaFasciaOraria.addItem(f);
            }
            caricamentoTendine = false;

            // Scelto l'orario si sa anche quali aree e quali posti restano liberi
            aggiornaAreeEPostazioni();
            etichettaEsito.setText("Fasce orarie, aree e postazioni libere caricate.");
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD.");
        }
    }

    @Override
    public void effettuaPrenotazione() {
        // Lettura delle scelte fatte nelle tendine
        String dataTesto = campoData.getText().trim();
        String idSala = idSalaSelezionata();
        String fascia = String.valueOf(sceltaFasciaOraria.getSelectedItem());
        String idArea = idAreaSelezionata();
        String idPostazione = String.valueOf(sceltaIdPostazione.getSelectedItem());

        // L'assegnazione automatica si comunica al Controller con la stringa
        // vuota, come faceva il campo lasciato in bianco.
        if (POSTAZIONE_AUTOMATICA.equals(idPostazione)) {
            idPostazione = "";
        }

        // Validazione: serve una data, una sala scelta e un orario valido
        if (dataTesto.isEmpty() || idSala == null
                || fascia.startsWith("---") || fascia.equals(NESSUNA_FASCIA)) {
            mostraErrore("Scegli data, sala e orario prima di confermare.");
            return;
        }

        try {
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Con l'area scelta e la postazione automatica il posto viene
            // preso fra quelli liberi di quell'area soltanto
            boolean successo = controller.effettuaPrenotazione(
                    matricolaStudente, dataScelta, idSala, fascia,
                    idArea == null ? "" : idArea, idPostazione);

            if (successo) {
                mostraMessaggio("Prenotazione completata! Se non hai scelto il posto, te ne è stato assegnato uno automatico. La conferma viene stampata nella console.");
                svuotaCampi();
            } else {
                mostraErrore("Impossibile completare la prenotazione. Il posto scelto potrebbe essere stato occupato nel frattempo: rifai la verifica degli orari.");
            }
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD.");
        }
    }


    // METODI DI UTILITÀ GRAFICA

    /** Riporta la schermata allo stato iniziale dopo un'operazione riuscita. */
    private void svuotaCampi() {
        caricamentoTendine = true;

        campoData.setText(LocalDate.now().plusDays(1).toString());
        sceltaFasciaRicerca.setSelectedIndex(0);

        sceltaIdSala.removeAllItems();
        sceltaIdSala.addItem(NESSUNA_SALA);
        idSaleCaricate.clear();

        sceltaFasciaOraria.removeAllItems();
        sceltaFasciaOraria.addItem(NESSUN_ORARIO);

        sceltaIdArea.removeAllItems();
        sceltaIdArea.addItem(AREA_QUALSIASI);
        idAreeCaricate.clear();

        sceltaIdPostazione.removeAllItems();
        sceltaIdPostazione.addItem(POSTAZIONE_AUTOMATICA);

        caricamentoTendine = false;
    }

    private void mostraMessaggio(String testo) {
        etichettaEsito.setText("Operazione completata.");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Esito", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText("Errore di input.");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Costruisce il contenitore (JFrame) e inserisce il pannelloPrincipale.
     */
    public JFrame apriFormPrenotazione() {
        JFrame frame = new JFrame("Gestione Prenotazioni");
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(650, 420);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    /**
     * MAIN DI TEST
     * Permette di avviare questa singola schermata in modo isolato per testarne
     * il funzionamento grafico senza dover avviare l'intero sistema.
     */
    public static void main(String[] args) {
        String matricola = args.length > 0 ? args[0] : "N46001234";

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new FormPrenotazione(matricola).apriFormPrenotazione();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}