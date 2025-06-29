public class Article implements java.io.Serializable {
    private final String reference;
    private final String famille;
    private final double prixUnitaire;
    private final int stockDisponible;

    public Article(String reference, String famille, double prixUnitaire, int stockDisponible) {
        this.reference = reference;
        this.famille = famille;
        this.prixUnitaire = prixUnitaire;
        this.stockDisponible = stockDisponible;
    }

    // Getters
    public String getReference() { return reference; }
    public String getFamille() { return famille; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public int getStockDisponible() { return stockDisponible; }

    @Override
    public String toString() {
        return String.format("Article{ref='%s', famille='%s', prix=%.2fe, stock=%d}", reference, famille, prixUnitaire, stockDisponible);
    }
}
