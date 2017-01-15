/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.core.behaviours.ReceiverBehaviour.Handle;

import java.util.*;

public class BuyerAgent extends Agent{
    private int money;
    private Map<String, Integer> productWishes;
    private Map<String, String> productsGroups;
    private AID seller = null;
    private boolean noShops;
    private MessageTemplate template;
    private String currentProductName;

    private class BuyWholesale extends SequentialBehaviour {
        public int onEnd() {
//            doWait(5000);
//            reset();
//            myAgent.addBehaviour(this);
            myAgent.addBehaviour(new WakerBehaviour(myAgent, 5000) {
                @Override
                protected void onWake() {
                    BuyWholesale buyWholesale = new BuyWholesale();
                    buyWholesale.addSubBehaviour(new GetShopsAndCreateGroups());
                    buyWholesale.addSubBehaviour(new Enlist());
                    myAgent.addBehaviour(buyWholesale);
                    super.onWake();
                }
            });
            return super.onEnd();
        }
    }
    // Gets best wholesale shops for each desired product and creates groups for them if they (groups) don't exist
    private class GetShopsAndCreateGroups extends OneShotBehaviour {

        public void action() {
            productsGroups = new HashMap<String, String>();
            noShops = false;
            DFAgentDescription[] shopsSearchResult = searchShops();
            if (shopsSearchResult == null || shopsSearchResult.length == 0) {
                noShops = true;
                System.out.println(myAgent.getName() + " found no shops!");
                return;
            }

            for (Map.Entry<String, Integer> product: productWishes.entrySet()) {
                if (product.getValue() > 0) {
                    int bestChoiceWholesalePrice = money;
                    int bestChoiceCondition = 0;
                    String bestChoiceAddress = null;

                    for (DFAgentDescription shop: shopsSearchResult) {
                        Iterator itr = shop.getAllServices();
                        while (itr.hasNext()) {
                            ServiceDescription productInfo = (ServiceDescription) itr.next();
                            if (YellowPagesParser.getShopProductName(productInfo.getName()).equals(product.getKey())) {
                                if (YellowPagesParser.getShopProductWholesalePrice(productInfo.getName()) < bestChoiceWholesalePrice) {
                                    bestChoiceWholesalePrice = YellowPagesParser.getShopProductWholesalePrice(productInfo.getName());
                                    bestChoiceCondition = YellowPagesParser.getShopProductCondition(productInfo.getName());
                                    bestChoiceAddress = shop.getName().getName();
                                }
                            }
                        }
                    }

                    if (bestChoiceAddress == null) {
                        continue;
                    }

                    String groupAddress = getGroupAddress(product.getKey(), bestChoiceAddress);
                    if (groupAddress == null) {
                        createGroup(product.getKey(), bestChoiceAddress, bestChoiceCondition, bestChoiceWholesalePrice);
                        while (groupAddress == null) {
                            groupAddress = getGroupAddress(product.getKey(), bestChoiceAddress);
                        }
                    }
                    productsGroups.put(product.getKey(), groupAddress);
                }
            }
        }

        private DFAgentDescription[] searchShops() {
            DFAgentDescription[] searchResult = null;

            DFAgentDescription agentTemplate = new DFAgentDescription();
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setType("Shop_product");
            agentTemplate.addServices(serviceDescription);
            try {
                searchResult = DFService.search(myAgent, agentTemplate);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            return searchResult;
        }
        private DFAgentDescription[] searchGroups() {
            DFAgentDescription[] searchResult = null;

            DFAgentDescription agentTemplate = new DFAgentDescription();
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setType("Group");
            agentTemplate.addServices(serviceDescription);
            try {
                searchResult = DFService.search(myAgent, agentTemplate);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

            return searchResult;
        }
        private void createGroup(String productName, String shopAddress, int condition, int wholesalePrice) {
            try {
                Object[] groupArgs = {productName, shopAddress, condition, wholesalePrice};
                ContainerController containerController = getContainerController();
                AgentController agentController = containerController.createNewAgent("Group_" + productName + "_" +
                    shopAddress.split("@")[0], "GroupAgent", groupArgs);
                agentController.start();
            }
            catch (StaleProxyException spe) {
                if (!spe.getMessage().split(" ")[0].equals("Name-clash")) {
                    spe.printStackTrace();
                }
            }
        }
        private String getGroupAddress (String productName, String shopAddress) {
            String result = null;

            DFAgentDescription[] groupsSearchResult = searchGroups();
            if (groupsSearchResult != null && groupsSearchResult.length != 0) {
                for (DFAgentDescription group : groupsSearchResult) {
                    Iterator itr = group.getAllServices();
                    while (itr.hasNext()) {
                        ServiceDescription groupInfo = (ServiceDescription) itr.next();
                        //System.out.println("===" + group.getName() + "===");
                        if (YellowPagesParser.getGroupShopName(groupInfo.getName()).equals(shopAddress)
                            && YellowPagesParser.getGroupProductName(groupInfo.getName()).equals(productName)) {
                            result = group.getName().getName();
                        }
                    }
                }
            }

            return result;
        }
    }
    private class Enlist extends OneShotBehaviour {
        public void action() {
            if (noShops) {
                return;
            }
            for (Map.Entry<String, String> group: productsGroups.entrySet()) {
                ACLMessage message = new ACLMessage(ACLMessage.SUBSCRIBE);
                AID groupAID = new AID(group.getValue());
                message.addReceiver(groupAID);
                message.setContent(productWishes.get(group.getKey()).toString());
                message.setConversationId("enlist");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);
            }
        }
    }
    private class AnswerReady extends CyclicBehaviour {
        public void action() {
            MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
                MessageTemplate.MatchConversationId("group_purchase"));
            ACLMessage message = myAgent.receive(template);
            if (message != null) {
                if (message.getContent().equals("ready_for_purchase?")) {
                    ACLMessage reply = message.createReply();
                    reply.setContent("ready");
                    myAgent.send(reply);
                }
            }
            else {
                block();
            }
        }
    }
    private class ReceiveBuyConfirmation extends CyclicBehaviour {
        public void action() {
            MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("group_purchase"));
            ACLMessage message = myAgent.receive(template);
            if (message != null) {
                String product = message.getContent().split(" ")[0];
                int price = Integer.valueOf(message.getContent().split(" ")[1]);
                money -= productWishes.get(product) * price;
                logWholesalePurchase(myAgent.getLocalName(), product, price);
                productWishes.put(product, 0);
            }
            else {
                block();
            }
        }
    }

    private class BuyRetail extends SequentialBehaviour {
        public int onEnd() {
            myAgent.addBehaviour(new WakerBehaviour(myAgent, 1000) {
                @Override
                protected void onWake() {
                    BuyRetail buyRetail = new BuyRetail();
                    buyRetail.addSubBehaviour(new FindProduct());
                    buyRetail.addSubBehaviour(new SendRequest());
                    Handle handle = ReceiverBehaviour.newHandle();
                    buyRetail.addSubBehaviour(new ReceiverBehaviour(myAgent, handle, -1, template));
                    buyRetail.addSubBehaviour(new ProcessReply(handle));
                    addBehaviour(buyRetail);
                    super.onWake();
                }
            });
            return super.onEnd();
        }
    }
    private class FindProduct extends OneShotBehaviour {
        public void action() {
            currentProductName = getNextWishedProductName();
            if (currentProductName == null) {
                System.out.println(getAID().getName() + " successfully finished shopping!");
                doDelete();
            }

            DFAgentDescription agentTemplate = new DFAgentDescription();
            ServiceDescription serviceDescription = new ServiceDescription();
            serviceDescription.setType("Shop_product");
            agentTemplate.addServices(serviceDescription);
            try {
                // Find a shop which sells desired product for best price(wholesale)
                DFAgentDescription[] result = DFService.search(myAgent, agentTemplate);
                int bestWholesalePrice = money;
                for (DFAgentDescription shop: result) {
                    Iterator itr = shop.getAllServices();
                    while (itr.hasNext()) {
                        ServiceDescription productInfo = (ServiceDescription) itr.next();
                        if (YellowPagesParser.getShopProductName(productInfo.getName()).equals(currentProductName)) {
                            if (YellowPagesParser.getShopProductWholesalePrice(productInfo.getName()) < bestWholesalePrice) {
                                bestWholesalePrice = YellowPagesParser.getShopProductWholesalePrice(productInfo.getName());
                                seller = new AID();
                                seller = shop.getName();
                            }
                        }
                    }
                }
                if (seller == null) {
                    System.out.println(getAID().getName() + " tried to find a shop which sells " + currentProductName + " but was unsuccessful!");
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }
    private class SendRequest extends OneShotBehaviour {

        public void action() {
            if (seller == null) {
                return;
            }

            ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
            message.addReceiver(seller);
            message.setContent(currentProductName);
            message.setConversationId("retail");
            message.setReplyWith(String.valueOf(System.currentTimeMillis()));
            myAgent.send(message);
            template = MessageTemplate.and(MessageTemplate.MatchConversationId("retail"), MessageTemplate.MatchInReplyTo(message.getReplyWith()));
        }
    }
    private class ProcessReply extends OneShotBehaviour {
        private Handle handle;
        private AID sender;

        public ProcessReply(Handle handle) {
            this.handle = handle;
        }

        public void action() {
            if (seller == null) {
                return;
            }
            try {
                ACLMessage reply = handle.getMessage();
                sender = reply.getSender();
                if (!reply.getContent().equals("not_available")) {
                    System.out.println(getAID().getName() + " bought " + productWishes.get(currentProductName) + " " + currentProductName
                        + " for " + (Integer.valueOf(reply.getContent()) * productWishes.get(currentProductName)) + "$!");
                    money -= Integer.valueOf(reply.getContent()) * productWishes.get(currentProductName);
                    productWishes.put(currentProductName, 0);
                } else {
                    System.out.println(getAID().getName() + " tried to buy " + currentProductName + " but it wasn't available!");
                }
            }
            catch (ReceiverBehaviour.NotYetReady nyr) {
                System.out.println(myAgent.getLocalName() + " tried to get reply message from " + sender.getLocalName()
                    + " but it was not yet ready");
            }
            catch (ReceiverBehaviour.TimedOut to) {
                System.out.println(myAgent.getLocalName() + " tried to get reply message from " + sender.getLocalName()
                    + " but it timed out");
            }
        }
    }

    protected void setup() {
        getArgs();

        final BuyRetail buyRetail = new BuyRetail();
        buyRetail.addSubBehaviour(new FindProduct());
        buyRetail.addSubBehaviour(new SendRequest());
        Handle handle = ReceiverBehaviour.newHandle();
        buyRetail.addSubBehaviour(new ReceiverBehaviour(this, handle,-1, template));
        buyRetail.addSubBehaviour(new ProcessReply(handle));

        final BuyWholesale buyWholesale = new BuyWholesale();
        buyWholesale.addSubBehaviour(new GetShopsAndCreateGroups());
        buyWholesale.addSubBehaviour(new Enlist());
        addBehaviour(buyWholesale);

        final AnswerReady answerReady = new AnswerReady();
        addBehaviour(answerReady);

        final ReceiveBuyConfirmation receiveBuyConfirmation = new ReceiveBuyConfirmation();
        addBehaviour(receiveBuyConfirmation);

        TickerBehaviour dieWhenFinishedShopping = new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (getNextWishedProductName() == null) {
                    logFinishedShopping();
                    doDelete();
                }
            }
        };
        addBehaviour(dieWhenFinishedShopping);

        final int retailTimeout = 15000;
        // Switch to retail after timeout
        addBehaviour(new WakerBehaviour(this, retailTimeout) {
            @Override
            protected void onWake() {
                if (getNextWishedProductName() == null) {
                    doDelete();
                    return;
                }

                logGoingRetail(myAgent.getLocalName(), retailTimeout);

                removeBehaviour(buyWholesale);
                removeBehaviour(answerReady);
                removeBehaviour(receiveBuyConfirmation);

                addBehaviour(buyRetail);

                super.onWake();
            }
        });

        logAppearing();
    }

    private void getArgs() {
        Object[] arg = getArguments();
        money = (Integer) arg[0];
        productWishes = (Map<String, Integer>) arg[1];
    }
    private void logAppearing() {
        String log = "New buyer appears! Name's " + getAID().getLocalName() + "!" + "\n";
        log += "    " + "Money: " + money + "$" + "\n";
        for (Map.Entry<String, Integer> wish: productWishes.entrySet()) {
            log += "    " + "Wants " + wish.getValue() + " " + wish.getKey() + "\n";
        }
        System.out.print(log);
    }
    private void logWholesalePurchase(String buyerName, String productName, int price) {
        System.out.println(buyerName + " bought " + productWishes.get(productName) + " " + productName + ", each for "
            + price + "$" + " (wholesale)");
    }
    private void logFinishedShopping() {
        System.out.println(getAID().getLocalName() + " successfully finished shopping!");
    }
    private void logGoingRetail(String buyerName, long timeMs) {
        String log = buyerName + " lives for " + timeMs + " ms already!" + "\n";
        for (Map.Entry<String, Integer> wish: productWishes.entrySet()) {
            if (wish.getValue() > 0) {
                log += "    " + "Still wants " + wish.getValue() + " " + wish.getKey() + "\n";
            }
        }
        log += "Going retail!" + "\n";
        System.out.print(log);
    }
    private String  getNextWishedProductName() {
        for (Map.Entry<String, Integer> product: productWishes.entrySet()) {
            if (product.getValue() > 0)
            {
                return  product.getKey();
            }
        }
        return null;
    }
}
