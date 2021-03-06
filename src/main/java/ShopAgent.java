/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class ShopAgent extends Agent{
    private List<Product> stock;

    // Retail queries have conversation id "retail"
    private class RetailStockInform extends CyclicBehaviour {
        public void action() {
            MessageTemplate informQuery = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            MessageTemplate retail = MessageTemplate.MatchConversationId("retail");
            MessageTemplate retailInform = MessageTemplate.and(informQuery, retail);
            ACLMessage message = myAgent.receive(retailInform);

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

    protected void setup() {
        getArgs();
        register();

        addBehaviour(new RetailStockInform());

        logOpening();
    }

    private void getArgs() {
        Object[] arg = getArguments();
        stock = (List<Product>) arg[0];
    }
    private void register() {
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
    }
    private void logOpening() {
        String log = "Shop " + getAID().getLocalName() + " opened!" + "\n";
        log += "Selling:" + "\n";
        for (Product product: stock) {
            log += "    " + product.getName() + ": " + product.getPrice() + "$" + " (wholesale: " +
                product.getWholesalePrice() + " condition: " + product.getCondition() + ")" + "\n";
        }
        System.out.print(log);
    }

    private Product findProductByName(String name) {
        for (Product product: stock) {
            if (product.getName().equals(name)) {
                return product;
            }
        }
        return null;
    }
}
