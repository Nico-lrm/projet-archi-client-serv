import java.util.Date;
import java.util.List; /**
 * Classe représentant une facture
 */
public class Facture implements java.io.Serializable {
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
