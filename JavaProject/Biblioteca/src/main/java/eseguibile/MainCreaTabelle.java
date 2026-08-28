package eseguibile;

import database.JpaUtil;
import jakarta.persistence.EntityManager;

public class MainCreaTabelle {

    public static void main(String[] args) {
        System.out.println("Avvio di Hibernate in corso...");

        // Questa singola riga accende la Factory e genera le tabelle in base al persistence.xml
        EntityManager em = JpaUtil.getInstance().getEntityManager();

        System.out.println("Tabelle generate con successo su MySQL!");

        // Chiudiamo le risorse in modo pulito
        em.close();
        JpaUtil.getInstance().close();
    }
}