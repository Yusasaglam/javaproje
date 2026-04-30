package persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileLogger {

    private static final String LOG_DOSYASI = "banka_kayit.txt";

    public void kaydet(String mesaj) {
        try (java.io.FileWriter fw = new java.io.FileWriter(LOG_DOSYASI, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("[" + LocalDateTime.now() + "] " + mesaj);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Kayıt hatası: " + e.getMessage());
        }
    }

    public String kayitlariOku() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(LOG_DOSYASI))) {
            String satir;
            while ((satir = br.readLine()) != null) {
                sb.append(satir).append("\n");
            }
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }
}
