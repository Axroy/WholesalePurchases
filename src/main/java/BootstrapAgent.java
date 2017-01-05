/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.Agent;
import jade.wrapper.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BootstrapAgent extends Agent{
    protected void setup() {
        bootstrapShops();
        bootstrapAgents();
        doDelete();
    }

    private void bootstrapShops() {
        ContainerController containerController = getContainerController();
        AgentController agentController;
        try {
            // Name must not have spaces for parser to work!
            Product cheeseHead = new Product("Cheese_head", 30, 15, 15);

            List<Product> cheeseShopStock = new LinkedList<Product>();
            cheeseShopStock.add(cheeseHead);
            Object[] cheeseShopArgs = {cheeseShopStock};
            // Name must not have spaces for parser to work!
            agentController = containerController.createNewAgent("Cheese_shop", "ShopAgent", cheeseShopArgs);
            agentController.start();
        }
        catch (StaleProxyException spe) {
            spe.printStackTrace();
        }
    }
    private void bootstrapAgents() {
        ContainerController containerController = getContainerController();
        AgentController agentController;
        try {
            for (int i = 0; i < 5; i++) {
                int money = 1000 + i * 100;
                Map<String, Integer> wish = new HashMap<String, Integer>();
                wish.put("Cheese_head", 1 + i);
                Object[] buyerArgs = {money, wish};
                agentController = containerController.createNewAgent("Cheese buyer " + i, "BuyerAgent", buyerArgs);
                agentController.start();
                doWait(1000);
            }
        }
        catch (StaleProxyException spe) {
            spe.printStackTrace();
        }
    }
}
