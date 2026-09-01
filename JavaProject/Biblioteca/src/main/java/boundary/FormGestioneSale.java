package boundary;

import controller.GestioneSaleController;
import dto.AreaDTO;
import dto.SalaStudioDTO;
import eccezioni.BusinessException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Finestra di creazione di una sala studio.
 *
 * Il codice del bibliotecario arriva al costruttore da chi apre la finestra
 *
 * La finestra non conosce le Entity: passa i dati grezzi a
 * GestioneSaleController, cosi' come li ha digitati l'utente, e riceve indietro
 * un SalaStudioDTO da mostrare. Tutte le regole di validita' stanno nel controller
 */
public class FormGestioneSale implements BoundaryGestioneSale {

    /**
     * Messaggio per i guasti tecnici, distinti dagli errori di regola di
     * business: questi ultimi arrivano come BusinessException con un testo gia'
     * pensato per l'utente.
     */
    private static final String TESTO_ERRORE_TECNICO =
            "Errore tecnico, operazione non riuscita. Controlla che il database sia raggiungibile.";

    private final GestioneSaleController controller = GestioneSaleController.getInstance();

    private final String codiceBibliotecario;

    private JPanel pannelloPrincipale;
    private JTextField campoNome;
    private JTextField campoDescrizione;
    private JTextField campoPostazioni;
    private JTextField campoOrari;
    private JComboBox<String> sceltaTipoArea;
    private JButton bottoneAggiungiArea;
    private JButton bottoneRimuoviArea;
    private JList<String> elencoAree;
    private DefaultListModel<String> modelloAree;
    private JButton bottoneCrea;
    private JLabel etichettaEsito;

    public FormGestioneSale(String codiceBibliotecario) {
        this.codiceBibliotecario = codiceBibliotecario;
        costruisciInterfaccia();

        bottoneAggiungiArea.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modelloAree.addElement(String.valueOf(sceltaTipoArea.getSelectedItem()));
            }
        });

        bottoneRimuoviArea.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int scelta = elencoAree.getSelectedIndex();
                if (scelta >= 0) {
                    modelloAree.remove(scelta);
                }
            }
        });

        bottoneCrea.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                creazioneAulaStudio();
            }
        });
    }

    private void costruisciInterfaccia() {
        campoNome = new JTextField(24);
        campoDescrizione = new JTextField(24);
        campoPostazioni = new JTextField(24);
        campoOrari = new JTextField("08:00 - 20:00", 24);
        etichettaEsito = new JLabel(" ");

        sceltaTipoArea = new JComboBox<>(
                GestioneSaleController.TIPI_AREA_AMMESSI.toArray(new String[0]));
        bottoneAggiungiArea = new JButton("Aggiungi area");
        bottoneRimuoviArea = new JButton("Rimuovi area");
        bottoneCrea = new JButton("Crea sala studio");

        modelloAree = new DefaultListModel<>();
        elencoAree = new JList<>(modelloAree);

        JPanel campi = new JPanel(new GridLayout(4, 2, 8, 8));
        campi.add(new JLabel("Nome:"));
        campi.add(campoNome);
        campi.add(new JLabel("Descrizione:"));
        campi.add(campoDescrizione);
        campi.add(new JLabel("Numero postazioni:"));
        campi.add(campoPostazioni);
        campi.add(new JLabel("Orari di apertura:"));
        campi.add(campoOrari);

        JPanel gestioneAree = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gestioneAree.add(new JLabel("Aree (opzionali):"));
        gestioneAree.add(sceltaTipoArea);
        gestioneAree.add(bottoneAggiungiArea);
        gestioneAree.add(bottoneRimuoviArea);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(gestioneAree, BorderLayout.NORTH);
        centro.add(new JScrollPane(elencoAree), BorderLayout.CENTER);

        JPanel basso = new JPanel(new BorderLayout(8, 4));
        basso.add(bottoneCrea, BorderLayout.WEST);
        basso.add(etichettaEsito, BorderLayout.CENTER);

        pannelloPrincipale = new JPanel(new BorderLayout(10, 10));
        pannelloPrincipale.add(campi, BorderLayout.NORTH);
        pannelloPrincipale.add(centro, BorderLayout.CENTER);
        pannelloPrincipale.add(basso, BorderLayout.SOUTH);
    }

    @Override
    public void creazioneAulaStudio() {
        List<String> tipiArea = new ArrayList<>();
        for (int i = 0; i < modelloAree.size(); i++) {
            tipiArea.add(modelloAree.get(i));
        }

        try {
            SalaStudioDTO sala = controller.creazioneAulaStudio(
                    campoNome.getText().trim(),
                    campoDescrizione.getText().trim(),
                    campoPostazioni.getText().trim(),
                    campoOrari.getText().trim(),
                    tipiArea,
                    codiceBibliotecario);

            mostraMessaggio("Operazione avvenuta con successo" + riepilogo(sala));
            svuotaCampi();

        } catch (BusinessException e) {
            mostraErrore(e.getMessage());
        } catch (RuntimeException e) {
            mostraErrore(TESTO_ERRORE_TECNICO);
        }
    }

    private String riepilogo(SalaStudioDTO sala) {
        if (sala.getAree().isEmpty()) {
            return "\nSala " + sala.getNome() + ", nessuna area.";
        }

        StringBuilder testo = new StringBuilder("\nSala " + sala.getNome() + ", aree create:");
        for (AreaDTO area : sala.getAree()) {
            testo.append("\n  ").append(area.getTipo())
                    .append(", ").append(area.getNumPostazioni()).append(" postazioni");
        }
        return testo.toString();
    }

    private void svuotaCampi() {
        campoNome.setText("");
        campoDescrizione.setText("");
        campoPostazioni.setText("");
        modelloAree.clear();
    }

    // DA COMPLETARE

    @Override
    public void modificaAulaStudio() {
        mostraMessaggio("Funzionalita' non ancora disponibile.");
    }

    @Override
    public void eliminazioneAulaStudio() {
        mostraMessaggio("Funzionalita' non ancora disponibile.");
    }

    private void mostraMessaggio(String testo) {
        etichettaEsito.setText("Operazione avvenuta con successo");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Esito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText(testo);
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce la finestra e la restituisce gia' pronta da mostrare. */
    public JFrame apriFormGestioneSale() {
        JFrame frame = new JFrame("Creazione sala studio, bibliotecario " + codiceBibliotecario);
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(640, 460);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    /**
     * Avvio di prova!!!! Finche' non c'e' il Log-in, il codice del bibliotecario si
     * passa come argomento; senza argomenti si usa quello dei dati di prova.
     */
    public static void main(String[] args) {
        String codice = args.length > 0 ? args[0] : "B1234";

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new FormGestioneSale(codice).apriFormGestioneSale();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}
