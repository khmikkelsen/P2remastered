package Network.client;

import RSA.InvalidRSAKeyException;
import RSA.RSAKey;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class UserClient extends Application
{
    @Override
    public void start(Stage primaryStage)
    {
        UserController controller = new UserController();
        VBox vbox = new VBox();
        vbox.setPrefSize(400, 400);
        Scene scene = new Scene(vbox);

        TextArea recipient = new TextArea();
        recipient.setPrefRowCount(10);
        recipient.setPrefColumnCount(40);

        TextArea messageFieldToSend = new TextArea();
        messageFieldToSend.setPrefColumnCount(40);
        messageFieldToSend.setPrefRowCount(20);

        TextField emailsRecieved = new TextField();
        emailsRecieved.setPrefColumnCount(40);

        Alert nullAlert = new Alert(Alert.AlertType.ERROR,
                "Could not send.\nMake sure messagefield and recipient field is filled.");
        Alert keyAlert = new Alert(Alert.AlertType.ERROR,
                "Could not send.\nInvalid public key.");


        Button sendMessageButton = new Button("Send message");
        sendMessageButton.setOnMouseClicked(e -> {
            try {
                controller.sendMessage(messageFieldToSend.getText(),
                        new RSAKey(recipient.getText().
                                replace("\n","").replace(" ", "")));
            }
            catch (IOException | InvalidRSAKeyException k) { keyAlert.show(); k.printStackTrace(); }
            catch (NullPointerException n) { nullAlert.show();}

            messageFieldToSend.clear();
            recipient.clear();
        });

        Button spamButton = new Button("Spam");
        spamButton.setOnMouseClicked(e -> {
            Thread spam =new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    try {
                        controller.sendMessage("Hej");
                        System.out.println("Spam message sent");
                        Thread.sleep(2000);
                    }
                    catch (NullPointerException | InterruptedException n) { nullAlert.show();}
                }
            });
            spam.start();
        });

        vbox.getChildren().addAll(new Label("Message:"), messageFieldToSend);
        vbox.getChildren().addAll(new Label("Recipient:"), recipient);
        vbox.getChildren().addAll(new Label("Inbox"), emailsRecieved);
        vbox.getChildren().addAll(sendMessageButton,spamButton);

        Thread timedBlockIndexRequest = new Thread(() -> {
            try {
                while(true) {
                    Thread.sleep(20000);
                    controller.getLatestBlockIndex();
                }
            }
            catch (InterruptedException | SQLException e) { e.printStackTrace(); }
        });
        timedBlockIndexRequest.start();

        primaryStage.setTitle("Client");

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
