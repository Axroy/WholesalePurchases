/**
 * Created by Nikita on 14.12.2016.
 */

import jade.Boot;

public class Main {
    public static void main(String[] args) {
        String[] s = {"-gui", "start:BootstrapAgent"};
        Boot.main(s);
        System.out.println("Agent system launched!");
    }
}
