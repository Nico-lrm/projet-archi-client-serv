import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Date;

/**
 * Interface RMI
 * Définit toutes les opérations disponibles entre client et serveur
 */
public interface BricoMerlinService extends Remote {
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
