package service;

import model.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepository implements Repository<Account, String> {

    private final Map<String, Account> depo = new HashMap<>();

    @Override
    public void kaydet(Account hesap) {
        depo.put(hesap.getHesapId(), hesap);
    }

    @Override
    public Account idIleGetir(String id) {
        return depo.get(id);
    }

    @Override
    public List<Account> hepsiniGetir() {
        return new ArrayList<>(depo.values());
    }

    @Override
    public void sil(String id) {
        depo.remove(id);
    }

    @Override
    public void temizle() {
        depo.clear();
    }
}
