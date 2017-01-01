/**
 * Created by Nikita on 31.12.2016.
 */
public class YellowPagesParser {
    // Shop product service info in yellow pages:
    //  service type: "Shop_product"
    //  service name: "<product name> <price> <wholesale price> <condition>"

    public static String getShopProductName(String shopProductInfo){
        return shopProductInfo.split(" ")[0];
    }

    public static int getShopProductPrice(String shopProductInfo){
        return Integer.parseInt(shopProductInfo.split(" ")[1]);
    }

    public static int getShopProductWholesalePrice(String shopProductInfo){
        return Integer.parseInt(shopProductInfo.split(" ")[2]);
    }

    public static int getShopProductCondition(String shopProductInfo){
        return Integer.parseInt(shopProductInfo.split(" ")[3]);
    }

    // Group service info in yellow pages:
    //  service type: "Group"
    //  service name: "<product_name> <shop_name>"

    public static String getGroupProductName(String groupInfo){
        return groupInfo.split(" ")[0];
    }

    public static String getGroupShopName(String groupInfo){
        return groupInfo.split(" ")[1];
    }
}
