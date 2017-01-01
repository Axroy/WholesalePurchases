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

import java.util.Iterator;
import java.util.Map;

public class BuyerAgent extends Agent{
    private int money;
    private Map<String, Integer> productWishes;
    private AID seller = null;

    private MessageTemplate template;
    private String currentProductName;

    protected void setup() {
        Object[] arg = getArguments();
        money = (Integer) arg[0];
        productWishes = (Map<String, Integer>) arg[1];

        SequentialBehaviour buyContinuously = new SequentialBehaviour(this) {
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
                message.setConversationId("trade");
                message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                myAgent.send(message);
                template = MessageTemplate.and(MessageTemplate.MatchConversationId("trade"), MessageTemplate.MatchInReplyTo(message.getReplyWith()));

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
                        System.out.println(getAID().getName() + " bought 1 " + currentProductName + "!");
                        // decrement wished quantity
                        productWishes.put(currentProductName, productWishes.get(currentProductName) - 1);
                    }
                    else {
                        System.out.println(getAID().getName() + " tried to buy 1 " + currentProductName + " but it wasn't available!");
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

        buyContinuously.addSubBehaviour(findProduct);
        buyContinuously.addSubBehaviour(sendRequest);
        buyContinuously.addSubBehaviour(receiveReply);
        addBehaviour(buyContinuously);

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
