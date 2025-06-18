import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lanceur principal pour l'application Brico-Merlin
 * Permet de démarrer le serveur, le client, ou les deux
 */
public class BricoMerlinLauncher {

    private static final String SEPARATOR = "=".repeat(60);
    private static Scanner scanner = new Scanner(System.in);
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        afficherBanniere();

        if (args.length > 0) {
            // Mode ligne de commande
            traiterArgumentsLigneCommande(args);
        } else {
            // Mode interactif
            demarrerModeInteractif();
        }
    }

    /**
     * Affiche la bannière de l'application
     */
    private static void afficherBanniere() {
        System.out.println(SEPARATOR);
        System.out.println("🔨 BRICO-MERLIN - Système de Gestion de Stock 🔨");
        System.out.println("    Architecture Client-Serveur avec RMI");
        System.out.println(SEPARATOR);
        System.out.println();
    }

    /**
     * Mode interactif pour choisir ce qu'on veut lancer
     */
    private static void demarrerModeInteractif() {
        boolean continuer = true;

        while (continuer) {
            afficherMenuPrincipal();
            int choix = lireChoix();
            continuer = traiterChoixMenu(choix);

            if (continuer) {
                System.out.println("\nAppuyez sur Entrée pour continuer...");
                scanner.nextLine();
            }
        }

        arreterServices();
    }

    /**
     * Affiche le menu principal
     */
    private static void afficherMenuPrincipal() {
        System.out.println("\n" + "─".repeat(40));
        System.out.println("           MENU PRINCIPAL");
        System.out.println("─".repeat(40));
        System.out.println("1. 🖥️  Démarrer le SERVEUR uniquement");
        System.out.println("2. 💻 Démarrer un CLIENT uniquement");
        System.out.println("3. 🔄 Démarrer SERVEUR + CLIENT");
        System.out.println("4. 🌐 Démarrer plusieurs CLIENTS");
        System.out.println("5. ℹ️  Afficher les informations de connexion");
        System.out.println("6. 🛠️  Tester la connexion au serveur");
        System.out.println("0. ❌ Quitter");
        System.out.println("─".repeat(40));
        System.out.print("Votre choix: ");
    }

    /**
     * Lit le choix de l'utilisateur
     */
    private static int lireChoix() {
        try {
            String input = scanner.nextLine().trim();
            return input.isEmpty() ? -1 : Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Traite le choix du menu
     */
    private static boolean traiterChoixMenu(int choix) {
        switch (choix) {
            case 1:
                demarrerServeur();
                return true;
            case 2:
                demarrerClient();
                return true;
            case 3:
                demarrerServeurEtClient();
                return true;
            case 4:
                demarrerPlusieursClients();
                return true;
            case 5:
                afficherInformationsConnexion();
                return true;
            case 6:
                testerConnexionServeur();
                return true;
            case 0:
                System.out.println("👋 Au revoir !");
                return false;
            default:
                System.out.println("❌ Choix invalide. Veuillez réessayer.");
                return true;
        }
    }

    /**
     * Traite les arguments de ligne de commande
     */
    private static void traiterArgumentsLigneCommande(String[] args) {
        String mode = args[0].toLowerCase();

        switch (mode) {
            case "server":
            case "serveur":
                System.out.println("🖥️  Démarrage du serveur...");
                demarrerServeur();
                break;

            case "client":
                String serverUrl = args.length > 1 ? args[1] : "//localhost/BricoMerlinService";
                System.out.println("💻 Démarrage du client...");
                demarrerClient(serverUrl);
                break;

            case "both":
            case "les-deux":
                System.out.println("🔄 Démarrage serveur + client...");
                demarrerServeurEtClient();
                break;

            default:
                afficherAideLigneCommande();
        }
    }

    /**
     * Démarre uniquement le serveur
     */
    private static void demarrerServeur() {
        System.out.println("\n🖥️  === DÉMARRAGE DU SERVEUR ===");

        executor.submit(() -> {
            try {
                System.out.println("📡 Initialisation du serveur Brico-Merlin...");
                BricoMerlinServer.main(new String[0]);
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du démarrage du serveur: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Attendre un peu que le serveur se lance
        try {
            Thread.sleep(3000);
            System.out.println("✅ Serveur démarré !");
            System.out.println("   URL: //localhost/BricoMerlinService");
            System.out.println("   Port RMI: 1099");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Démarre uniquement un client
     */
    private static void demarrerClient() {
        System.out.print("\n🌐 URL du serveur (Entrée = localhost): ");
        String serverUrl = scanner.nextLine().trim();
        if (serverUrl.isEmpty()) {
            serverUrl = "//localhost/BricoMerlinService";
        }
        demarrerClient(serverUrl);
    }

    /**
     * Démarre un client avec une URL spécifique
     */
    private static void demarrerClient(String serverUrl) {
        System.out.println("\n💻 === DÉMARRAGE DU CLIENT ===");
        System.out.println("🔗 Connexion à: " + serverUrl);

        try {
            BricoMerlinClient client = new BricoMerlinClient();
            if (client.connecterAuServeur(serverUrl)) {
                client.demarrer();
            } else {
                System.err.println("❌ Impossible de se connecter au serveur.");
                System.err.println("💡 Vérifiez que le serveur est démarré.");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage du client: " + e.getMessage());
        }
    }

    /**
     * Démarre le serveur puis un client
     */
    private static void demarrerServeurEtClient() {
        System.out.println("\n🔄 === DÉMARRAGE SERVEUR + CLIENT ===");

        // Démarrer le serveur
        demarrerServeur();

        // Attendre que le serveur soit prêt
        System.out.println("⏳ Attente du démarrage complet du serveur...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Démarrer le client
        System.out.println("💻 Démarrage du client...");
        demarrerClient("//localhost/BricoMerlinService");
    }

    /**
     * Démarre plusieurs clients
     */
    private static void demarrerPlusieursClients() {
        System.out.print("\n🔢 Nombre de clients à démarrer (1-5): ");
        int nombreClients;

        try {
            nombreClients = Integer.parseInt(scanner.nextLine().trim());
            if (nombreClients < 1 || nombreClients > 5) {
                System.out.println("❌ Nombre invalide. Utilisation de 2 clients par défaut.");
                nombreClients = 2;
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Nombre invalide. Utilisation de 2 clients par défaut.");
            nombreClients = 2;
        }

        System.out.print("🌐 URL du serveur (Entrée = localhost): ");
        String serverUrl = scanner.nextLine().trim();
        if (serverUrl.isEmpty()) {
            serverUrl = "//localhost/BricoMerlinService";
        }

        final String finalServerUrl = serverUrl;

        System.out.println("\n🚀 Démarrage de " + nombreClients + " clients...");

        for (int i = 1; i <= nombreClients; i++) {
            final int clientNum = i;

            executor.submit(() -> {
                try {
                    Thread.sleep(1000 * clientNum); // Décaler les démarrages
                    System.out.println("💻 Démarrage du client #" + clientNum);

                    BricoMerlinClient client = new BricoMerlinClient();
                    if (client.connecterAuServeur(finalServerUrl)) {
                        System.out.println("✅ Client #" + clientNum + " connecté");
                        client.demarrer();
                    } else {
                        System.err.println("❌ Client #" + clientNum + " : connexion échouée");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur client #" + clientNum + ": " + e.getMessage());
                }
            });
        }
    }

    /**
     * Affiche les informations de connexion
     */
    private static void afficherInformationsConnexion() {
        System.out.println("\n📋 === INFORMATIONS DE CONNEXION ===");
        System.out.println("🖥️  Serveur:");
        System.out.println("   • URL RMI: //localhost/BricoMerlinService");
        System.out.println("   • Port: 1099");
        System.out.println("   • Base de données: MySQL sur localhost:3306");
        System.out.println("   • Schéma: bricomerlin");
        System.out.println();
        System.out.println("💻 Client:");
        System.out.println("   • Se connecte automatiquement au serveur local");
        System.out.println("   • Peut spécifier une URL distante");
        System.out.println();
        System.out.println("🌐 Connexion distante:");
        System.out.println("   • Format: //adresse-ip/BricoMerlinService");
        System.out.println("   • Exemple: //192.168.1.100/BricoMerlinService");
    }

    /**
     * Teste la connexion au serveur
     */
    private static void testerConnexionServeur() {
        System.out.print("\n🌐 URL du serveur à tester (Entrée = localhost): ");
        String serverUrl = scanner.nextLine().trim();
        if (serverUrl.isEmpty()) {
            serverUrl = "//localhost/BricoMerlinService";
        }

        System.out.println("\n🔍 Test de connexion à: " + serverUrl);

        try {
            BricoMerlinClient testClient = new BricoMerlinClient();
            boolean connected = testClient.connecterAuServeur(serverUrl);

            if (connected) {
                System.out.println("✅ Connexion réussie !");
                System.out.println("   Le serveur répond correctement.");
            } else {
                System.out.println("❌ Connexion échouée !");
                System.out.println("   Vérifiez que le serveur est démarré.");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur de connexion: " + e.getMessage());
        }
    }

    /**
     * Affiche l'aide pour la ligne de commande
     */
    private static void afficherAideLigneCommande() {
        System.out.println("\n📖 === AIDE LIGNE DE COMMANDE ===");
        System.out.println("Usage:");
        System.out.println("  java BricoMerlinLauncher                    # Mode interactif");
        System.out.println("  java BricoMerlinLauncher server             # Serveur uniquement");
        System.out.println("  java BricoMerlinLauncher client [url]       # Client uniquement");
        System.out.println("  java BricoMerlinLauncher both               # Serveur + Client");
        System.out.println();
        System.out.println("Exemples:");
        System.out.println("  java BricoMerlinLauncher server");
        System.out.println("  java BricoMerlinLauncher client");
        System.out.println("  java BricoMerlinLauncher client //192.168.1.100/BricoMerlinService");
        System.out.println("  java BricoMerlinLauncher both");
    }

    /**
     * Arrête tous les services
     */
    private static void arreterServices() {
        System.out.println("\n🛑 Arrêt des services...");
        executor.shutdownNow();
        scanner.close();
        System.out.println("✅ Services arrêtés.");
    }
}