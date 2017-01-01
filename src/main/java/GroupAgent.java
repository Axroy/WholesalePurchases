import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
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
    // Address and desired quantity of products
    private Map<String, Integer> buyersDesires;
    private Map<String, Boolean> buyersReadiness;

    private boolean conditionFulfilled;
    private MessageTemplate template;

    // Gets buyer and their desired quantity of the product, enlists them and replies
    private class EnlistBuyers extends CyclicBehaviour {
        public void action() {
            MessageTemplate enlistRequest = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
            ACLMessage message = myAgent.receive();

            if (message != null) {
                AID buyer = message.getSender();
                int quantity = Integer.valueOf(message.getContent());
                buyersDesires.put(buyer.getName(), quantity);

                ACLMessage reply = message.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("enlisted");
            }
            else {
                block();
            }
        }
    }

    // If condition is fulfilled checks readiness of all current buyers and then sends them purchase confirmation
    private class Purchase extends SequentialBehaviour {
        public int onEnd() {
            reset();
            myAgent.addBehaviour(this);
            return super.onEnd();
        }
    }
    private class CheckEnlistedIfConditionReached extends SimpleBehaviour {
        private boolean finished = false;

        public void action() {
            int sum = 0;
            for (int quantity: buyersDesires.values()) {
                sum += quantity;
            }
            if (sum < condition) {
                finished = true;
                conditionFulfilled = false;
                return;
            }
            conditionFulfilled = true;

            for (String buyerAddress: buyersDesires.keySet()) {
                ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
                AID buyer = new AID(buyerAddress);
                message.addReceiver(buyer);
                message.setContent("ready_for_purchase?");
                message.setConversationId("group_purchase");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);
                template = MessageTemplate.and(MessageTemplate.MatchConversationId("group_purchase"), MessageTemplate.MatchInReplyTo(message.getReplyWith()));
            }
            buyersReadiness = new HashMap<String, Boolean>();
            finished = true;
        }

        public boolean done(){
            return finished;
        }
    }
    private class GetRepliesAndPurchaseIfEveryoneReady extends SimpleBehaviour {
        private boolean finished = false;
        public void action() {
            if (!conditionFulfilled) {
                finished = true;
                return;
            }

            ACLMessage reply = myAgent.receive(template);
            if (reply != null) {
                if (reply.getContent().equals("ready")) {
                    buyersReadiness.put(reply.getSender().getName(), true);

                    for (boolean readiness: buyersReadiness.values()) {
                        if (!readiness) {
                            finished = true;
                            return;
                        }
                    }

                    for (String buyerAddress: buyersReadiness.keySet()) {
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        AID buyer = new AID(buyerAddress);
                        message.addReceiver(buyer);
                        message.setContent(productName);
                        message.setConversationId("group_purchase");
                        message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                        myAgent.send(message);
                    }

                    buyersDesires.clear();
                    finished = true;
                }
            }
        }

        public boolean done() {
            return finished;
        }
    }

    protected void setup() {
        getArgs();
        register();

        buyersDesires = new HashMap<String, Integer>();

        addBehaviour(new EnlistBuyers());

        SequentialBehaviour purchase = new Purchase();
        purchase.addSubBehaviour(new CheckEnlistedIfConditionReached());
        purchase.addSubBehaviour(new GetRepliesAndPurchaseIfEveryoneReady());
        addBehaviour(purchase);

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
