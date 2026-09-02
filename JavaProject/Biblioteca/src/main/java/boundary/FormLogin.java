package boundary;

import controller.AutenticazioneController;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.Studente;
import entity.Utente;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FormLogin implements BoundaryAutenticazione {

    private final AutenticazioneController controller = AutenticazioneController.getInstance();

    private JFrame frameCorrente;
    private JPanel pannelloPrincipale;
    private JTextField campoEmail;
    private JPasswordField campoPassword;
    private JButton bottoneLogin;
    private JButton bottoneRegistrati;
    private JLabel etichettaEsito;

    public FormLogin() {
        costruisciInterfaccia();

        bottoneLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        bottoneRegistrati.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrazione();
            }
        });
    }

    private void costruisciInterfaccia() {
        campoEmail = new JTextField(20);
        campoPassword = new JPasswordField(20);

        bottoneLogin = new JButton("Accedi");
        bottoneRegistrati = new JButton("Non hai un account? Registrati");
        etichettaEsito = new JLabel(" ");

        // Griglia centrale per gli input
        JPanel campi = new JPanel(new GridLayout(2, 2, 8, 12));
        campi.add(new JLabel("Email:"));
        campi.add(campoEmail);
        campi.add(new JLabel("Password:"));
        campi.add(campoPassword);

        // Area inferiore con i bottoni
        JPanel pannelloBottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        pannelloBottoni.add(bottoneLogin);
        pannelloBottoni.add(bottoneRegistrati);

        JPanel basso = new JPanel(new BorderLayout(8, 8));
        basso.add(pannelloBottoni, BorderLayout.NORTH);
        basso.add(etichettaEsito, BorderLayout.CENTER);

        // Assemblaggio del pannello principale
        pannelloPrincipale = new JPanel(new BorderLayout(15, 15));
        pannelloPrincipale.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pannelloPrincipale.add(new JLabel("Accesso al Sistema Biblioteca", JLabel.CENTER), BorderLayout.NORTH);
        pannelloPrincipale.add(campi, BorderLayout.CENTER);
        pannelloPrincipale.add(basso, BorderLayout.SOUTH);
    }

    @Override
    public void login() {
        try {
            // Estrazione dati e chiamata al controller
            Utente utente = controller.login(
                    campoEmail.getText().trim(),
                    new String(campoPassword.getPassword())
            );

            // Smistamento logico in base all'identità (Polimorfismo)
            String tipoUtente = (utente instanceof Studente) ? "Studente" : "Bibliotecario";
            mostraMessaggio("Accesso effettuato con successo!\nBenvenuto " + utente.getNome() + " (" + tipoUtente + ")");

            // TODO: Qui chiuderemo il FormLogin e apriremo la Homepage/Dashboard
            // if (frameCorrente != null) { frameCorrente.dispose(); }

        } catch (BusinessException ex) {
            mostraErrore(ex.getMessage());
        } catch (Exception ex) {
            mostraErrore("Errore di sistema: " + ex.getMessage());
        }
    }

    @Override
    public void registrazione() {
        // Navigazione: Apre la finestra di registrazione creata nel caso d'uso precedente
        JFrame formReg = new FormRegistrazione().apriFormRegistrazione();
        formReg.setVisible(true);
    }

    private void mostraMessaggio(String testo) {
        etichettaEsito.setText("Login avvenuto con successo.");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Accesso Consentito", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText("Accesso negato.");
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore Login", JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce e restituisce la finestra */
    public JFrame apriFormLogin() {
        frameCorrente = new JFrame("Login - Sistema Biblioteca");
        frameCorrente.setContentPane(pannelloPrincipale);
        frameCorrente.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameCorrente.pack();
        frameCorrente.setResizable(false);
        frameCorrente.setLocationRelativeTo(null);
        return frameCorrente;
    }
}
