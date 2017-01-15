import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

/**
 * Created by Nikita on 30.12.2016.
 */
public class GroupAgent extends Agent{
    private String productName;
    private String shopAddress;
    private int condition;
    private int wholesalePrice;
    // Address and desired quantity of products
    private Map<String, Integer> buyersDesires;
    private boolean sentQueries;
    private boolean conditionFulfilled;
    private List<ReceiverBehaviour.Handle> handles;

    // Gets buyer and their desired quantity of the product, enlists them and replies
    private class EnlistBuyers extends CyclicBehaviour {
        public void action() {
            MessageTemplate enlistRequest = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
            ACLMessage message = myAgent.receive(enlistRequest);

            if (message != null) {
                AID buyer = message.getSender();
                int quantity = Integer.valueOf(message.getContent());
                buyersDesires.put(buyer.getName(), quantity);
            }
            else {
                block();
            }
        }
    }

    // If condition is fulfilled checks readiness of all current buyers and then sends them purchase confirmation
    private class Purchase extends SequentialBehaviour {
        public int onEnd() {
            myAgent.addBehaviour(new WakerBehaviour(myAgent, 1000) {
                @Override
                protected void onWake() {
                    ParallelBehaviour getReplies = new ParallelBehaviour(myAgent, ParallelBehaviour.WHEN_ALL);

                    SequentialBehaviour purchase = new Purchase();
                    purchase.addSubBehaviour(new CheckEnlistedIfConditionReached(getReplies));
                    purchase.addSubBehaviour(getReplies);
                    purchase.addSubBehaviour(new PurchaseIfEveryoneReady());
                    addBehaviour(purchase);
                    super.onWake();
                }
            });
            return super.onEnd();
        }
    }
    private class CheckEnlistedIfConditionReached extends SimpleBehaviour {
        private boolean finished = false;
        private ParallelBehaviour getReplies;

        public CheckEnlistedIfConditionReached(ParallelBehaviour getReplies) {
            this.getReplies = getReplies;
        }

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

            if (sentQueries) {
                finished = true;
                return;
            }

            logConditionFulfilled(myAgent.getLocalName());

            handles = new LinkedList<ReceiverBehaviour.Handle>();

            for (String buyerAddress: buyersDesires.keySet()) {
                ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
                AID buyer = new AID(buyerAddress);
                message.addReceiver(buyer);
                message.setContent("ready_for_purchase?");
                message.setConversationId("group_purchase");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);

                ReceiverBehaviour.Handle handle = ReceiverBehaviour.newHandle();
                getReplies.addSubBehaviour(new ReceiverBehaviour(myAgent, handle, 5000, MessageTemplate.and(
                    MessageTemplate.MatchSender(buyer), MessageTemplate.MatchConversationId("group_purchase"))));
                handles.add(handle);
            }

            sentQueries = true;
            finished = true;
        }

        public boolean done(){
            return finished;
        }
    }
    private class PurchaseIfEveryoneReady extends SimpleBehaviour {
        private boolean finished = false;

        public void action() {
            if (!conditionFulfilled) {
                finished = true;
                return;
            }

            try {
                for (ReceiverBehaviour.Handle handle: handles) {
                    if (!handle.getMessage().getContent().equals("ready")) {
                        buyersDesires.clear();
                        sentQueries = false;
                        System.out.println(myAgent.getLocalName() + " found out that somebody is not ready anymore! Resetting.");
                        finished = true;
                        return;
                    }
                    else {
                        logReceivedReadyMessage(handle.getMessage().getSender().getLocalName(), myAgent.getLocalName());
                    }
                }
            }
            catch (ReceiverBehaviour.TimedOut to) {
                buyersDesires.clear();
                handles.clear();
                sentQueries = false;
                System.out.println(myAgent.getLocalName() + " failed to get all readiness responses in time! Resetting.");
                finished = true;
                return;
            }
            catch (ReceiverBehaviour.NotYetReady nyr) {
                finished = true;
                return;
            }

            for (String buyerAddress: buyersDesires.keySet()) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                AID buyer = new AID(buyerAddress);
                message.addReceiver(buyer);
                message.setContent(productName + " " + wholesalePrice);
                message.setConversationId("group_purchase");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);
            }

            buyersDesires.clear();
            handles.clear();
            sentQueries = false;
            finished = true;
        }

        public boolean done() {
            return finished;
        }
    }

    protected void setup() {
        getArgs();
        register();

        buyersDesires = new HashMap<String, Integer>();
        sentQueries = false;

        addBehaviour(new EnlistBuyers());

        ParallelBehaviour getReplies = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);

        SequentialBehaviour purchase = new Purchase();
        purchase.addSubBehaviour(new CheckEnlistedIfConditionReached(getReplies));
        purchase.addSubBehaviour(getReplies);
        purchase.addSubBehaviour(new PurchaseIfEveryoneReady());
        addBehaviour(purchase);

        logAppearing();
    }

    private void getArgs() {
        Object[] args = getArguments();
        productName = (String) args[0];
        shopAddress = (String) args[1];
        condition = (Integer) args[2];
        wholesalePrice = (Integer) args[3];
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
        String log = "Group " + getAID().getLocalName() + " was created!" + "\n";
        log += "    Used for wholesale purchases of " + productName + " from " + shopAddress.split("@")[0] + "\n";
        System.out.print(log);
    }
    private void logConditionFulfilled(String groupName) {
        String log = groupName + " ready for wholesale purchase!" + " Buyers:" + "\n";
        for (String buyer: buyersDesires.keySet()) {
            log += "    " + buyer.split("@")[0] + "\n";
        }
        System.out.print(log);
    }
    private void logReceivedReadyMessage(String senderName, String groupName) {
        System.out.println(senderName + " is ready to purchase " + productName + " using group "
            + groupName);
    }
}
