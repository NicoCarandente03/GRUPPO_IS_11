package boundary;

import controller.GestionePrenotazioneController;
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

    // Componenti grafici (Swing)
    private JPanel pannelloPrincipale;

    // Campi di input
    private JTextField campoData;
    private JTextField campoFasciaRicerca; // Per cercare le sale disponibili
    private JTextField campoIdSala;
    private JTextField campoIdArea;
    private JTextField campoIdPostazione;
    private JComboBox<String> sceltaFasciaOraria; // Per scegliere l'orario della prenotazione

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
    }


    private void costruisciInterfaccia() {
        // Inizializzazione dei campi di testo
        campoData = new JTextField(14);
        campoData.setToolTipText("Es: 2026-06-15");

        campoFasciaRicerca = new JTextField(14);
        campoFasciaRicerca.setToolTipText("Es: 09:00-11:00");

        campoIdSala = new JTextField(14);
        campoIdArea = new JTextField(14);
        campoIdPostazione = new JTextField(14);

        // La tendina parte con un messaggio di default
        sceltaFasciaOraria = new JComboBox<>();
        sceltaFasciaOraria.addItem("--- Verifica prima gli orari ---");

        bottoneCercaSale = new JButton("Cerca Sale Disponibili");
        bottoneVerificaOrari = new JButton("Verifica Orari Sala");
        bottonePrenota = new JButton("Conferma Prenotazione");
        etichettaEsito = new JLabel(" ");

        // Creazione di una griglia a 2 colonne per allineare le etichette (Label) e i campi (TextField)
        JPanel campi = new JPanel(new GridLayout(7, 2, 8, 12));

        // - SEZIONE 1: Consultazione
        campi.add(new JLabel("Data (YYYY-MM-DD):"));
        campi.add(campoData);

        campi.add(new JLabel("Fascia Oraria (per cercare sale):"));
        JPanel pannelloCercaSale = new JPanel(new BorderLayout(5, 0));
        pannelloCercaSale.add(campoFasciaRicerca, BorderLayout.CENTER);
        pannelloCercaSale.add(bottoneCercaSale, BorderLayout.EAST);
        campi.add(pannelloCercaSale);

        // - SEZIONE 2: Dettagli Prenotazione
        campi.add(new JLabel("ID Sala (scelta):"));
        campi.add(campoIdSala);

        campi.add(new JLabel("Seleziona Orario:"));
        JPanel pannelloOrari = new JPanel(new BorderLayout(5, 0));
        pannelloOrari.add(sceltaFasciaOraria, BorderLayout.CENTER);
        pannelloOrari.add(bottoneVerificaOrari, BorderLayout.EAST);
        campi.add(pannelloOrari);

        campi.add(new JLabel("ID Area (Opzionale):"));
        campi.add(campoIdArea);

        campi.add(new JLabel("ID Postazione (Opzionale):"));
        campi.add(campoIdPostazione);

        // Header della finestra
        JPanel alto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        alto.add(new JLabel("Area Prenotazioni - Studente: " + matricolaStudente));

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
        String fascia = campoFasciaRicerca.getText().trim();

        // Controllo base sugli input grafici
        if (dataTesto.isEmpty() || fascia.isEmpty()) {
            mostraErrore("Inserisci la Data e la Fascia Oraria per vedere le sale disponibili.");
            return;
        }

        try {
            // Conversione della stringa nel tipo LocalDate richiesto dal backend
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Chiamata al Controller
            List<SalaStudio> sale = controller.consultazioneDisponibilitaSaleStudio(dataScelta, fascia);

            // Elaborazione della risposta per mostrarla a video
            if (sale == null || sale.isEmpty()) {
                mostraMessaggio("Non ci sono sale registrate o disponibili nel sistema.");
            } else {
                StringBuilder sb = new StringBuilder("Sale attualmente disponibili:\n\n");
                for (SalaStudio s : sale) {
                    sb.append("- ").append(s.getNome()).append(" (ID: ").append(s.getIdSala()).append(")\n");
                }
                // Mostra un pop-up con l'elenco delle sale
                JOptionPane.showMessageDialog(pannelloPrincipale, sb.toString(), "Risultato Ricerca", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD (es. 2026-06-15).");
        }
    }

    @Override
    public void verificaFasceOrarie() {
        String dataTesto = campoData.getText().trim();
        String idSala = campoIdSala.getText().trim();

        if (dataTesto.isEmpty() || idSala.isEmpty()) {
            mostraErrore("Inserisci Data e ID Sala per verificare le fasce disponibili in quella stanza.");
            return;
        }

        try {
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Chiamata al Controller per estrarre le fasce non esaurite
            List<String> fasce = controller.visualizzazioneFasceOrarieDisponibili(idSala, dataScelta);

            // Svuota la tendina e la ripopola con i dati freschi dal DB
            sceltaFasciaOraria.removeAllItems();
            if (fasce.isEmpty()) {
                sceltaFasciaOraria.addItem("Nessuna fascia disponibile");
                mostraErrore("La sala selezionata è al completo (sold-out) per la data indicata.");
            } else {
                for (String f : fasce) {
                    sceltaFasciaOraria.addItem(f);
                }
                etichettaEsito.setText("Fasce orarie caricate con successo.");
            }
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD.");
        }
    }

    @Override
    public void effettuaPrenotazione() {
        // Lettura di tutti i campi testuali
        String dataTesto = campoData.getText().trim();
        String idSala = campoIdSala.getText().trim();
        String fascia = String.valueOf(sceltaFasciaOraria.getSelectedItem());
        String idArea = campoIdArea.getText().trim();
        String idPostazione = campoIdPostazione.getText().trim();

        // Validazione dei campi.
        // I campi obbligatori non devono essere vuoti e l'orario deve essere valido
        if (dataTesto.isEmpty() || idSala.isEmpty() || fascia.startsWith("---") || fascia.equals("Nessuna fascia disponibile")) {
            mostraErrore("Compila tutti i campi obbligatori (Data, Sala, Orario selezionato) prima di confermare.");
            return;
        }

        try {
            LocalDate dataScelta = LocalDate.parse(dataTesto);

            // Il Boundary passa i dati grezzi al Controller
            boolean successo = controller.effettuaPrenotazione(
                    matricolaStudente, dataScelta, idSala, fascia, idArea, idPostazione);

            if (successo) {
                mostraMessaggio("Prenotazione completata! Se non hai scelto il posto, te ne è stato assegnato uno automatico. Controlla le notifiche.");
                svuotaCampi();
            } else {
                mostraErrore("Impossibile completare la prenotazione. Controlla che gli ID inseriti siano corretti.");
            }
        } catch (DateTimeParseException e) {
            mostraErrore("Formato data errato. Utilizza YYYY-MM-DD.");
        }
    }


    // METODI DI UTILITÀ GRAFICA

    /** Ripulisce i campi testuali dopo un'operazione di successo. */
    private void svuotaCampi() {
        campoData.setText("");
        campoFasciaRicerca.setText("");
        campoIdSala.setText("");
        sceltaFasciaOraria.removeAllItems();
        sceltaFasciaOraria.addItem("--- Verifica prima gli orari ---");
        campoIdArea.setText("");
        campoIdPostazione.setText("");
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