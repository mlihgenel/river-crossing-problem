public class Person {                                      //Bu sınıftan hacker ve serf ler türetilir.

    public String ad;                                      //hacker ve serler için ortak instance variable lar
    public Semaphors smphrs;
    public boolean isCaptain = false;

    public Person(String ad, Semaphors smphrs) {          //construter
        this.ad = ad;
        this.smphrs = smphrs;
    }

    public void Board() throws InterruptedException {     // her thredin başvurması gereken Board() fonksiyonu
        // Boş - alt sınıflar override edebilir
    }

    public void rowBoat() throws InterruptedException {   // sadece bir thread in yani kaptanın başvurması gereken rowBoat() fonksiyonu

        Logger.separator();
        Logger.info("🚣 TEKNE KALKIYOR!");
        Logger.info("Kaptan: " + ad);
        Logger.separator();
        System.out.println();

        // Geçiş sayısını arttır
        smphrs.incrementCrossingCount();
    }
}
