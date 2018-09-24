package javaFX_server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class server_app extends Application implements EventHandler<ActionEvent>{
	
	
	public static void main(String[] argv) {
		launch(argv);
	}
	
	/////////////////////////////////
	/// JavaFX methods and variables
	/////////////////////////////////
	
	//Server status scene.
	VBox vb_serverStatus;
	Text  t_serverStatus;
	Text  t_clientStatus;
	TextField 	t_showIP;
	Button		b_showIP;
	boolean    ipShowing;
	
	private Scene setStatusScene() {
		t_serverStatus=new Text("Trying to establish ServerSocket");
		t_clientStatus=new Text("");
		
		b_showIP = new Button("Show IP");
		b_showIP.setOnAction(this);
		
		t_showIP = new TextField("***.***.***.***:"+PORT);
		t_showIP.setEditable(false);
		t_showIP.setAlignment(Pos.CENTER);
		
		vb_serverStatus=new VBox(t_serverStatus,t_clientStatus,b_showIP,t_showIP);
		vb_serverStatus.setAlignment(Pos.CENTER);
		
		ipShowing = false;
		
		StackPane layout = new StackPane();
		layout.getChildren().add(vb_serverStatus);
		return new Scene(layout,300,200);
	}
	
	
	//Client selection scene.
	Text       ss_info;
	VBox  submitBundle;
	HBox     hb_submit;
	Button    b_submit;
	TextField tf_clNum;
	
	private Scene setSelectionScene() {
		b_submit=new Button("Submit");
		b_submit.setOnAction(this);
		
		ss_info=new Text("Enter number of clients");
		ss_info.setTextAlignment(TextAlignment.CENTER);
		ss_info.setOnKeyPressed(
				(event) -> { 
					if(event.getCode() == KeyCode.ENTER)
						b_submit.fire();
				});
		
		tf_clNum = new TextField();
		tf_clNum.setPrefWidth(60);
		hb_submit    = new HBox(tf_clNum,b_submit);
		hb_submit.setAlignment(Pos.CENTER);
		
		submitBundle = new VBox(ss_info,hb_submit);
		submitBundle.setAlignment(Pos.CENTER);
		
		StackPane layout = new StackPane();
		layout.getChildren().add(submitBundle);
		return new Scene(layout,300,200);	
	}
	
	Stage prime;
	
	@Override
	public void start(Stage primaryStage) {
		prime = primaryStage;
		prime.setTitle("Chat server");
		prime.setScene(setSelectionScene());
		
		prime.show();
	}
	
	//////////////////////////////////////////////
	/// Server functionality methods and variables
	//////////////////////////////////////////////
	int PORT = 1984;
	int CLIENT_COUNT;
	ServerSocket SOURCE;
	boolean establishedPort=false;
	
	private void establishPort() {
		t_serverStatus.setText("Attempting to start server on port "+PORT);
		
		while(establishedPort==false) {
			try {
				SOURCE = new ServerSocket(PORT);
				establishedPort = true;
				
				t_serverStatus.setText("Server running on port "+PORT);
			}catch(IOException e) {
				
				t_serverStatus.setText("Failed to establish "+PORT);
				++PORT;
			}
		}
	}
	
	DataOutputStream[] clients_output;
	 DataInputStream[] clients_input;
	          Socket[] clients;	
	
    Thread t_listener;
	private void listen() {
		clients_output = new DataOutputStream[CLIENT_COUNT];
		clients_input = new DataInputStream [CLIENT_COUNT];
		clients = new Socket[CLIENT_COUNT];
		
		Runnable r_listener = ()->{
			for(int i=0;i<CLIENT_COUNT;++i) {
				try {
					clients[i]=SOURCE.accept();
					int tmp = i+1;
					int tmp2=CLIENT_COUNT-tmp;
					t_clientStatus.setText( tmp+" users connected so far. Waiting for "+tmp2 +" more.");
					
				}catch(IOException e) {
					System.out.println("Failed to establis connection with user " + (i+1) );
				}
			}
			
			for(int i=0;i<CLIENT_COUNT;++i) {
				try {
					clients_output[i]= new DataOutputStream(clients[i].getOutputStream());
					clients_input[i] = new DataInputStream(clients[i].getInputStream());
				}catch(IOException e) {
					try {clients[i].close();} catch (IOException e1) {}
					System.out.println("I/O Error on client "+ i);
					System.exit(-1);
				}
			}
			
			t_clientStatus.setText("All users connected.");
			clientCommunication();
		};
		
		t_listener = new Thread(r_listener);
		t_listener.start();
	}
	
	
	String[] userNames; 
	
	Thread t_clientCommunication;
	private void clientCommunication() {
		userNames = new String[CLIENT_COUNT];
		
		Runnable[] client_threads_receive = new Runnable[CLIENT_COUNT];
			for(int i=0;i<CLIENT_COUNT;++i) {
				final int tmp=i;
				
				client_threads_receive[i]=()->{
					String Message="";
					try {
						userNames[tmp]=clients_input[tmp].readUTF();
						System.out.println(userNames[tmp]);
					}catch(IOException e){
						System.out.print("Unable to get client username\n");
						System.exit(-1);
					}
					while(true) {
						try {
							Message=clients_input[tmp].readUTF();
							for(int j=0;j<CLIENT_COUNT;++j) {
								if(j!=tmp) {
									System.out.println("Sending message from "+userNames[j]+" to "+userNames[tmp]);
									clients_output[j].writeUTF(userNames[tmp]+": "+Message);
								}
							}
						}catch(IOException e) {
							System.out.println("Message I\\O error from "+ userNames[tmp]);
							System.exit(-1);
						}
					}
				};
				
				Thread w = new Thread(client_threads_receive[i]);
				w.start();
			}

	}
	
	private String getIP() throws IOException {
		URL amazonService = new URL("http://checkip.amazonaws.com");
		BufferedReader amazonServiceInput = null;
		
		amazonServiceInput = new BufferedReader(
				new InputStreamReader(amazonService.openStream()));

		String ip = amazonServiceInput.readLine();
	    return ip;
	}

	@Override
	public void handle(ActionEvent event) {
		/// Get number of clients and start server.
		if(event.getSource()==b_submit) {
			String tmp = tf_clNum.getText();
			int a = 0;
			
			a=Integer.parseInt(tmp);
			CLIENT_COUNT = 0;
			
			if(a>1&&a<1000) {
				prime.setScene(setStatusScene());
				prime.show();
				CLIENT_COUNT = a;
				establishPort();
				t_clientStatus.setText("Waiting for "+CLIENT_COUNT+" users to connect.");
				listen();
			}
		}
		
		if(event.getSource()==b_showIP) {
			if(!ipShowing) {
				try {
					t_showIP.setText(getIP()+ ":" + PORT);
					b_showIP.setText("Hide IP");
					ipShowing = true;
				}catch(IOException e) {
					t_showIP.setText("Failed to retreive IP");
				}				
			}
			else {
				t_showIP.setText("***.***.***.***:"+PORT);
				b_showIP.setText("Show IP");
				ipShowing = false;
			}
		}
	}
}
