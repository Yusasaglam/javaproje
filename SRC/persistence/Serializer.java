package persistence;

import java.io.*;

public class Serializer {

    public static void serialize(Object nesne, String dosyaYolu) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dosyaYolu))) {
            oos.writeObject(nesne);
        } catch (IOException e) {
            System.err.println("Serileştirme hatası: " + e.getMessage());
        }
    }

    public static Object deserialize(String dosyaYolu) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dosyaYolu))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Seri çözme hatası: " + e.getMessage());
            return null;
        }
    }
}
