package ascendix.data;

public class Plate {
    private String provider;
    private String name;
    private double price;

    public Plate(String provider, String name, double price) {
        this.provider = provider;
        this.name = name;
        this.price = price;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
