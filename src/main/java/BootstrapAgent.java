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
//        bootstrapChangeGroupDemo();
        doDelete();
    }

    private void bootstrapShops() {
        ContainerController containerController = getContainerController();
        AgentController agentController;
        try {
            // Name must not have spaces for parser to work!
            Product cheeseHead = new Product("Cheese_head", 30, 15, 15);
            Product bread = new Product("Bread", 20, 10, 10);

            List<Product> cheeseShopStock = new LinkedList<Product>();
            cheeseShopStock.add(cheeseHead);
            Object[] cheeseShopArgs = {cheeseShopStock};
            // Name must not have spaces for parser to work!
            agentController = containerController.createNewAgent("Cheese_shop", "ShopAgent", cheeseShopArgs);
            agentController.start();

            List<Product> breadShopStock = new LinkedList<Product>();
            breadShopStock.add(bread);
            Object[] breadShopArgs = {breadShopStock};
            agentController = containerController.createNewAgent("Bread_shop", "ShopAgent", breadShopArgs);
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

            for (int i = 0; i < 5; i++) {
                int money = 1000 + i * 100;
                Map<String, Integer> wish = new HashMap<String, Integer>();
                wish.put("Bread", 1 + i);
                Object[] buyerArgs = {money, wish};
                agentController = containerController.createNewAgent("Bread buyer " + i, "BuyerAgent", buyerArgs);
                agentController.start();
                doWait(1000);
            }

            for (int i = 0; i < 5; i++) {
                int money = 1000 + i * 100;
                Map<String, Integer> wish = new HashMap<String, Integer>();
                wish.put("Bread", 1 + i);
                wish.put("Cheese_head", 1 + i);
                Object[] buyerArgs = {money, wish};
                agentController = containerController.createNewAgent("Bread and cheese buyer " + i, "BuyerAgent", buyerArgs);
                agentController.start();
                doWait(1000);
            }
        }
        catch (StaleProxyException spe) {
            spe.printStackTrace();
        }
    }
    private void bootstrapChangeGroupDemo() {
        ContainerController containerController = getContainerController();
        AgentController agentController;

        try {
            Product expensivePotato = new Product("Potato", 10, 7, 10);

            List<Product> expensivePotatoShopStock = new LinkedList<Product>();
            expensivePotatoShopStock.add(expensivePotato);
            Object[] expensivePotatoShopArgs = {expensivePotatoShopStock};
            agentController = containerController.createNewAgent("Expensive_potato_shop", "ShopAgent", expensivePotatoShopArgs);
            agentController.start();


            for (int i = 0; i < 3; i++) {
                int money = 1000 + i * 10;
                Map<String, Integer> wish = new HashMap<String, Integer>();
                wish.put("Potato", 2);
                Object[] buyerArgs = {money, wish};
                agentController = containerController.createNewAgent("Potato_buyer_" + i, "BuyerAgent", buyerArgs);
                agentController.start();
                doWait(1000);
            }

            Product cheapPotato = new Product("Potato", 9, 5, 10);

            List<Product> cheapPotatoShopStock = new LinkedList<Product>();
            cheapPotatoShopStock.add(cheapPotato);
            Object[] cheapPotatoShopArgs = {cheapPotatoShopStock};
            agentController = containerController.createNewAgent("Cheap_potato_shop", "ShopAgent", cheapPotatoShopArgs);
            agentController.start();

            for (int i = 3; i < 5; i++) {
                int money = 1000 + i * 100;
                Map<String, Integer> wish = new HashMap<String, Integer>();
                wish.put("Potato", 2);
                Object[] buyerArgs = {money, wish};
                agentController = containerController.createNewAgent("Potato_buyer_" + i, "BuyerAgent", buyerArgs);
                agentController.start();
                doWait(1000);
            }

            doWait(10000);

            int money = 1000;
            Map<String, Integer> wish = new HashMap<String, Integer>();
            wish.put("Potato", 2);
            Object[] buyerArgs = {money, wish};
            agentController = containerController.createNewAgent("Potato_buyer_X", "BuyerAgent", buyerArgs);
            agentController.start();
        }
        catch (StaleProxyException spe) {
            spe.printStackTrace();
        }
    }
}
