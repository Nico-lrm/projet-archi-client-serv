import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Implémentation du serveur Brico-Merlin
 * Gère les données en base MySQL et traite les requêtes des clients
 */
public class BricoMerlinServer extends UnicastRemoteObject implements BricoMerlinService {

    // Configuration de la base de données
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bricomerlin";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    private Connection connection;

    /**
     * Constructeur du serveur
     */
    public BricoMerlinServer() throws RemoteException {
        super();
        try {
            // Initialisation de la connexion à la base de données
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connexion à la base de données établie");
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion à la base de données: " + e.getMessage());
            throw new RemoteException("Impossible de se connecter à la base de données", e);
        }
    }

    // ========== IMPLÉMENTATION DES MÉTHODES DE GESTION DU STOCK ==========

    @Override
    public Article consulterStock(String reference) throws RemoteException {
        String query = "SELECT * FROM articles WHERE reference = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, reference);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Article(
                        rs.getString("reference"),
                        rs.getString("famille"),
                        rs.getDouble("prix_unitaire"),
                        rs.getInt("stock_disponible")
                );
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation du stock: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }
    }

    @Override
    public List<String> rechercherArticles(String famille) throws RemoteException {
        List<String> references = new ArrayList<>();
        String query = "SELECT reference FROM articles WHERE famille = ? AND stock_disponible > 0";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, famille);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                references.add(rs.getString("reference"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche d'articles: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }

        return references;
    }

    @Override
    public boolean acheterArticle(String reference, int quantite, String clientId) throws RemoteException {
        try {
            connection.setAutoCommit(false);

            // Vérifier le stock disponible
            Article article = consulterStock(reference);
            if (article == null || article.getStockDisponible() < quantite) {
                connection.rollback();
                return false;
            }

            // Mettre à jour le stock
            String updateStock = "UPDATE articles SET stock_disponible = stock_disponible - ? WHERE reference = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateStock)) {
                stmt.setInt(1, quantite);
                stmt.setString(2, reference);
                stmt.executeUpdate();
            }

            // Ajouter à la facture du client
            ajouterLigneFacture(clientId, reference, quantite, article.getPrixUnitaire());

            connection.commit();
            System.out.println("Achat effectué: " + quantite + " x " + reference + " pour client " + clientId);
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Erreur lors du rollback: " + ex.getMessage());
            }
            System.err.println("Erreur lors de l'achat: " + e.getMessage());
            throw new RemoteException("Erreur lors de l'achat", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur lors de la restauration de l'autocommit: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean ajouterStock(String reference, int quantite) throws RemoteException {
        String query = "UPDATE articles SET stock_disponible = stock_disponible + ? WHERE reference = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, quantite);
            stmt.setString(2, reference);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Stock ajouté: " + quantite + " unités pour " + reference);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de stock: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }
    }

    // ========== IMPLÉMENTATION DES MÉTHODES DE GESTION DES FACTURES ==========

    @Override
    public Facture consulterFacture(String clientId) throws RemoteException {
        String queryFacture = "SELECT * FROM factures WHERE client_id = ? AND payee = false";
        String queryLignes = "SELECT * FROM lignes_facture WHERE facture_id = ?";

        try (PreparedStatement stmtFacture = connection.prepareStatement(queryFacture)) {
            stmtFacture.setString(1, clientId);
            ResultSet rsFacture = stmtFacture.executeQuery();

            if (rsFacture.next()) {
                int factureId = rsFacture.getInt("id");
                double montantTotal = rsFacture.getDouble("montant_total");
                Date dateFacturation = rsFacture.getTimestamp("date_facturation");

                // Récupérer les lignes de facture
                List<LigneFacture> lignes = new ArrayList<>();
                try (PreparedStatement stmtLignes = connection.prepareStatement(queryLignes)) {
                    stmtLignes.setInt(1, factureId);
                    ResultSet rsLignes = stmtLignes.executeQuery();

                    while (rsLignes.next()) {
                        lignes.add(new LigneFacture(
                                rsLignes.getString("reference_article"),
                                rsLignes.getInt("quantite"),
                                rsLignes.getDouble("prix_unitaire")
                        ));
                    }
                }

                return new Facture(clientId, montantTotal, lignes, dateFacturation);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation de facture: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }
    }

    @Override
    public boolean payerFacture(String clientId, String modePaiement) throws RemoteException {
        String query = "UPDATE factures SET payee = true, mode_paiement = ?, date_paiement = NOW() WHERE client_id = ? AND payee = false";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, modePaiement);
            stmt.setString(2, clientId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Facture payée pour client " + clientId + " (mode: " + modePaiement + ")");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors du paiement: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }
    }

    @Override
    public double calculerChiffreAffaires(Date date) throws RemoteException {
        String query = "SELECT SUM(montant_total) as chiffre_affaires FROM factures WHERE DATE(date_facturation) = DATE(?) AND payee = true";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setTimestamp(1, new Timestamp(date.getTime()));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("chiffre_affaires");
            }
            return 0.0;
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul du chiffre d'affaires: " + e.getMessage());
            throw new RemoteException("Erreur de base de données", e);
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Ajouter une ligne à la facture d'un client
     */
    private void ajouterLigneFacture(String clientId, String reference, int quantite, double prixUnitaire) throws SQLException {
        // Récupérer ou créer la facture du client
        int factureId = obtenirOuCreerFacture(clientId);

        // Ajouter la ligne de facture
        String insertLigne = "INSERT INTO lignes_facture (facture_id, reference_article, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertLigne)) {
            stmt.setInt(1, factureId);
            stmt.setString(2, reference);
            stmt.setInt(3, quantite);
            stmt.setDouble(4, prixUnitaire);
            stmt.executeUpdate();
        }

        // Mettre à jour le montant total de la facture
        String updateMontant = "UPDATE factures SET montant_total = (SELECT SUM(quantite * prix_unitaire) FROM lignes_facture WHERE facture_id = ?) WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateMontant)) {
            stmt.setInt(1, factureId);
            stmt.setInt(2, factureId);
            stmt.executeUpdate();
        }
    }

    /**
     * Obtenir l'ID de la facture courante du client ou en créer une nouvelle
     */
    private int obtenirOuCreerFacture(String clientId) throws SQLException {
        String queryExistante = "SELECT id FROM factures WHERE client_id = ? AND payee = false";
        try (PreparedStatement stmt = connection.prepareStatement(queryExistante)) {
            stmt.setString(1, clientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        // Créer une nouvelle facture
        String insertFacture = "INSERT INTO factures (client_id, montant_total, date_facturation, payee) VALUES (?, 0, NOW(), false)";
        try (PreparedStatement stmt = connection.prepareStatement(insertFacture, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, clientId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("Impossible de créer une nouvelle facture");
    }

    // ========== MÉTHODE MAIN ==========

    public static void main(String[] args) {
        try {
            // Démarrer le registre RMI
            LocateRegistry.createRegistry(1099);
            System.out.println("Registre RMI démarré sur le port 1099");

            // Créer et enregistrer le serveur
            BricoMerlinServer server = new BricoMerlinServer();
            Naming.rebind("//localhost/BricoMerlinService", server);

            System.out.println("Serveur Brico-Merlin démarré et prêt à recevoir des connexions");
            System.out.println("URL du service: //localhost/BricoMerlinService");

        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}