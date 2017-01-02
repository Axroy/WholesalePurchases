/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.NameClashException;
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BuyerAgent extends Agent{
    private int money;
    private Map<String, Integer> productWishes;
    private List<String> groups;
    private AID seller = null;

    private MessageTemplate template;
    private String currentProductName;

    private class BuyWholesale extends SequentialBehaviour {
        public int onEnd() {
            doWait(1000);
            reset();
            myAgent.addBehaviour(this);
            return super.onEnd();
        }
    }
    // Gets best wholesale shops for each desired product and creates groups for them if they (groups) don't exist
    private class GetShopsAndCreateGroups extends SimpleBehaviour {
        private boolean finished = false;

        public void action() {
            groups = new LinkedList<String>();

            DFAgentDescription[] shopsSearchResult = searchShops();
            if (shopsSearchResult == null || shopsSearchResult.length == 0) {
                finished = true;
                System.out.println(myAgent.getName() + " found no shops!");
                return;
            }

            for (Map.Entry<String, Integer> product: productWishes.entrySet()) {
                if (product.getValue() > 0) {
                    int bestChoiceWholesalePrice = money;
                    int bestChoiceCondition = 0;
                    String bestChoiceAddress = "";

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

                    String groupAddress = getGroupAddress(product.getKey(), bestChoiceAddress);
                    if (groupAddress == null) {
                        createGroup(product.getKey(), bestChoiceAddress, bestChoiceCondition);
                    }
                    groups.add(groupAddress);
                }
            }
        }

        public boolean done() {
            return finished;
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
        private void createGroup(String productName, String shopAddress, int condition) {
            try {
                Object[] groupArgs = {productName, shopAddress, condition};
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

    protected void setup() {
        getArgs();

        SequentialBehaviour buyRetail = new SequentialBehaviour(this) {
            public int onEnd() {
                doWait(1000);
                reset();
                myAgent.addBehaviour(this);
                return super.onEnd();
            }
        };

        SimpleBehaviour findProduct = new SimpleBehaviour() {
            private boolean finished = false;

            @Override
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
                    finished = true;
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }

            @Override
            public boolean done() {
                return finished;
            }
        };
        SimpleBehaviour sendRequest = new SimpleBehaviour() {
            private boolean finished = false;

            @Override
            public void action() {
                if (seller == null) {
                    finished = true;
                }

                ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
                message.addReceiver(seller);
                message.setContent(currentProductName);
                message.setConversationId("retail");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);
                template = MessageTemplate.and(MessageTemplate.MatchConversationId("retail"), MessageTemplate.MatchInReplyTo(message.getReplyWith()));

                finished = true;
            }

            @Override
            public boolean done() {
                return finished;
            }
        };
        SimpleBehaviour receiveReply = new SimpleBehaviour() {
            boolean finished = false;

            @Override
            public void action() {
                if (seller == null) {
                    finished = true;
                }

                ACLMessage reply = myAgent.receive(template);
                if (reply != null) {
                    if (!reply.getContent().equals("not_available")) {
                        System.out.println(getAID().getName() + " bought " + productWishes.get(currentProductName) + " " + currentProductName
                            + " for " + (Integer.valueOf(reply.getContent()) * productWishes.get(currentProductName)) + "$!");
                        money -= Integer.valueOf(reply.getContent()) * productWishes.get(currentProductName);
                        productWishes.put(currentProductName, 0);
                    }
                    else {
                        System.out.println(getAID().getName() + " tried to buy " + currentProductName + " but it wasn't available!");
                    }
                    finished = true;
                }
                else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return finished;
            }
        };

        buyRetail.addSubBehaviour(findProduct);
        buyRetail.addSubBehaviour(sendRequest);
        buyRetail.addSubBehaviour(receiveReply);
        //addBehaviour(buyRetail);

        BuyWholesale buyWholesale = new BuyWholesale();
        buyWholesale.addSubBehaviour(new GetShopsAndCreateGroups());
        addBehaviour(buyWholesale);

        logAppearing();
    }

    private void getArgs() {
        Object[] arg = getArguments();
        money = (Integer) arg[0];
        productWishes = (Map<String, Integer>) arg[1];
    }
    private void logAppearing() {
        String log = "New buyer appears! Name's " + getAID().getName() + "!" + "\n";
        log += "    " + "Money: " + money + "$" + "\n";
        for (Map.Entry<String, Integer> wish: productWishes.entrySet()) {
            log += "    " + "Wants " + wish.getValue() + " " + wish.getKey() + "\n";
        }
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
