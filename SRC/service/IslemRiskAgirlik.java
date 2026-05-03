package service;

public enum IslemRiskAgirlik {
    NAKIT_CEKIM  (1.0,  6),   // nakit: anonim, geri döndürülemez
    DIS_TRANSFER (1.0,  6),   // dış havale: izlenebilir ama hedef belirsiz
    IC_TRANSFER  (0.5, 10),   // banka içi: tam izlenebilir
    PARA_YATIRMA (0.1, 12),   // yatırma: alıcısınız, risk neredeyse yok
    DOVIZ_ISLEM  (1.0,  6),   // döviz: kur riski
    KREDI_CEKIM  (0.8,  7),   // kredi: banka parası, izlenebilir
    DAVRANISSAL  (1.0,  6);   // velocity / günlük limit ihlalleri

    public final double carpan;      // baz puanla çarpılır
    public final int    gunlukDecay; // günde kaç puan azalır

    IslemRiskAgirlik(double carpan, int gunlukDecay) {
        this.carpan      = carpan;
        this.gunlukDecay = gunlukDecay;
    }
}
