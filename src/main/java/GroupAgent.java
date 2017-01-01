import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.Map;

/**
 * Created by Nikita on 30.12.2016.
 */
public class GroupAgent extends Agent{
    private String productName;
    private String shopAddress;
    private int condition;
    private Map<String, Integer> buyers;

    protected void setup() {
        getArgs();
        register();
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
}
