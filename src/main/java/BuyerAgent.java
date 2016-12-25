/**
 * Created by Nikita on 15.12.2016.
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Map;

public class BuyerAgent extends Agent{
    private int money;
    private Map<String, Integer> productWishes;
    private AID seller;

    private MessageTemplate template;
    private int step = 0;

    protected void setup() {
        Object[] arg = getArguments();
        money = (Integer) arg[0];
        productWishes = (Map<String, Integer>) arg[1];

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                String currentProductName = getNextWishedProductName();
                if (currentProductName == null) {
                    System.out.println(getAID().getName() + " successfully finished shopping!");
                    doDelete();
                }
                switch (step) {
                    case 0:
                        ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);

                        DFAgentDescription agentTemplate = new DFAgentDescription();
                        ServiceDescription serviceDescription = new ServiceDescription();
                        serviceDescription.setType(currentProductName);
                        agentTemplate.addServices(serviceDescription);
                        try {
                            DFAgentDescription[] result = DFService.search(myAgent, agentTemplate);
                            if (result.length == 0) {
                                System.out.println(getAID().getName() + " tried to find a shop which sells " + currentProductName + " but was unsuccessful!");
                                return;
                            }
                            seller = new AID();
                            seller = result[0].getName();
                        }
                        catch (FIPAException fe) {
                            fe.printStackTrace();
                        }

                        message.addReceiver(seller);
                        message.setContent(currentProductName);
                        message.setConversationId("trade");
                        message.setReplyWith(String.valueOf(System.currentTimeMillis()));
                        myAgent.send(message);
                        template = MessageTemplate.and(MessageTemplate.MatchConversationId("trade"), MessageTemplate.MatchInReplyTo(message.getReplyWith()));
                        step = 1;
                        break;
                    case 1:
                        ACLMessage reply = myAgent.receive(template);
                        if (reply != null) {
                            if (reply.getContent() != "not_available") {
                                System.out.println(getAID().getName() + " bought 1 " + currentProductName + "!");
                                // decrement wished quantity
                                productWishes.put(currentProductName, productWishes.get(currentProductName) - 1);
                            }
                            else {
                                System.out.println(getAID().getName() + " tried to buy 1 " + currentProductName + " but it wasn't available!");
                            }
                            step = 0;
                        }
                        else {
                            block();
                        }
                        break;
                }
            }
        });

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
