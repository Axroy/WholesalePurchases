import jade.core.Agent;

import java.util.Map;

/**
 * Created by Nikita on 30.12.2016.
 */
public class GroupAgent extends Agent{
    private String productName;
    private String shopAddress;
    private int condition;
    private Map<String, Integer> buyers;

    protected void setup() {
        Object[] args = getArguments();
        productName = (String) args[0];
        shopAddress = (String) args[1];
        condition = (Integer) args[2];


    }
}
