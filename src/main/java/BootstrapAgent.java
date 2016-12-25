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
            Product potato = new Product("potato", 1000, 5, 3, 10);
            Product youngPotato = new Product("young_potato", 200, 10, 6, 10);
            Product tomato = new Product("tomato", 3000, 20, 12, 30);
            Product maize = new Product("maize", 9000, 8, 5, 20);
            Product cucumber = new Product("cucumber", 800, 15, 10, 15);

            List<Product> potatoCountryStock = new LinkedList<Product>();
            potatoCountryStock.add(potato);
            potatoCountryStock.add(youngPotato);
            Object[] potatoCountryArgs = {potatoCountryStock};
            agentController = containerController.createNewAgent("The Potato Country", "ShopAgent", potatoCountryArgs);
            agentController.start();

            List<Product> royalStock = new LinkedList<Product>();
            royalStock.add(tomato);
            royalStock.add(cucumber);
            Object[] royalArgs = {royalStock};
            agentController = containerController.createNewAgent("Royal", "ShopAgent", royalArgs);
            agentController.start();

            List<Product> effgStock = new LinkedList<Product>();
            effgStock.add(potato);
            effgStock.add(maize);
            Object[] effgArgs = {effgStock};
            agentController = containerController.createNewAgent("Ed's Friendly Farming Goods", "ShopAgent", effgArgs);
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
            int mikeMoney = 100500;
            Map<String, Integer> mikeWish = new HashMap<String, Integer>();
            mikeWish.put("potato", 5);
            mikeWish.put("tomato", 2);
            mikeWish.put("maize", 1);
            Object[] mikeArgs = {mikeMoney, mikeWish};
            agentController = containerController.createNewAgent("Mike", "BuyerAgent", mikeArgs);
            agentController.start();

            int edwardMoney = 9000;
            Map<String, Integer> edwardWish = new HashMap<String, Integer>();
            edwardWish.put("coconut", 1);
            Object[] edwardArgs = {edwardMoney, edwardWish};
            agentController = containerController.createNewAgent("Edward", "BuyerAgent", edwardArgs);
            agentController.start();
        }
        catch (StaleProxyException spe) {
            spe.printStackTrace();
        }
    }
}
