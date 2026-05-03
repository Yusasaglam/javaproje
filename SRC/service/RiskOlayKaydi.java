package service;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RiskOlayKaydi implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String           olayId;
    public final String           islemId;          // hangi bankacılık işleminden geldi
    public final String           hesapId;
    public final String           musteriId;
    public final int              puan;
    public final IslemRiskAgirlik agirlik;
    public final LocalDateTime    olusturmaTarihi;
    public       boolean          aktif;             // false → geri alındı, skora dahil edilmez

    public RiskOlayKaydi(String olayId, String islemId, String hesapId,
                          String musteriId, int puan, IslemRiskAgirlik agirlik,
                          LocalDateTime olusturmaTarihi) {
        this.olayId          = olayId;
        this.islemId         = islemId;
        this.hesapId         = hesapId;
        this.musteriId       = musteriId;
        this.puan            = puan;
        this.agirlik         = agirlik;
        this.olusturmaTarihi = olusturmaTarihi;
        this.aktif           = true;
    }
}
