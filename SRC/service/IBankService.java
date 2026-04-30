package service;

import model.Account;
import model.Customer;
import model.HesapLimiti;
import model.SupheSebebi;

import java.util.List;

public interface IBankService {
    Customer musteriOlustur(String ad, String eposta);
    Account  hesapOlustur(String musteriId, String tur, double baslangicBakiye);
    boolean  paraYatir(String hesapId, double miktar);
    boolean  paraCek(String hesapId, double miktar);
    boolean  transferYap(String kaynakId, String hedefId, double miktar);
    Customer getMusteri(String musteriId);
    Account  getHesap(String hesapId);
    List<Customer> tumMusteriler();
    List<Account>  tumHesaplar();
    boolean        hesapMusteriyeAitMi(String hesapId, String musteriId);
    List<Account>  musteriHesaplari(String musteriId);

    void        limitGuncelle(String hesapId, HesapLimiti limit);
    HesapLimiti getHesapLimiti(String hesapId);
    double      getGunlukCekimLimiti(String hesapId);
    double      getGunlukTransferLimiti(String hesapId);

    SupheSebebi        supheSebebiGetir(String hesapId);
    List<SupheSebebi>  tumSupheSebebleri();
}
