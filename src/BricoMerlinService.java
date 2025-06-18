import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Date;

/**
 * Interface RMI pour le service Brico-Merlin
 * Définit toutes les opérations disponibles entre client et serveur
 */
public interface BricoMerlinService extends Remote {

    // ========== GESTION DU STOCK ==========

    /**
     * Consulter le stock d'un article
     * @param reference La référence de l'article
     * @return Les informations de l'article (null si inexistant)
     */
    Article consulterStock(String reference) throws RemoteException;

    /**
     * Rechercher des articles par famille
     * @param famille La famille d'articles recherchée
     * @return Liste des références d'articles en stock de cette famille
     */
    List<String> rechercherArticles(String famille) throws RemoteException;

    /**
     * Acheter un article
     * @param reference Référence de l'article
     * @param quantite Quantité à acheter
     * @param clientId Identifiant du client
     * @return true si l'achat est possible, false sinon
     */
    boolean acheterArticle(String reference, int quantite, String clientId) throws RemoteException;

    /**
     * Ajouter du stock pour un produit existant
     * @param reference Référence du produit
     * @param quantite Quantité à ajouter
     * @return true si l'ajout est réussi
     */
    boolean ajouterStock(String reference, int quantite) throws RemoteException;

    // ========== GESTION DES FACTURES ==========

    /**
     * Consulter la facture d'un client
     * @param clientId Identifiant du client
     * @return La facture du client (null si inexistante)
     */
    Facture consulterFacture(String clientId) throws RemoteException;

    /**
     * Payer une facture
     * @param clientId Identifiant du client
     * @param modePaiement Mode de paiement utilisé
     * @return true si le paiement est effectué
     */
    boolean payerFacture(String clientId, String modePaiement) throws RemoteException;

    /**
     * Calculer le chiffre d'affaires à une date donnée
     * @param date Date pour le calcul
     * @return Le montant du chiffre d'affaires
     */
    double calculerChiffreAffaires(Date date) throws RemoteException;
}

/**
 * Classe représentant un article
 */
class Article implements java.io.Serializable {
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

/**
 * Classe représentant une facture
 */
class Facture implements java.io.Serializable {
    private String clientId;
    private double montantTotal;
    private List<LigneFacture> lignesFacture;
    private String modePaiement;
    private Date dateFacturation;
    private boolean payee;

    public Facture(String clientId, double montantTotal, List<LigneFacture> lignesFacture, Date dateFacturation) {
        this.clientId = clientId;
        this.montantTotal = montantTotal;
        this.lignesFacture = lignesFacture;
        this.dateFacturation = dateFacturation;
        this.payee = false;
    }

    // Getters et setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public List<LigneFacture> getLignesFacture() { return lignesFacture; }
    public void setLignesFacture(List<LigneFacture> lignesFacture) { this.lignesFacture = lignesFacture; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public Date getDateFacturation() { return dateFacturation; }
    public void setDateFacturation(Date dateFacturation) { this.dateFacturation = dateFacturation; }

    public boolean isPayee() { return payee; }
    public void setPayee(boolean payee) { this.payee = payee; }

    @Override
    public String toString() {
        return String.format("Facture{client='%s', total=%.2f€, payée=%s, date=%s}",
                clientId, montantTotal, payee ? "Oui" : "Non", dateFacturation);
    }
}

/**
 * Classe représentant une ligne de facture
 */
class LigneFacture implements java.io.Serializable {
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