package boundary;

import controller.AutenticazioneController;
import eccezioni.BusinessException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

//Finestra di registrazione di un nuovo utente

public class FormRegistrazione implements BoundaryAutenticazione {

    // Collegamento all'unico Controller autorizzato (Singleton)
    private final AutenticazioneController controller = AutenticazioneController.getInstance();

    private JPanel pannelloPrincipale;
    private JTextField campoNome;
    private JTextField campoCognome;
    private JTextField campoEmail;
    private JPasswordField campoPassword;
    private JComboBox<String> comboRuolo;
    private JLabel labelParametro;
    private JTextField campoParametro;
    private JButton bottoneRegistrati;
    private JLabel etichettaEsito;

    public FormRegistrazione() {
        costruisciInterfaccia();

        // Comportamento dinamico: adatta l'etichetta in base al ruolo
        comboRuolo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ruoloSelezionato = (String) comboRuolo.getSelectedItem();
                if ("Studente".equals(ruoloSelezionato)) {
                    labelParametro.setText("Matricola:");
                } else {
                    labelParametro.setText("Codice Identificativo:");
                }
            }
        });

        // Avvio del caso d'uso
        bottoneRegistrati.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrazione();
            }
        });
    }

    private void costruisciInterfaccia() {
        campoNome = new JTextField(20);
        campoCognome = new JTextField(20);
        campoEmail = new JTextField(40);
        campoPassword = new JPasswordField(20);

        // I ruoli ammessi dal sistema
        comboRuolo = new JComboBox<>(new String[]{"Studente", "Bibliotecario"});

        labelParametro = new JLabel("Matricola:");
        campoParametro = new JTextField(20);

        bottoneRegistrati = new JButton("Registrati");
        etichettaEsito = new JLabel(" ");

        // Disposizione a griglia per i campi di input
        JPanel campi = new JPanel(new GridLayout(6, 2, 8, 12));
        campi.add(new JLabel("Nome:"));
        campi.add(campoNome);
        campi.add(new JLabel("Cognome:"));
        campi.add(campoCognome);
        campi.add(new JLabel("Email:"));
        campi.add(campoEmail);
        campi.add(new JLabel("Password (min 6 caratteri):"));
        campi.add(campoPassword);
        campi.add(new JLabel("Ruolo:"));
        campi.add(comboRuolo);
        campi.add(labelParametro);
        campi.add(campoParametro);

        // Area inferiore con bottone ed esito
        JPanel basso = new JPanel(new BorderLayout(8, 4));
        basso.add(bottoneRegistrati, BorderLayout.WEST);
        basso.add(etichettaEsito, BorderLayout.CENTER);

        // Contenitore per centrare esteticamente il bottone
        JPanel pannelloBottone = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pannelloBottone.add(bottoneRegistrati);

        // Assemblaggio finale
        pannelloPrincipale = new JPanel(new BorderLayout(15, 15));
        pannelloPrincipale.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pannelloPrincipale.add(new JLabel("Registrazione Nuovo Utente", JLabel.CENTER), BorderLayout.NORTH);
        pannelloPrincipale.add(campi, BorderLayout.CENTER);
        pannelloPrincipale.add(pannelloBottone, BorderLayout.SOUTH);
    }

    @Override
    public void registrazione() {
        try {
            // L'interfaccia estrae solo le stringhe e le passa al Controller
            controller.registrazione(
                    campoNome.getText().trim(),
                    campoCognome.getText().trim(),
                    campoEmail.getText().trim(),
                    new String(campoPassword.getPassword()),
                    (String) comboRuolo.getSelectedItem(),
                    campoParametro.getText().trim()
            );

            mostraMessaggio("Registrazione completata con successo! Ora puoi effettuare il login.");
            svuotaCampi();

        } catch (BusinessException ex) {
            // Il Controller ha rilevato un errore di validazione o un duplicato
            mostraErrore(ex.getMessage());
        }
    }

    private void svuotaCampi() {
        campoNome.setText("");
        campoCognome.setText("");
        campoEmail.setText("");
        campoPassword.setText("");
        campoParametro.setText("");
        // Resetta la tendina sul primo valore (Studente)
        comboRuolo.setSelectedIndex(0);
    }

    private void mostraMessaggio(String testo) {
        etichettaEsito.setText("Operazione avvenuta con successo");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Esito Registrazione", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText("Registrazione fallita");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore", JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce la finestra e la restituisce già pronta da mostrare. */
    public JFrame apriFormRegistrazione() {
        JFrame frame = new JFrame("Registrazione - Sistema Biblioteca");
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    @Override
    public void login() {
        mostraMessaggio("Funzionalità di Login in via di sviluppo.");
    }

    /**
     * Avvio per collaudare questa singola finestra senza dover avviare l'intero applicativo.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new FormRegistrazione().apriFormRegistrazione();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}