import java.util.Date;
import java.util.List;

public class Facture implements java.io.Serializable {
    private final String clientId;
    private final double montantTotal;
    private final List<LigneFacture> lignesFacture;
    private final String modePaiement;
    private final Date dateFacturation;
    private final Date datePaiement;
    private final boolean payee;

    public Facture(String clientId, double montantTotal, List<LigneFacture> lignesFacture, Date dateFacturation, Date datePaiement, boolean payee, String modePaiement) {
        this.clientId = clientId;
        this.montantTotal = montantTotal;
        this.lignesFacture = lignesFacture;
        this.dateFacturation = dateFacturation;
        this.datePaiement = datePaiement;
        this.payee = payee;
        this.modePaiement = modePaiement;
    }

    public Facture(String clientId, double montantTotal, List<LigneFacture> lignesFacture, Date dateFacturation) {
        this.clientId = clientId;
        this.montantTotal = montantTotal;
        this.lignesFacture = lignesFacture;
        this.dateFacturation = dateFacturation;
        this.datePaiement = null;
        this.payee = false;
        this.modePaiement = null;
    }

    // Getters
    public String getClientId() { return clientId; }
    public double getMontantTotal() { return montantTotal; }
    public List<LigneFacture> getLignesFacture() { return lignesFacture; }
    public String getModePaiement() { return modePaiement; }
    public Date getDateFacturation() { return dateFacturation; }
    public Date getDatePaiement() { return datePaiement; }
    public boolean isPayee() { return payee; }

    @Override
    public String toString() {
        return String.format("Facture{client='%s', total=%.2fe, payée=%s, date=%s}", clientId, montantTotal, payee ? "Oui" : "Non", dateFacturation);
    }
}
