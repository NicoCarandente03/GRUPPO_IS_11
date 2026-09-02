package boundary;

import controller.AutenticazioneController;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.Studente;
import entity.Utente;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

/**
 * Finestra di avvio dell'applicazione, unico punto di ingresso.
 *
 * Contiene tre schermate che si alternano nella stessa finestra: l'accesso e le
 * due aree di lavoro, una per lo studente e una per il bibliotecario. Quale
 * delle due venga mostrata dopo l'accesso dipende dal ruolo dell'utente
 */
public class MainFrame {

    private static final String CARTA_ACCESSO = "accesso";
    private static final String CARTA_STUDENTE = "studente";
    private static final String CARTA_BIBLIOTECARIO = "bibliotecario";

    private static final String TESTO_ERRORE_TECNICO =
            "Errore tecnico, operazione non riuscita. Controlla che il database sia raggiungibile.";

    private final AutenticazioneController controller = AutenticazioneController.getInstance();

    private JPanel pannelloPrincipale;
    private CardLayout carte;

    private JTextField campoEmail;
    private JPasswordField campoPassword;
    private JLabel etichettaEsito;

    private JLabel benvenutoStudente;
    private JLabel benvenutoBibliotecario;

    // identita' di chi ha effettuato l'accesso, passata alle schermate operative
    private String matricolaStudente;
    private String codiceBibliotecario;

    // finestre gia' aperte, per non aprirne due uguali premendo il bottone due volte
    private JFrame finestraProfilo;
    private JFrame finestraPrenotazione;
    private JFrame finestraGestioneSale;
    private JFrame finestraMonitoraggio;
    private JFrame finestraRegistrazione;

    public MainFrame() {
        costruisciInterfaccia();
    }

    private void costruisciInterfaccia() {
        carte = new CardLayout();
        pannelloPrincipale = new JPanel(carte);

        pannelloPrincipale.add(costruisciAccesso(), CARTA_ACCESSO);
        pannelloPrincipale.add(costruisciAreaStudente(), CARTA_STUDENTE);
        pannelloPrincipale.add(costruisciAreaBibliotecario(), CARTA_BIBLIOTECARIO);

        carte.show(pannelloPrincipale, CARTA_ACCESSO);
    }

    // ---------- schermata di accesso ----------

    private JPanel costruisciAccesso() {
        campoEmail = new JTextField(22);
        campoPassword = new JPasswordField(22);
        etichettaEsito = new JLabel(" ");

        JButton bottoneEntra = new JButton("Entra");
        bottoneEntra.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                entra();
            }
        });

        JButton bottoneRegistrati = new JButton("Registrati");
        bottoneRegistrati.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                apriRegistrazione();
            }
        });

        // premere Invio nel campo password equivale a premere Entra
        campoPassword.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                entra();
            }
        });

        JPanel pannello = colonna();
        pannello.add(titolo("Sistema di prenotazione sale studio"));
        pannello.add(Box.createVerticalStrut(24));
        pannello.add(etichetta("Email"));
        pannello.add(campoEmail);
        pannello.add(Box.createVerticalStrut(10));
        pannello.add(etichetta("Password"));
        pannello.add(campoPassword);
        pannello.add(Box.createVerticalStrut(18));
        pannello.add(bottoneEntra);
        pannello.add(Box.createVerticalStrut(6));
        pannello.add(bottoneRegistrati);
        pannello.add(Box.createVerticalStrut(14));
        pannello.add(etichettaEsito);

        allinea(pannello);
        return pannello;
    }

    private void entra() {
        String email = campoEmail.getText().trim();
        String password = new String(campoPassword.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            mostraErrore("Inserisci email e password.");
            return;
        }

        try {
            Utente utente = controller.login(email, password);

            if (utente instanceof Studente) {
                Studente studente = (Studente) utente;
                matricolaStudente = studente.getMatricola();
                benvenutoStudente.setText("Studente " + utente.getNome() + " " + utente.getCognome()
                        + ", matricola " + matricolaStudente);
                vaiA(CARTA_STUDENTE);

            } else if (utente instanceof Bibliotecario) {
                Bibliotecario bibliotecario = (Bibliotecario) utente;
                codiceBibliotecario = bibliotecario.getCodiceIdentificativo();
                benvenutoBibliotecario.setText("Bibliotecario " + utente.getNome() + " "
                        + utente.getCognome() + ", codice " + codiceBibliotecario);
                vaiA(CARTA_BIBLIOTECARIO);

            } else {
                mostraErrore("Ruolo dell'utente non riconosciuto.");
            }

        } catch (BusinessException e) {
            mostraErrore(e.getMessage());
        } catch (RuntimeException e) {
            mostraErrore(TESTO_ERRORE_TECNICO);
        }
    }

    // ---------- area dello studente ----------

    private JPanel costruisciAreaStudente() {
        benvenutoStudente = new JLabel(" ");

        JButton bottoneProfilo = new JButton("Le mie prenotazioni");
        bottoneProfilo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finestraProfilo = apriUnaVolta(finestraProfilo,
                        () -> new FormProfiloStudente().apriFormProfiloStudente());
            }
        });

        JButton bottonePrenota = new JButton("Prenota una postazione");
        bottonePrenota.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finestraPrenotazione = apriUnaVolta(finestraPrenotazione,
                        () -> new FormPrenotazione(matricolaStudente).apriFormPrenotazione());
            }
        });

        JPanel pannello = colonna();
        pannello.add(titolo("Area studente"));
        pannello.add(Box.createVerticalStrut(10));
        pannello.add(benvenutoStudente);
        pannello.add(Box.createVerticalStrut(24));
        pannello.add(bottoneProfilo);
        pannello.add(Box.createVerticalStrut(8));
        pannello.add(bottonePrenota);
        pannello.add(Box.createVerticalStrut(24));
        pannello.add(bottoneEsci());

        allinea(pannello);
        return pannello;
    }

    // ---------- area del bibliotecario ----------

    private JPanel costruisciAreaBibliotecario() {
        benvenutoBibliotecario = new JLabel(" ");

        JButton bottoneSale = new JButton("Crea sala studio");
        bottoneSale.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finestraGestioneSale = apriUnaVolta(finestraGestioneSale,
                        () -> new FormGestioneSale().apriFormGestioneSale());
            }
        });

        JButton bottoneStorico = new JButton("Storico prenotazioni");
        bottoneStorico.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finestraMonitoraggio = apriUnaVolta(finestraMonitoraggio,
                        () -> new FormMonitoraggio().apriFormMonitoraggio());
            }
        });

        JPanel pannello = colonna();
        pannello.add(titolo("Area bibliotecario"));
        pannello.add(Box.createVerticalStrut(10));
        pannello.add(benvenutoBibliotecario);
        pannello.add(Box.createVerticalStrut(24));
        pannello.add(bottoneSale);
        pannello.add(Box.createVerticalStrut(8));
        pannello.add(bottoneStorico);
        pannello.add(Box.createVerticalStrut(24));
        pannello.add(bottoneEsci());

        allinea(pannello);
        return pannello;
    }

    // ---------- navigazione e apertura delle finestre ----------

    private JButton bottoneEsci() {
        JButton bottone = new JButton("Esci");
        bottone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                esci();
            }
        });
        return bottone;
    }

    /**
     * Torna alla schermata di accesso e dimentica l'identita' corrente.
     *
     * Chiude la sessione e con essa le schermate operative gia' aperte: sono le
     * finestre di chi stava lavorando fino a un momento fa, e lasciarle aperte
     * le farebbe usare da chi entra dopo.
     */
    private void esci() {
        controller.logout();

        chiudi(finestraProfilo);
        chiudi(finestraPrenotazione);
        chiudi(finestraGestioneSale);
        chiudi(finestraMonitoraggio);

        finestraProfilo = null;
        finestraPrenotazione = null;
        finestraGestioneSale = null;
        finestraMonitoraggio = null;

        matricolaStudente = null;
        codiceBibliotecario = null;
        campoEmail.setText("");
        campoPassword.setText("");
        etichettaEsito.setText(" ");
        vaiA(CARTA_ACCESSO);
    }

    private void chiudi(JFrame finestra) {
        if (finestra != null) {
            finestra.dispose();
        }
    }

    private void vaiA(String carta) {
        etichettaEsito.setText(" ");
        campoPassword.setText("");
        carte.show(pannelloPrincipale, carta);
    }

    private void apriRegistrazione() {
        finestraRegistrazione = apriUnaVolta(finestraRegistrazione,
                () -> new FormRegistrazione().apriFormRegistrazione());
    }

    /**
     * Apre la finestra solo se non e' gia' aperta, altrimenti porta in primo
     * piano quella esistente. Evita che premendo due volte lo stesso bottone si
     * ottengano due copie della stessa schermata.
     */
    private JFrame apriUnaVolta(JFrame finestra, Supplier<JFrame> costruttore) {
        if (finestra == null || !finestra.isDisplayable()) {
            try {
                JFrame nuova = costruttore.get();
                nuova.setVisible(true);
                return nuova;

            } catch (BusinessException e) {
                // La finestra si rifiuta di aprirsi, per esempio perche' la
                // sessione non e' quella che si aspetta.
                mostraErrore(e.getMessage());
                return null;

            } catch (RuntimeException e) {
                mostraErrore(TESTO_ERRORE_TECNICO);
                return null;
            }
        }

        finestra.toFront();
        finestra.requestFocus();
        return finestra;
    }

    // ---------- utilita' di composizione ----------

    private JPanel colonna() {
        JPanel pannello = new JPanel();
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        return pannello;
    }

    private JLabel titolo(String testo) {
        JLabel etichetta = new JLabel(testo, SwingConstants.CENTER);
        etichetta.setFont(etichetta.getFont().deriveFont(Font.BOLD, 16f));
        return etichetta;
    }

    private JLabel etichetta(String testo) {
        return new JLabel(testo);
    }

    /** Centra i componenti e impedisce ai campi di allargarsi a dismisura. */
    private void allinea(JPanel pannello) {
        for (Component componente : pannello.getComponents()) {
            if (componente instanceof JPanel || componente instanceof JLabel
                    || componente instanceof JButton || componente instanceof JTextField) {
                ((javax.swing.JComponent) componente).setAlignmentX(Component.CENTER_ALIGNMENT);
            }
            if (componente instanceof JTextField) {
                componente.setMaximumSize(new Dimension(320, 28));
            }
        }
    }

    private void mostraErrore(String testo) {
        etichettaEsito.setText(testo);
        JOptionPane.showMessageDialog(pannelloPrincipale, testo, "Errore",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Costruisce la finestra e la restituisce gia' pronta da mostrare. */
    public JFrame apriMainFrame() {
        JFrame frame = new JFrame("Biblioteca universitaria");
        frame.setContentPane(pannelloPrincipale);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 400);
        frame.setLocationRelativeTo(null);
        return frame;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().apriMainFrame().setVisible(true);
            }
        });
    }
}
