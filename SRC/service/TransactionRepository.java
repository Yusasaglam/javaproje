package service;

import model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionRepository implements Repository<Transaction, String> {

    private final Map<String, Transaction> depo = new HashMap<>();

    @Override
    public void kaydet(Transaction islem) {
        depo.put(islem.getIslemId(), islem);
    }

    @Override
    public Transaction idIleGetir(String id) {
        return depo.get(id);
    }

    @Override
    public List<Transaction> hepsiniGetir() {
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
