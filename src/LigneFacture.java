public class LigneFacture implements java.io.Serializable {
    private final String referenceArticle;
    private final int quantite;
    private final double prixUnitaire;

    public LigneFacture(String referenceArticle, int quantite, double prixUnitaire) {
        this.referenceArticle = referenceArticle;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    // Getters
    public String getReferenceArticle() { return referenceArticle; }
    public int getQuantite() { return quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public double getSousTotal() {
        return quantite * prixUnitaire;
    }

    @Override
    public String toString() {
        return String.format("LigneFacture{ref='%s', qte=%d, prix=%.2fe, sous-total=%.2fe}", referenceArticle, quantite, prixUnitaire, getSousTotal());
    }
}
