package service;

import model.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerRepository implements Repository<Customer, String> {

    private final Map<String, Customer> depo = new HashMap<>();

    @Override
    public void kaydet(Customer musteri) {
        depo.put(musteri.getMusteriId(), musteri);
    }

    @Override
    public Customer idIleGetir(String id) {
        return depo.get(id);
    }

    @Override
    public List<Customer> hepsiniGetir() {
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
