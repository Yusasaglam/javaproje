package service;

import java.util.List;

public interface Repository<T, ID> {
    void kaydet(T entity);
    T idIleGetir(ID id);
    List<T> hepsiniGetir();
    void sil(ID id);
    void temizle();
}
