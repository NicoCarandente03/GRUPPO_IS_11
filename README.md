# Sistema di Prenotazione Sale Studio

Progetto del corso di Ingegneria del Software, Universita' degli Studi di Napoli
Federico II, A.A. 2025-26. Gruppo 11.

L'applicazione gestisce la prenotazione delle postazioni nelle sale studio di una
biblioteca. Lo studente consulta le sale, le fasce orarie e le postazioni libere,
prenota, annulla e conferma la propria presenza con il check-in. Il bibliotecario
crea le sale studio con le relative aree e consulta lo storico delle prenotazioni.

## Struttura della repository

```
Documentation/      Documentazione.docx, il documento di progetto
VisualParadigm/     VisualParadigmProject.vpp, i diagrammi UML
JavaProject/
  Biblioteca/       progetto Maven con il codice sorgente e i test
```

## Requisiti

- JDK 21 o superiore
- Maven 3.9 o superiore
- MySQL 8

Le dipendenze (Hibernate ORM 6.6, MySQL Connector/J 9.4, forms_rt di IntelliJ,
JUnit 5 e Mockito) sono dichiarate nel `pom.xml` e vengono scaricate da Maven.

## Configurazione

I parametri di connessione non stanno nel `persistence.xml` ma in un file escluso
da git, cosi' ognuno usa le proprie credenziali e la password non finisce nella
repository.

1. creare lo schema vuoto su MySQL:

   ```sql
   CREATE DATABASE biblioteca_db;
   ```

2. copiare il file di esempio e compilarlo:

   ```
   cd JavaProject/Biblioteca/src/main/resources
   cp db.properties.example db.properties
   ```

   ```properties
   db.host=localhost
   db.port=3306
   db.name=biblioteca_db
   db.user=il_proprio_utente
   db.password=la_propria_password
   db.params=serverTimezone=UTC
   ```

Se `db.properties` manca o contiene ancora i valori di esempio, l'avvio si
interrompe subito con un messaggio esplicito.

In `config.properties` stanno invece i due parametri temporali, gia' valorizzati:

- `annullamento.limite.minuti`, quanti minuti prima dell'inizio della fascia si
  puo' ancora annullare (60)
- `checkin.intervallo.minuti`, ampiezza della finestra di check-in prima e dopo
  l'inizio della fascia (30)

## Primo avvio

Dalla cartella `JavaProject/Biblioteca`:

1. `mvn compile`
2. eseguire `eseguibile.MainCreaTabelle`, che lascia creare le tabelle a
   Hibernate a partire dalle entity
3. eseguire `eseguibile.DatiTestBiblioteca`, che inserisce i dati di prova del
   piano di test (non fa nulla se sono gia' presenti)
4. eseguire `boundary.MainFrame`, la finestra di avvio dell'applicazione

Lo schema viene generato con `hibernate.hbm2ddl.auto=update`: le colonne mancanti
vengono aggiunte, ma niente viene mai rinominato o cancellato. Per rigenerarlo da
zero serve un `DROP DATABASE` seguito dalla ricreazione.

### Dati di prova

- bibliotecario `B1234`, marco.esposito@unina.it, password `biblio2026`
- studente `N46001234`, giulia.romano@studenti.unina.it, password `studente1`
- studente `N46004321`, luca.desimone@studenti.unina.it, password `studente2`
- sale `S001` Sala Lettura A (aree A001 silenziosa e A002 consultazione) e `S002`
  Sala Informatica (area A003 lavoro di gruppo)
- prenotazioni da `P001` a `P005`, una per ogni stato che serve ai casi di test

## Architettura

Pattern BCED, con in piu' un package `dto` che disaccoppia la presentazione dal
modello: i Controller non restituiscono mai entity al Boundary.

| Package      | Ruolo |
|--------------|-------|
| `boundary`   | interfacce di Boundary e finestre Swing che le implementano |
| `controller` | logica applicativa, un singleton per area funzionale |
| `entity`     | classi di dominio, mappate con JPA |
| `dto`        | oggetti di trasporto verso il Boundary |
| `database`   | `DBManager` per la connessione, `GestorePersistenza` per le query |
| `external`   | servizio esterno di notifiche, con adapter e mock |
| `eccezioni`  | `BusinessException` e `DataAccessException` |
| `eseguibile` | creazione delle tabelle e caricamento dei dati di prova |

Il Boundary non tocca mai entity ne' il livello database: passa sempre da un
Controller e riceve indietro un DTO. I Controller sollevano `BusinessException`
con il messaggio da mostrare all'utente, che il Boundary intercetta.

`DBManager` tiene una sola `EntityManagerFactory` e crea un `EntityManager` per
operazione, con i metodi `esegui` per le letture e `eseguiInTransazione` per le
scritture, che fa il rollback in caso di errore.

Il servizio di notifiche e' dietro l'interfaccia `ServizioDiNotifiche`, cosi' nei
test si usa `ServizioDiNotificheMock` al posto dell'adapter reale.

### Boundary

| Classe               | Contenuto |
|----------------------|-----------|
| `MainFrame`          | accesso e smistamento fra area studente e area bibliotecario |
| `FormLogin`          | accesso |
| `FormRegistrazione`  | registrazione di studenti e bibliotecari |
| `FormPrenotazione`   | consultazione delle disponibilita' e nuova prenotazione |
| `FormProfiloStudente`| prenotazioni dello studente, annullamento e check-in |
| `FormGestioneSale`   | creazione di una sala studio |
| `FormMonitoraggio`   | storico delle prenotazioni, con filtro per sala e per studente |

Le finestre sono scritte in Swing. Se si usa il GUI Designer di IntelliJ, la
generazione va impostata su "Java source code" e non sui class binari, altrimenti
il progetto si compila solo dentro l'IDE.

### Modello dati

Sei tabelle: `studente`, `bibliotecario`, `salastudio`, `area`, `postazione`,
`prenotazione`.

`Utente` e' una `@MappedSuperclass` senza tabella propria, con una tabella per
sottoclasse: la chiave e' la matricola per lo studente e il codice identificativo
per il bibliotecario. `Notifica` non e' persistente: le notifiche ricevute
restano in una lista transiente dello studente.

Le chiavi ridondanti sono state evitate. Da una prenotazione si risale ad area e
sala navigando `getPostazione().getArea().getSalaStudio()`.

## Regole di dominio

- fasce orarie prenotabili: sei blocchi da due ore, da `08:00-10:00` a
  `18:00-20:00`, elencati in `entity.FasceOrarie`
- stati di una prenotazione: `ATTIVA`, `CONFERMATA`, `ANNULLATA`, `SCADUTA`
- una prenotazione si puo' annullare solo se e' attiva, se appartiene a chi la
  richiede e se mancano piu' di 60 minuti all'inizio della fascia
- la finestra di check-in e' simmetrica attorno all'inizio della fascia, 30
  minuti prima e 30 dopo
- tipi di area ammessi: silenziosa, consultazione, lavoro di gruppo. Una sala ha
  sempre almeno un'area: se non ne viene indicata nessuna, ne viene creata una di
  tipo generica che copre l'intera sala, e le postazioni vengono distribuite
  equamente fra le aree
- gli identificativi sono progressivi e generati dal sistema: `S001` per le sale,
  `A001` per le aree, `P001` per le prenotazioni, `P-A001-01` per le postazioni

## Casi d'uso realizzati

- Registrazione
- Log-in
- Consultazione Disponibilita' Sale Studio
- Visualizzazione Fasce Orarie
- Effettua Prenotazione
- Annullamento Prenotazione
- Check-in
- Creazione Aula Studio
- Consultazione Storico Prenotazioni

Le operazioni dichiarate nelle interfacce di Boundary ma non ancora realizzate
(modifica ed eliminazione di una sala, monitoraggio della sala e dell'andamento
dei servizi, consultazione degli accessi e delle notifiche) rispondono con un
avviso di funzionalita' non disponibile.

Finche' il Log-out non e' realizzato, l'identita' di chi opera viene passata ai
Controller come parametro (la matricola dello studente o il codice del
bibliotecario). Quando ci sara' la sessione, quel parametro sparira' dalle firme.

## Test

55 test JUnit 5, con Mockito per sostituire `GestorePersistenza`: si eseguono
senza database.

```
cd JavaProject/Biblioteca
mvn test
```

| Suite | Contenuto |
|-------|-----------|
| `AutenticazioneControllerTest` | registrazione e log-in |
| `GestionePrenotazioneControllerTest` | annullamento e check-in |
| `GestioneSaleControllerTest` | creazione aula studio |
| `MonitoraggioSaleControllerTest` | storico delle prenotazioni |
| `ControlliAggiuntiviTest` | controlli non previsti dal piano di test |
| `PrenotazioneTest` | regole di stato dell'entity |

I casi delle suite funzionali portano gli stessi identificativi (TC1, TC2, ...)
del piano di test della documentazione e usano gli stessi dati.

I metodi che dipendono dall'ora corrente hanno una variante che riceve l'istante
di riferimento come parametro, usata dai test per non dipendere dall'orologio di
sistema.

## Note

- `db.properties`, `target/` e `.idea/` non entrano nella repository
- l'artifactId nel `pom.xml` e' ancora quello del progetto di esempio da cui si
  e' partiti
