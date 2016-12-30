/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.Agent;
import jade.core.AgentContainer;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.*;

import java.util.List;
import java.util.Map;

public class ShopAgent extends Agent{
    private List<Product> stock;

    private class StockInform extends CyclicBehaviour {
        public void action() {
            MessageTemplate informQuery = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage message = myAgent.receive(informQuery);
            if (message != null) {
                String productName = message.getContent();
                ACLMessage reply = message.createReply();
                Product product = findProductByName(productName);
                if (product != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(product.getPrice()));
                }
                else {
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("not_available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

//    private class Sell extends CyclicBehaviour {
//        public void action() {
//            MessageTemplate sellRequest = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
//            ACLMessage message = myAgent.receive(sellRequest);
//            if (message != null) {
//                String product = message.getContent();
//                ACLMessage reply = message.createReply();
//                Integer price = (Integer) stock.get(product);
//                if (price != null) {
//                    reply.setPerformative(ACLMessage.INFORM);
//                    reply.setContent(String.valueOf(price.intValue()));
//                }
//                else {
//                    reply.setPerformative(ACLMessage.INFORM);
//                    reply.setContent("not_available");
//                }
//                myAgent.send(reply);
//            }
//            else {
//                block();
//            }
//        }
//    }

    protected void setup() {
        Object[] arg = getArguments();
        stock = (List<Product>) arg[0];

        addBehaviour(new StockInform());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for (Product product: stock) {
            ServiceDescription sd = new ServiceDescription();
            sd.setType("Shop_product");
            sd.setName(product.getName() + " " + String.valueOf(product.getPrice()) + " " +
                String.valueOf(product.getWholesalePrice()) + " " + String.valueOf(product.getCondition()));
            dfd.addServices(sd);
        }
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        String log = "Shop " + getAID().getName() + " opened!" + "\n";
        log += "Selling:" + "\n";
        for (Product product: stock) {
            log += "    " + product.getName() + ": " + product.getPrice() + "$" + "\n";
        }
        System.out.print(log);
    }

    private Product findProductByName(String name) {
        for (Product product: stock) {
            if (product.getName() == name) {
                return product;
            }
        }
        return null;
    }
}
