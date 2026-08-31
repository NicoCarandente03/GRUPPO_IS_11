package boundary;

import controller.MonitoraggioSaleController;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Finestra di consultazione dello storico delle prenotazioni.
 *
 * I due filtri, sala e matricola dello studente, sono facoltativi: lasciandoli
 * vuoti si ottiene lo storico completo.
 *
 * La sala si sceglie da un menu a tendina costruito sulle sale registrate,
 * invece di digitarne il nome: cosi' non si puo' sbagliare, e il requisito RF21
 * parla appunto di consultare lo storico per ciascuna sala. La prima voce dice
 * esplicitamente "Tutte le sale" e corrisponde al filtro non applicato.
 *
 * Il codice del bibliotecario non si digita: arriva al costruttore da chi apre
 * la finestra, oggi il main di prova e domani AutenticazioneController
 * leggendolo dalla sessione. Cosi' la finestra non permette di spacciarsi per un
 * altro bibliotecario.
 *
 * La finestra non conosce le Entity: riceve dei PrenotazioneDTO gia' pronti da
 * mettere in tabella.
 */
public class FormMonitoraggio implements BoundaryMonitoraggioSale {

    /** Prima voce del menu, corrisponde al filtro sulla sala non applicato. */
    private static final String TUTTE_LE_SALE = "Tutte le sale";

    private static final String[] COLONNE = {
            "ID", "Studente", "Sala", "Area", "Postazione", "Data", "Fascia oraria", "Stato"
    };

    private final MonitoraggioSaleController controller =
            MonitoraggioSaleController.getInstance();

    private final String codiceBibliotecario;

    private JPanel pannelloPrincipale;
    private JComboBox<String> sceltaSala;
    private JTextField campoMatricola;
    private JButton bottoneCerca;
    private JButton bottonePulisci;
    private JTable tabellaRisultati;
    private JLabel etichettaEsito;

    public FormMonitoraggio(String codiceBibliotecario) {
        this.codiceBibliotecario = codiceBibliotecario;
        costruisciInterfaccia();

        bottoneCerca.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consultazioneStoricoPrenotazioni();
            }
        });

        bottonePulisci.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sceltaSala.setSelectedIndex(0);
                campoMatricola.setText("");
                consultazioneStoricoPrenotazioni();
            }
        });

        consultazioneStoricoPrenotazioni();
    }

    private void costruisciInterfaccia() {
        sceltaSala = new JComboBox<>();
        sceltaSala.addItem(TUTTE_LE_SALE);
        for (String nome : controller.getElencoNomiSale()) {
            sceltaSala.addItem(nome);
        }

        campoMatricola = new JTextField(14);
        bottoneCerca = new JButton("Cerca");
        bottonePulisci = new JButton("Pulisci filtri");
        etichettaEsito = new JLabel(" ");

        tabellaRisultati = new JTable(new DefaultTableModel(COLONNE, 0));

        JPanel filtri = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtri.add(new JLabel("Sala:"));
        filtri.add(sceltaSala);
        filtri.add(new JLabel("Matricola studente:"));
        filtri.add(campoMatricola);
        filtri.add(bottoneCerca);
        filtri.add(bottonePulisci);

        pannelloPrincipale = new JPanel(new BorderLayout(8, 8));
        pannelloPrincipale.add(filtri, BorderLayout.NORTH);
        pannelloPrincipale.add(new JScrollPane(tabellaRisultati), BorderLayout.CENTER);
        pannelloPrincipale.add(etichettaEsito, BorderLayout.SOUTH);
    }

    @Override
    public void consultazioneStoricoPrenotazioni() {
        try {
            List<PrenotazioneDTO> risultati = controller.consultazioneStoricoPrenotazioni(
                    codiceBibliotecario,
                    salaSelezionata(),
                    campoMatricola.getText().trim());

            DefaultTableModel modello = new DefaultTableModel(COLONNE, 0);

            for (PrenotazioneDTO prenotazione : risultati) {
                modello.addRow(new Object[]{
                        prenotazione.getIdPrenotazione(),
                        prenotazione.getMatricolaStudente(),
                        prenotazione.getNomeSala(),
                        prenotazione.getTipoArea(),
                        prenotazione.getIdPostazione(),
                        prenotazione.getData(),
                        prenotazione.getFasciaOraria(),
                        prenotazione.getStato()
                });
            }

            tabellaRisultati.setModel(modello);

            etichettaEsito.setText(componiEsito(risultati.size()));

        } catch (BusinessException e) {
            tabellaRisultati.setModel(new DefaultTableModel(COLONNE, 0));
            mostraErrore(e.getMessage());
        }
    }

    /**
     * Nome della sala scelta, oppure stringa vuota se e' selezionata la voce
     * "Tutte le sale": il controller interpreta il filtro vuoto come nessun
     * filtro.
     */
    private String salaSelezionata() {
        String scelta = String.valueOf(sceltaSala.getSelectedItem()).trim();
        return TUTTE_LE_SALE.equals(scelta) ? "" : scelta;
    }

    /**
     * Descrive a parole i filtri applicati, per dire al bibliotecario su quale
     * insieme ha appena cercato.
     */
    private String descrizioneFiltri() {
        String sala = salaSelezionata();
        String matricola = campoMatricola.getText().trim();

        if (sala.isEmpty() && matricola.isEmpty()) {
            return "";
        }
        if (matricola.isEmpty()) {
            return " per " + sala;
        }
        if (sala.isEmpty()) {
            return " per lo studente " + matricola;
        }
        return " per lo studente " + matricola + " in " + sala;
    }

    /**
     * Messaggio di esito della ricerca. Quando non ci sono risultati lo dice
     * esplicitamente e ripete i filtri, cosi' e' chiaro che la ricerca e'
     * andata a buon fine ma non ha trovato nulla
     */
    private String componiEsito(int quante) {
        String filtri = descrizioneFiltri();

        if (quante == 0) {
            return "Non risultano prenotazioni"
                    + (filtri.isEmpty() ? " registrate" : filtri) + ".";
        }

        String conteggio = quante == 1
                ? "1 prenotazione trovata" : quante + " prenotazioni trovate";

        return conteggio + (filtri.isEmpty() ? " in tutte le sale" : filtri) + ".";
    }

    // Le operazioni che seguono appartengono ad altri casi d'uso e vengono
    // completate da chi li sviluppa.

    @Override
    public void monitoraggioSala() {
        JOptionPane.showMessageDialog(pannelloPrincipale,
                "Funzionalita' non ancora disponibile.", "Esito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void monitoraggioAndamentoServizi() {
        JOptionPane.showMessageDialog(pannelloPrincipale,
                "Funzionalita' non ancora disponibile.", "Esito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText(testo);
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce la finestra e la restituisce gia' pronta da mostrare */
    public JFrame apriFormMonitoraggio() {
        JFrame frame = new JFrame("Storico prenotazioni, bibliotecario " + codiceBibliotecario);
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(940, 420);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    /**
     * Avvio di prova. Finche' non c'e' il Log-in, il codice del bibliotecario si
     * passa come argomento; senza argomenti si usa quello dei dati di prova
     */
    public static void main(String[] args) {
        String codice = args.length > 0 ? args[0] : "B1234";

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new FormMonitoraggio(codice).apriFormMonitoraggio();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}
