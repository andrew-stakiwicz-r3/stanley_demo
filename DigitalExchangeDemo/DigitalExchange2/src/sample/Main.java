package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;


public class Main extends Application {

    @Override
    //private String ip="";
    public void start(Stage primaryStage) throws Exception{

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        System.out.println(list.size());
        for(String each : list){
            System.out.println(each);
        }
        System.out.println("list=" + list.toString());


        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(list.get(0));
        if (list.size() < 2){
            throw new Exception("the first arg is rpc address e.g. localhost:10006 and the second is role e.g. E, I P");
        }
        String role = list.get(1);

        if (Arrays.asList("E", "I", "P").contains(role) == false){
            throw new Exception("the second arg be either  E (exchange), I (issuer) & P (exchange participant)");
        }



        String username = "user1";
        String password = "test";
        System.out.println("NodeAddress=" + nodeAddress.toString());


            System.out.println("RPC Connecting....");
            final CordaRPCClient client = new CordaRPCClient(nodeAddress);

           System.out.println("2");
            final CordaRPCConnection connection = client.start(username, password);
            System.out.println("3");

            final CordaRPCOps proxy = connection.getProxy();

            System.out.println(proxy.networkMapSnapshot().toString());







        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = (Parent)fxmlLoader.load();
        MainController controller = fxmlLoader.<MainController>getController();

        //turn on
        controller.init(primaryStage, proxy, role);

        Scene scene = new Scene(root, 600, 450);

        primaryStage.setScene(scene);
        //turn on
        primaryStage.setTitle(proxy.nodeInfo().getLegalIdentities().get(0).getName().toString());
        primaryStage.show();


    }


    public static void main(String[] args)
    {
        launch(args);
        System.out.println(args.toString());

    }
}
