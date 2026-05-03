package service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class ActivityLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum IslemTipi {
        GIRIS, BASARISIZ_GIRIS,
        PARA_YATIRMA, PARA_CEKME, TRANSFER, GERI_AL, KREDI_ODEME,
        HESAP_OLUSTURMA, MUSTERI_OLUSTURMA,
        HESAP_DONDURMA, HESAP_COZ,
        LIMIT_DEGISIMI
    }

    public enum Kaynak { GERCEK, DEMO, SISTEM }

    public final String        logId;
    public final String        musteriId;
    public final String        hesapId;
    public final IslemTipi     islemTipi;
    public final double        miktar;
    public final int           riskOncesi;
    public final int           riskDelta;
    public final int           riskSonrasi;
    public final String        tetiklenenKural;
    public final String        aciklama;
    public final Kaynak        kaynak;
    public final LocalDateTime olusturmaTarihi;

    public ActivityLog(String musteriId, String hesapId, IslemTipi islemTipi,
                       double miktar, int riskOncesi, int riskDelta, int riskSonrasi,
                       String tetiklenenKural, String aciklama, Kaynak kaynak) {
        this.logId           = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.musteriId       = musteriId != null ? musteriId : "SISTEM";
        this.hesapId         = hesapId;
        this.islemTipi       = islemTipi;
        this.miktar          = miktar;
        this.riskOncesi      = riskOncesi;
        this.riskDelta       = riskDelta;
        this.riskSonrasi     = riskSonrasi;
        this.tetiklenenKural = tetiklenenKural != null ? tetiklenenKural : "";
        this.aciklama        = aciklama != null ? aciklama : "";
        this.kaynak          = kaynak;
        this.olusturmaTarihi = LocalDateTime.now();
    }
}
