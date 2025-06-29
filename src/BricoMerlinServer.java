import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Gere les donnees en base MySQL et traite les requêtes des clients
 * Initialise le service RMI (plutot que de devoir lancer le service + serveur)
*/
public class BricoMerlinServer extends UnicastRemoteObject implements BricoMerlinService {

    // Configuration de la base de donnees
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "bricomerlin";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private final Connection connection;

    public BricoMerlinServer() throws RemoteException {
        super();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            creerBaseDeDonnees();
            connection = DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
            initialiserBaseDeDonnees();
            System.out.println("Connexion a la base de donnees etablie");
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion a la base de donnees: " + e.getMessage());
            throw new RemoteException("Impossible de se connecter a la base de donnees", e);
        }
    }

    // Service

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
            throw new RemoteException("Erreur de base de donnees", e);
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
            throw new RemoteException("Erreur de base de donnees", e);
        }

        return references;
    }

    @Override
    public boolean acheterArticle(String reference, int quantite, String clientId) throws RemoteException {
        try {
            connection.setAutoCommit(false);

            // Verifier le stock disponible
            Article article = consulterStock(reference);
            if (article == null || article.getStockDisponible() < quantite) {
                connection.rollback();
                return false;
            }

            // Mettre a jour le stock
            String updateStock = "UPDATE articles SET stock_disponible = stock_disponible - ? WHERE reference = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateStock)) {
                stmt.setInt(1, quantite);
                stmt.setString(2, reference);
                stmt.executeUpdate();
            }

            // Ajouter a la facture du client
            ajouterLigneFacture(clientId, reference, quantite, article.getPrixUnitaire());

            connection.commit();
            System.out.println("Achat effectue: " + quantite + " x " + reference + " pour client " + clientId);
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
                System.out.println("Stock ajoute: " + quantite + " unites pour " + reference);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de stock: " + e.getMessage());
            throw new RemoteException("Erreur de base de donnees", e);
        }
    }

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

                // Recuperer les lignes de facture
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
            throw new RemoteException("Erreur de base de donnees", e);
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
                System.out.println("Facture payee pour client " + clientId + " (mode: " + modePaiement + ")");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors du paiement: " + e.getMessage());
            throw new RemoteException("Erreur de base de donnees", e);
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
            throw new RemoteException("Erreur de base de donnees", e);
        }
    }

    // Utilitaire

    private void ajouterLigneFacture(String clientId, String reference, int quantite, double prixUnitaire) throws SQLException {
        // Recuperer ou creer la facture du client
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

        // Mettre a jour le montant total de la facture
        String updateMontant = "UPDATE factures SET montant_total = (SELECT SUM(quantite * prix_unitaire) FROM lignes_facture WHERE facture_id = ?) WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateMontant)) {
            stmt.setInt(1, factureId);
            stmt.setInt(2, factureId);
            stmt.executeUpdate();
        }
    }

    private int obtenirOuCreerFacture(String clientId) throws SQLException {
        String queryExistante = "SELECT id FROM factures WHERE client_id = ? AND payee = false";
        try (PreparedStatement stmt = connection.prepareStatement(queryExistante)) {
            stmt.setString(1, clientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        // Creer une nouvelle facture
        String insertFacture = "INSERT INTO factures (client_id, montant_total, date_facturation, payee) VALUES (?, 0, NOW(), false)";
        try (PreparedStatement stmt = connection.prepareStatement(insertFacture, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, clientId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("Impossible de creer une nouvelle facture");
    }

    // BDD

    private void creerBaseDeDonnees() throws SQLException {
        try (Connection tempConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = tempConnection.createStatement()) {

            // Créer la base de données si elle n'existe pas
            String createDatabase = "CREATE DATABASE IF NOT EXISTS " + DB_NAME +
                    " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.execute(createDatabase);

            System.out.println("Base de donnees '" + DB_NAME + "' verifiee/creee avec succes");
        }
    }

    private void initialiserBaseDeDonnees() {
        try {
            Statement stmt = connection.createStatement();

            // Création de la table articles
            String createArticlesTable = """
                CREATE TABLE IF NOT EXISTS articles (
                    reference VARCHAR(50) PRIMARY KEY,
                    famille VARCHAR(100) NOT NULL,
                    prix_unitaire DECIMAL(10,2) NOT NULL,
                    stock_disponible INT NOT NULL DEFAULT 0
                )
            """;

            // Création de la table factures
            String createFacturesTable = """
                CREATE TABLE IF NOT EXISTS factures (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    client_id VARCHAR(100) NOT NULL,
                    montant_total DECIMAL(10,2) NOT NULL,
                    mode_paiement VARCHAR(50),
                    date_facturation DATETIME NOT NULL,
                    date_paiement DATETIME DEFAULT NULL,
                    payee BOOLEAN DEFAULT FALSE
                )
            """;

            // Création de la table lignes_facture
            String createLignesFactureTable = """
                CREATE TABLE IF NOT EXISTS lignes_facture (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    facture_id INT,
                    reference_article VARCHAR(50),
                    quantite INT NOT NULL,
                    prix_unitaire DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (facture_id) REFERENCES factures(id),
                    FOREIGN KEY (reference_article) REFERENCES articles(reference)
                )
            """;

            // Exécuter les créations de tables
            stmt.execute(createArticlesTable);
            stmt.execute(createFacturesTable);
            stmt.execute(createLignesFactureTable);

            System.out.println("Tables creees avec succes !");

            insertionDonneesTest(stmt);

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation : " + e.getMessage());
        }
    }

    private void insertionDonneesTest(Statement stmt) throws SQLException {
        // Vérifier si des données existent déjà
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM articles");
        rs.next();
        int count = rs.getInt(1);
        if (count == 0) {
            System.out.println("Insertion de donnees de test...");
            String[] insertArticles = {
                    "INSERT INTO articles VALUES ('VIS001', 'Visserie', 0.15, 1000)",
                    "INSERT INTO articles VALUES ('VIS002', 'Visserie', 0.25, 500)",
                    "INSERT INTO articles VALUES ('OUT001', 'Outillage', 25.90, 20)",
                    "INSERT INTO articles VALUES ('OUT002', 'Outillage', 45.50, 15)",
                    "INSERT INTO articles VALUES ('PEI001', 'Peinture', 12.99, 50)",
                    "INSERT INTO articles VALUES ('PEI002', 'Peinture', 18.75, 30)"
            };
            for (String sql : insertArticles) {
                stmt.execute(sql);
            }
            System.out.println("Donnees de test inserees !");
        }
    }

    // Main

    public static void main(String[] args) {
        try {
            // Demarrer le registre RMI
            LocateRegistry.createRegistry(1099);
            System.out.println("Registre RMI demarre sur le port 1099");

            // Creer et enregistrer le serveur
            BricoMerlinServer server = new BricoMerlinServer();
            Naming.rebind("//localhost/BricoMerlinService", server);

            System.out.println("Serveur Brico-Merlin demarre et prêt a recevoir des connexions");
            System.out.println("URL du service: //localhost/BricoMerlinService");

        } catch (Exception e) {
            System.err.println("Erreur lors du demarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}