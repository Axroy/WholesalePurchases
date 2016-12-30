/**
 * Created by Nikita on 15.12.2016.
 */
public class Product {
    public String getName() {
        return name;
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

    public Product(String name, int price, int wholesalePrice, int condition) {
        this.name = name;
        this.price = price;
        this.wholesalePrice = wholesalePrice;
        this.condition = condition;
    }

    private String name;
    private int price;
    private int wholesalePrice;
    // minimal number of products needed for wholesale price
    private int condition;
}
