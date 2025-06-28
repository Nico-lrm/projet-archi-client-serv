import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Client Brico-Merlin
 * Interface utilisateur pour interagir avec le serveur via RMI
 */
public class BricoMerlinClient {

    private BricoMerlinService service;
    private Scanner scanner;
    private String clientId;
    private SimpleDateFormat dateFormat;

    /**
     * Constructeur du client
     */
    public BricoMerlinClient() {
        this.scanner = new Scanner(System.in);
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    /**
     * Connexion au serveur RMI
     */
    public boolean connecterAuServeur(String serverUrl) {
        try {
            service = (BricoMerlinService) Naming.lookup(serverUrl);
            System.out.println("Connexion au serveur etablie avec succes");
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion au serveur: " + e.getMessage());
            return false;
        }
    }

    /**
     * Demarrer l'interface utilisateur
     */
    public void demarrer() {
        System.out.println("=== BIENVENUE CHEZ BRICO-MERLIN ===");
        System.out.print("Entrez votre identifiant client: ");
        this.clientId = scanner.nextLine();

        boolean continuer = true;
        while (continuer) {
            afficherMenu();
            int choix = lireChoix();
            continuer = traiterChoix(choix);
        }

        System.out.println("Au revoir !");
        scanner.close();
    }

    /**
     * Afficher le menu principal
     */
    private void afficherMenu() {
        System.out.println("\n=== MENU PRINCIPAL ===");
        System.out.println("1. Consulter le stock d'un article");
        System.out.println("2. Rechercher des articles par famille");
        System.out.println("3. Acheter un article");
        System.out.println("4. Ajouter du stock (employe)");
        System.out.println("5. Consulter ma facture");
        System.out.println("6. Payer ma facture");
        System.out.println("7. Calculer le chiffre d'affaires (manager)");
        System.out.println("0. Quitter");
        System.out.print("Votre choix: ");
    }

    /**
     * Lire le choix de l'utilisateur
     */
    private int lireChoix() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Traiter le choix de l'utilisateur
     */
    private boolean traiterChoix(int choix) {
        try {
            switch (choix) {
                case 1:
                    consulterStock();
                    break;
                case 2:
                    rechercherArticles();
                    break;
                case 3:
                    acheterArticle();
                    break;
                case 4:
                    ajouterStock();
                    break;
                case 5:
                    consulterFacture();
                    break;
                case 6:
                    payerFacture();
                    break;
                case 7:
                    calculerChiffreAffaires();
                    break;
                case 0:
                    return false;
                default:
                    System.out.println("Choix invalide. Veuillez reessayer.");
            }
        } catch (RemoteException e) {
            System.err.println("Erreur de communication avec le serveur: " + e.getMessage());
        }
        return true;
    }

    // ========== IMPLeMENTATION DES FONCTIONNALITeS ==========

    /**
     * Consulter le stock d'un article
     */
    private void consulterStock() throws RemoteException {
        System.out.print("Entrez la reference de l'article: ");
        String reference = scanner.nextLine();

        Article article = service.consulterStock(reference);
        if (article != null) {
            System.out.println("\n=== INFORMATIONS ARTICLE ===");
            System.out.println("Reference: " + article.getReference());
            System.out.println("Famille: " + article.getFamille());
            System.out.println("Prix unitaire: " + String.format("%.2fe", article.getPrixUnitaire()));
            System.out.println("Stock disponible: " + article.getStockDisponible() + " unites");
        } else {
            System.out.println("Article non trouve ou reference invalide.");
        }
    }

    /**
     * Rechercher des articles par famille
     */
    private void rechercherArticles() throws RemoteException {
        System.out.print("Entrez la famille d'articles recherchee: ");
        String famille = scanner.nextLine();

        List<String> references = service.rechercherArticles(famille);
        if (!references.isEmpty()) {
            System.out.println("\n=== ARTICLES DISPONIBLES ===");
            System.out.println("Famille: " + famille);
            System.out.println("References en stock:");
            for (String ref : references) {
                System.out.println("- " + ref);
            }
        } else {
            System.out.println("Aucun article trouve pour cette famille ou tous les articles sont en rupture de stock.");
        }
    }

    /**
     * Acheter un article
     */
    private void acheterArticle() throws RemoteException {
        System.out.print("Entrez la reference de l'article a acheter: ");
        String reference = scanner.nextLine();

        // Verifier d'abord le stock disponible
        Article article = service.consulterStock(reference);
        if (article == null) {
            System.out.println("Article non trouve.");
            return;
        }

        System.out.println("Article trouve: " + article.getReference());
        System.out.println("Prix unitaire: " + String.format("%.2fe", article.getPrixUnitaire()));
        System.out.println("Stock disponible: " + article.getStockDisponible() + " unites");

        System.out.print("Quantite a acheter: ");
        try {
            int quantite = Integer.parseInt(scanner.nextLine());

            if (quantite <= 0) {
                System.out.println("Quantite invalide.");
                return;
            }

            if (quantite > article.getStockDisponible()) {
                System.out.println("Stock insuffisant. Quantite disponible: " + article.getStockDisponible());
                return;
            }

            boolean succes = service.acheterArticle(reference, quantite, clientId);
            if (succes) {
                double sousTotal = quantite * article.getPrixUnitaire();
                System.out.println("Achat effectue avec succes!");
                System.out.println("Sous-total: " + String.format("%.2fe", sousTotal));
                System.out.println("L'article a ete ajoute a votre facture.");
            } else {
                System.out.println("Erreur lors de l'achat. Veuillez reessayer.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Quantite invalide.");
        }
    }

    /**
     * Ajouter du stock (fonction employe)
     */
    private void ajouterStock() throws RemoteException {
        System.out.print("Entrez la reference du produit: ");
        String reference = scanner.nextLine();

        System.out.print("Quantite a ajouter: ");
        try {
            int quantite = Integer.parseInt(scanner.nextLine());

            if (quantite <= 0) {
                System.out.println("Quantite invalide.");
                return;
            }

            boolean succes = service.ajouterStock(reference, quantite);
            if (succes) {
                System.out.println("Stock ajoute avec succes!");
                System.out.println(quantite + " unites ajoutees pour le produit " + reference);
            } else {
                System.out.println("Erreur lors de l'ajout de stock. Verifiez la reference du produit.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Quantite invalide.");
        }
    }

    /**
     * Consulter la facture du client
     */
    private void consulterFacture() throws RemoteException {
        Facture facture = service.consulterFacture(clientId);
        if (facture != null) {
            System.out.println("\n=== VOTRE FACTURE ===");
            System.out.println("Client: " + facture.getClientId());
            System.out.println("Date: " + dateFormat.format(facture.getDateFacturation()));
            System.out.println("Statut: " + (facture.isPayee() ? "PAYEE" : "EN ATTENTE"));

            if (facture.getModePaiement() != null) {
                System.out.println("Mode de paiement: " + facture.getModePaiement());
            }

            System.out.println("\n--- DETAIL DES ACHATS ---");
            for (LigneFacture ligne : facture.getLignesFacture()) {
                System.out.printf("%-15s | Qte: %3d | Prix: %6.2fe | Sous-total: %8.2fe%n",
                        ligne.getReferenceArticle(),
                        ligne.getQuantite(),
                        ligne.getPrixUnitaire(),
                        ligne.getSousTotal());
            }

            System.out.println("----------------------------------------");
            System.out.printf("TOTAL: %.2fe%n", facture.getMontantTotal());

            if (!facture.isPayee()) {
                System.out.println("\n⚠Cette facture n'est pas encore payee.");
            }
        } else {
            System.out.println("Aucune facture en cours pour ce client.");
        }
    }

    /**
     * Payer la facture du client
     */
    private void payerFacture() throws RemoteException {
        // Verifier d'abord s'il y a une facture a payer
        Facture facture = service.consulterFacture(clientId);
        if (facture == null) {
            System.out.println("Aucune facture en attente de paiement.");
            return;
        }

        if (facture.isPayee()) {
            System.out.println("Votre facture est deja payee.");
            return;
        }

        System.out.printf("Montant a payer: %.2fe%n", facture.getMontantTotal());
        System.out.println("\nModes de paiement disponibles:");
        System.out.println("1. Carte bancaire");
        System.out.println("2. Especes");
        System.out.println("3. Cheque");
        System.out.println("4. Virement");
        System.out.print("Choisissez votre mode de paiement (1-4): ");

        String modePaiement = "";
        try {
            int choixPaiement = Integer.parseInt(scanner.nextLine());
            switch (choixPaiement) {
                case 1: modePaiement = "Carte bancaire"; break;
                case 2: modePaiement = "Especes"; break;
                case 3: modePaiement = "Cheque"; break;
                case 4: modePaiement = "Virement"; break;
                default:
                    System.out.println("Mode de paiement invalide.");
                    return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Choix invalide.");
            return;
        }

        System.out.print("Confirmer le paiement? (o/n): ");
        String confirmation = scanner.nextLine();

        if (confirmation.equalsIgnoreCase("o") || confirmation.equalsIgnoreCase("oui")) {
            boolean succes = service.payerFacture(clientId, modePaiement);
            if (succes) {
                System.out.println("Paiement effectue avec succes!");
                System.out.println("Mode de paiement: " + modePaiement);
                System.out.printf("Montant paye: %.2fe%n", facture.getMontantTotal());
                System.out.println("Merci pour votre achat!");
            } else {
                System.out.println("Erreur lors du paiement. Veuillez reessayer.");
            }
        } else {
            System.out.println("Paiement annule.");
        }
    }

    /**
     * Calculer le chiffre d'affaires (fonction manager)
     */
    private void calculerChiffreAffaires() throws RemoteException {
        System.out.print("Entrez la date (format DD/MM/YYYY): ");
        String dateStr = scanner.nextLine();

        try {
            Date date = dateFormat.parse(dateStr);
            double chiffreAffaires = service.calculerChiffreAffaires(date);

            System.out.println("\n=== CHIFFRE D'AFFAIRES ===");
            System.out.println("Date: " + dateFormat.format(date));
            System.out.printf("Chiffre d'affaires: %.2f€%n", chiffreAffaires);

            if (chiffreAffaires == 0) {
                System.out.println("Aucune vente enregistree pour cette date.");
            }
        } catch (ParseException e) {
            System.out.println("Format de date invalide. Utilisez le format DD/MM/YYYY.");
        }
    }

    // ========== MeTHODE MAIN ==========

    public static void main(String[] args) {
        BricoMerlinClient client = new BricoMerlinClient();

        // URL du serveur par defaut
        String serverUrl = "//localhost/BricoMerlinService";

        // Permettre de specifier l'URL du serveur en parametre
        if (args.length > 0) {
            serverUrl = args[0];
        }

        System.out.println("Tentative de connexion au serveur: " + serverUrl);

        if (client.connecterAuServeur(serverUrl)) {
            client.demarrer();
        } else {
            System.err.println("Impossible de se connecter au serveur.");
            System.err.println("Verifiez que le serveur est demarre et accessible.");
            System.exit(1);
        }
    }
}