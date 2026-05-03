package service;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AktiviteLogServisi implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<ActivityLog> loglar = new ArrayList<>();

    public void kaydet(ActivityLog log) {
        loglar.add(log);
    }

    public List<ActivityLog> gercekLoglar() {
        return loglar.stream()
                .filter(l -> l.kaynak == ActivityLog.Kaynak.GERCEK)
                .collect(Collectors.toList());
    }

    public List<ActivityLog> musteriyeGore(String musteriId, boolean sadeceGercek) {
        return loglar.stream()
                .filter(l -> musteriId.equals(l.musteriId))
                .filter(l -> !sadeceGercek || l.kaynak == ActivityLog.Kaynak.GERCEK)
                .sorted(Comparator.comparing((ActivityLog l) -> l.olusturmaTarihi).reversed())
                .collect(Collectors.toList());
    }

    public List<ActivityLog> tarihFiltrele(List<ActivityLog> kaynak,
                                            LocalDate baslangic, LocalDate bitis) {
        return kaynak.stream()
                .filter(l -> {
                    LocalDate t = l.olusturmaTarihi.toLocalDate();
                    return !t.isBefore(baslangic) && !t.isAfter(bitis);
                })
                .collect(Collectors.toList());
    }

    public List<ActivityLog> islemTipiFiltrele(List<ActivityLog> kaynak,
                                                ActivityLog.IslemTipi tip) {
        if (tip == null) return new ArrayList<>(kaynak);
        return kaynak.stream().filter(l -> l.islemTipi == tip).collect(Collectors.toList());
    }

    public void loklarYukle(List<ActivityLog> yeniLoglar) {
        loglar.clear();
        if (yeniLoglar != null) loglar.addAll(yeniLoglar);
    }

    public List<ActivityLog> getLoglar() {
        return Collections.unmodifiableList(loglar);
    }
}
