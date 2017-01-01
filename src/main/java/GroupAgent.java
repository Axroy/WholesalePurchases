import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nikita on 30.12.2016.
 */
public class GroupAgent extends Agent{
    private String productName;
    private String shopAddress;
    private int condition;
    private Map<String, Integer> buyers;

    // Gets buyer and their desired quantity of the product, enlists them and replies
    private class EnlistBuyers extends CyclicBehaviour {
        public void action() {
            MessageTemplate enlistRequest = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
            ACLMessage message = myAgent.receive();

            if (message != null) {
                String buyer = message.getContent().split(" ")[0];
                int quantity = Integer.valueOf(message.getContent().split(" ")[1]);
                buyers.put(buyer, quantity);

                ACLMessage reply = message.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("enlisted");
            }
            else {
                block();
            }
        }
    }

    protected void setup() {
        getArgs();
        register();

        buyers = new HashMap<String, Integer>();

        addBehaviour(new EnlistBuyers());

        logAppearing();
    }

    private void getArgs() {
        Object[] args = getArguments();
        productName = (String) args[0];
        shopAddress = (String) args[1];
        condition = (Integer) args[2];
    }
    private void register() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("Group");
        sd.setName(productName + " " + shopAddress);
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    private void logAppearing() {
        String log = "Group " + getAID().getName() + " was created!" + "\n";
        log += "Used for wholesale purchases of " + productName + " from " + shopAddress + "\n";
        System.out.print(log);
    }
}
