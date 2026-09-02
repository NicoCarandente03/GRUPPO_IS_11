package boundary;

import controller.AutenticazioneController;
import controller.GestionePrenotazioneController;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;
import entity.Studente;
import entity.Utente;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Finestra del profilo studente: elenca le prenotazioni dello studente che ha
 * effettuato l'accesso e permette di annullare quella selezionata o di
 * confermarne la presenza con il check-in.
 *
 * La matricola non si digita e non viene passata da fuori: si legge dalla
 * sessione tenuta da AutenticazioneController, cosi' la finestra non permette
 * di agire sulle prenotazioni di un altro studente.
 *
 * La finestra non conosce le Entity: riceve dei PrenotazioneDTO gia' pronti da
 * mettere in tabella.
 */
public class FormProfiloStudente implements BoundaryProfiloStudente {

    /**
     * Messaggio per i guasti tecnici, distinti dagli errori di regola di
     * business: questi ultimi arrivano come BusinessException con un testo gia'
     * pensato per l'utente, mentre un database irraggiungibile non ha un
     * messaggio suo da mostrare.
     */
    private static final String TESTO_ERRORE_TECNICO =
            "Errore tecnico, operazione non riuscita. Controlla che il database sia raggiungibile.";

    private static final String[] COLONNE = {
            "ID", "Data", "Fascia oraria", "Sala", "Area", "Postazione", "Stato"
    };

    private final GestionePrenotazioneController controller =
            GestionePrenotazioneController.getInstance();

    private final String matricolaStudente;

    private JPanel pannelloPrincipale;
    private JButton bottoneAggiorna;
    private JButton bottoneAnnulla;
    private JButton bottoneCheckin;
    private JTable tabellaPrenotazioni;
    private JLabel etichettaEsito;

    public FormProfiloStudente() {
        this.matricolaStudente = matricolaDallaSessione();

        costruisciInterfaccia();

        bottoneAggiorna.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visualizzaPrenotazioniEffettuate();
            }
        });

        bottoneAnnulla.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annullamentoPrenotazione();
            }
        });

        bottoneCheckin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkin();
            }
        });

        visualizzaPrenotazioniEffettuate();
    }

    /**
     * Matricola dello studente che ha effettuato l'accesso.
     *
     * Una sola condizione copre due casi, perche' instanceof e' falso anche per
     * null: nessuna sessione aperta, e sessione aperta da un bibliotecario, che
     * nel profilo di uno studente non ha niente da fare.
     */
    private String matricolaDallaSessione() {
        Utente utente = AutenticazioneController.getInstance().getUtenteLoggato();

        if (!(utente instanceof Studente)) {
            throw new BusinessException("Errore, nessuno studente ha effettuato l'accesso!");
        }

        return ((Studente) utente).getMatricola();
    }

    private void costruisciInterfaccia() {
        bottoneAggiorna = new JButton("Aggiorna elenco");
        bottoneAnnulla = new JButton("Annulla prenotazione selezionata");
        bottoneCheckin = new JButton("Conferma presenza");
        etichettaEsito = new JLabel(" ");

        tabellaPrenotazioni = new JTable(new DefaultTableModel(COLONNE, 0));
        tabellaPrenotazioni.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel alto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        alto.add(new JLabel("Prenotazioni dello studente " + matricolaStudente));
        alto.add(bottoneAggiorna);

        JPanel pannelloBottoni = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pannelloBottoni.add(bottoneAnnulla);
        pannelloBottoni.add(bottoneCheckin);

        JPanel basso = new JPanel(new BorderLayout(8, 4));
        basso.add(pannelloBottoni, BorderLayout.WEST);
        basso.add(etichettaEsito, BorderLayout.CENTER);

        pannelloPrincipale = new JPanel(new BorderLayout(8, 8));
        pannelloPrincipale.add(alto, BorderLayout.NORTH);
        pannelloPrincipale.add(new JScrollPane(tabellaPrenotazioni), BorderLayout.CENTER);
        pannelloPrincipale.add(basso, BorderLayout.SOUTH);
    }

    @Override
    public void visualizzaPrenotazioniEffettuate() {
        List<PrenotazioneDTO> prenotazioni;

        try {
            prenotazioni = controller.visualizzaPrenotazioniEffettuate(matricolaStudente);
        } catch (RuntimeException e) {
            tabellaPrenotazioni.setModel(new DefaultTableModel(COLONNE, 0));
            mostraErrore(TESTO_ERRORE_TECNICO);
            return;
        }

        DefaultTableModel modello = new DefaultTableModel(COLONNE, 0);

        for (PrenotazioneDTO prenotazione : prenotazioni) {
            modello.addRow(new Object[]{
                    prenotazione.getIdPrenotazione(),
                    prenotazione.getData(),
                    prenotazione.getFasciaOraria(),
                    prenotazione.getNomeSala(),
                    prenotazione.getTipoArea(),
                    prenotazione.getIdPostazione(),
                    prenotazione.getStato()
            });
        }

        tabellaPrenotazioni.setModel(modello);

        if (prenotazioni.isEmpty()) {
            etichettaEsito.setText("Nessuna prenotazione effettuata.");
        } else {
            etichettaEsito.setText(prenotazioni.size() + " prenotazioni trovate.");
        }
    }

    @Override
    public void annullamentoPrenotazione() {
        int riga = tabellaPrenotazioni.getSelectedRow();

        if (riga < 0) {
            mostraErrore("Seleziona la prenotazione da annullare.");
            return;
        }

        String idPrenotazione = String.valueOf(tabellaPrenotazioni.getValueAt(riga, 0));

        try {
            controller.annullamentoPrenotazione(idPrenotazione);
            mostraMessaggio("Prenotazione annullata con successo");
        } catch (BusinessException e) {
            mostraErrore(e.getMessage());
        } catch (RuntimeException e) {
            mostraErrore(TESTO_ERRORE_TECNICO);
        }

        visualizzaPrenotazioniEffettuate();
    }

    @Override
    public void checkin() {
        int riga = tabellaPrenotazioni.getSelectedRow();

        if (riga < 0) {
            mostraErrore("Seleziona la prenotazione per cui effettuare il check-in.");
            return;
        }

        String idPrenotazione = String.valueOf(tabellaPrenotazioni.getValueAt(riga, 0));

        try {
            controller.checkin(idPrenotazione);
            mostraMessaggio("Check-in completato con successo. Presenza confermata");
        } catch (BusinessException e) {
            mostraErrore(e.getMessage());
        } catch (RuntimeException e) {
            mostraErrore(TESTO_ERRORE_TECNICO);
        }

        visualizzaPrenotazioniEffettuate();
    }

    @Override
    public void consultaAccessi() {
        mostraMessaggio("Funzionalita' non ancora disponibile.");
    }

    @Override
    public void visualizzaNotifiche() {
        mostraMessaggio("Funzionalita' non ancora disponibile.");
    }

    private void mostraMessaggio(String testo) {
        etichettaEsito.setText(testo);
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Esito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText(testo);
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce la finestra e la restituisce gia' pronta da mostrare. */
    public JFrame apriFormProfiloStudente() {
        JFrame frame = new JFrame("Profilo studente " + matricolaStudente);
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(840, 400);
        frame.setLocationRelativeTo(null);
        return frame;
    }
}
