/**
 * Created by Nikita on 31.12.2016.
 */
public class YellowPagesParser {
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

    public static String getGroupProductName(String groupInfo){
        return groupInfo.split(" ")[0];
    }

    public static String getGroupShopName(String groupInfo){
        return groupInfo.split(" ")[1];
    }
}
