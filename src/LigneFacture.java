/**
 * Classe représentant une ligne de facture
 */
public class LigneFacture implements java.io.Serializable {
    private String referenceArticle;
    private int quantite;
    private double prixUnitaire;

    public LigneFacture(String referenceArticle, int quantite, double prixUnitaire) {
        this.referenceArticle = referenceArticle;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    // Getters et setters
    public String getReferenceArticle() { return referenceArticle; }
    public void setReferenceArticle(String referenceArticle) { this.referenceArticle = referenceArticle; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public double getSousTotal() {
        return quantite * prixUnitaire;
    }

    @Override
    public String toString() {
        return String.format("LigneFacture{ref='%s', qté=%d, prix=%.2f€, sous-total=%.2f€}",
                referenceArticle, quantite, prixUnitaire, getSousTotal());
    }
}
