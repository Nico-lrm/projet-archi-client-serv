/**
 * Classe représentant un article
 */
public class Article implements java.io.Serializable {
    private String reference;
    private String famille;
    private double prixUnitaire;
    private int stockDisponible;

    public Article(String reference, String famille, double prixUnitaire, int stockDisponible) {
        this.reference = reference;
        this.famille = famille;
        this.prixUnitaire = prixUnitaire;
        this.stockDisponible = stockDisponible;
    }

    // Getters et setters
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getFamille() { return famille; }
    public void setFamille(String famille) { this.famille = famille; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }

    @Override
    public String toString() {
        return String.format("Article{ref='%s', famille='%s', prix=%.2f€, stock=%d}",
                reference, famille, prixUnitaire, stockDisponible);
    }
}
