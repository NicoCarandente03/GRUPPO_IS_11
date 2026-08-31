package dto;

/**
 * Dati di un'area nella forma che serve alle finestre.
 */
public class AreaDTO {

    private final String idArea;
    private final String tipo;
    private final int numPostazioni;

    public AreaDTO(String idArea, String tipo, int numPostazioni) {
        this.idArea = idArea;
        this.tipo = tipo;
        this.numPostazioni = numPostazioni;
    }

    public String getIdArea() {
        return idArea;
    }

    public String getTipo() {
        return tipo;
    }

    public int getNumPostazioni() {
        return numPostazioni;
    }
}
