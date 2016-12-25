/**
 * Created by Nikita on 15.12.2016.
 */
public class Product {
    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public int getPrice() {
        return price;
    }

    public int getWholesalePrice() {
        return wholesalePrice;
    }

    public int getCondition() {
        return condition;
    }

    public Product(String name, int number, int price, int wholesalePrice, int condition) {
        this.name = name;
        this.number = number;
        this.price = price;
        this.wholesalePrice = wholesalePrice;
        this.condition = condition;
    }

    private String name;
    private int number;
    private int price;
    private int wholesalePrice;
    private int condition;
}
