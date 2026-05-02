package model;

public class DovizHesabi extends Account {

    private static final long serialVersionUID = 1L;

    public enum ParaBirimi { USD, EUR, GBP }

    private final ParaBirimi paraBirimi;
    private double           dovizKuru; // 1 yabancı para = kaç TL

    public DovizHesabi(String hesapId, String sahibiId,
                       double bakiyeYabanciPara, ParaBirimi paraBirimi, double dovizKuru) {
        super(hesapId, sahibiId, bakiyeYabanciPara);
        this.paraBirimi = paraBirimi;
        this.dovizKuru  = dovizKuru;
    }

    /** Bakiyeyi TL cinsine çevirir. */
    public double getBakiyeTL() { return bakiye * dovizKuru; }

    /** Kuru günceller (anlık kur yenilemesi). */
    public void dovizKuruGuncelle(double yeniKur) { this.dovizKuru = yeniKur; }

    @Override
    public String getHesapTuru() { return "DÖVİZ-" + paraBirimi.name(); }

    public ParaBirimi getParaBirimi() { return paraBirimi; }
    public double     getDovizKuru()  { return dovizKuru; }

    public String getParaBirimiSimgesi() {
        switch (paraBirimi) {
            case USD: return "$";
            case EUR: return "€";
            case GBP: return "£";
            default:  return "?";
        }
    }
}
