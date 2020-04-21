//Imports
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//Stores all the global information about the game and starts the listening thread
public class Server {
    private boolean running = true;
    private String gameState = DataPackage.WAITINGFORPLAYERS;
    private int timeRemaining = -1;

    private ArrayList<Player> players = new ArrayList<Player>();
    private String message;//the text message
    private ArrayList<Player> messageReaders = new ArrayList<Player>();//the players who read the text message

    private DrawingComponent[] drawingComponents = null;
    private Player artist;
    private ArrayList<Player> previousArtists = new ArrayList<>();
    private ArrayList<Player> winners = new ArrayList<Player>();
    private String currentMagicWord = null; //should be a read from a txt file
    private ArrayList<String> magicWords = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        new ListenForClients(server).start();
        server.loadMagicWords("words");
    }

    public boolean isRunning(){return running;}

    public void setGameState(String state){
        if (state.equals(DataPackage.GAMESTARTING) || state.equals(DataPackage.ROUNDINPROGRESS) || state.equals(DataPackage.WAITINGFORPLAYERS)){
            gameState = state;
        }
        if (gameState.equals(DataPackage.GAMESTARTING)){
            newRound();
        }
    }

    public String getGameState(){return gameState;}

    public void newRound(){
        drawingComponents = null;
        timeRemaining = 90;
        gameTimer.start();
        if (previousArtists.size() >= players.size()){
            previousArtists.clear();
        }
        Player randArtist;
        while (true) {
            randArtist = players.get(randint(0, players.size() - 1)); //or we could go through the list
            if (!previousArtists.contains(randArtist)) {
                artist = randArtist;
                previousArtists.add(randArtist);
                break;
            }
        }
        winners = new ArrayList<Player>();
        currentMagicWord = magicWords.get(randint(0,magicWords.size()-1));
        magicWords.remove(currentMagicWord);
        if (magicWords.size() == 0){
            try {
                loadMagicWords("words");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        randArtist.addMessageOnlyForMe("#FF0000~You are the artist");
        randArtist.addMessageOnlyForMe("#FF0000~you must draw: "+currentMagicWord);

        for (Player p :players){
            if (p != randArtist){
                p.addMessageOnlyForMe("#FF0000~"+randArtist.getName()+" Is the artist");
            }
        }
    }

    public List<Player> getPlayers() {return players;}

    public synchronized void playerConnected(Player player) {
        players.add(player);
        newMessage(("#FF0000~" + player.getName() + " has joined the game"), player);
    }

    public synchronized void playerDisconnected(Player player){
        players.remove(player);
        newMessage("#FF0000~" + player.getName() + " has left the game", player);
    }

    public synchronized void newMessage(String msg, Player player){
        messageReaders.clear();
        if (msg.contains("~")) {//this means that the client is sending a message and not the user
            message = msg;
        } else {
            if(currentMagicWord != null && msg.toLowerCase().equals(currentMagicWord.toLowerCase())){
                winners.add(player);
                message = ("#FF0000~"+player.getName() + " has guessed correctly!");
                player.addMessageOnlyForMe("#FF0000~You have guessed correctly!");
                if(winners.size() == players.size() - 1){
                    System.out.println("new round");
                    newRound();
                }
            }
            else{
                message = (player.getName()) + ": " + msg;
            }
        }
        messageReaders.add(player);
    }

    public synchronized String getMessage(Player player) {
        if (messageReaders.contains(player)){
            return null;
        }else{
            messageReaders.add(player);
            return message;
        }
    }

    public synchronized void setDrawingComponents(DrawingComponent[] components) {
        this.drawingComponents = components;
    }

    public boolean isArtist(Player player){
        return (player == artist);
    }

    //groups all of the global information into 1 object for distribution to all clients
    public synchronized DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {e.printStackTrace();}
        return new DataPackage(gameState,timeRemaining, players, player, getMessage(player), drawingComponents, artist, winners);
    }

    public void loadMagicWords(String filename) throws IOException{
        Scanner inFile = new Scanner(new BufferedReader(new FileReader(filename)));
        int numWords = inFile.nextInt();
        inFile.nextLine();
        for(int i = 0; i < numWords; i++){
            magicWords.add(inFile.nextLine());
        }
        inFile.close();
    }

    public static int randint(int low, int high){
        //this method returns a random int between the low and high args inclusive
        return (int)(Math.random()*(high-low+1)+low);
    }

    private Timer gameTimer = new Timer(1000, new TickListener());
    class TickListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            timeRemaining--;
            if (timeRemaining == 0){
                gameTimer.stop();
                newRound();
            }
        }
    }
}

// ------------ Listening Thread ------------------------------------------
//Listens for new clients and creates the I/O threads when a client joins
class ListenForClients extends Thread{
    private Server server;
    public ListenForClients(Server server) {
        this.server = server;
    }

    public void run( ){
        int portNumber = 4445;
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Player player = new Player("");
                new ServerChatInputThread(server, player, socket).start();
                new ServerOutputThread(server, player, socket).start();
            }
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(-1);
        }
    }
}

// ------------ I/O TO Client Threads ------------------------------------------
//Reads messages from the client and sends them to the server
class ServerOutputThread extends Thread {
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectInputStream in;
    private boolean gotUserName = false;

    public ServerOutputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            while (server.isRunning()) {
                if (server.isArtist(player)){
                    try {
                        DrawingComponent[] shapesArray = (DrawingComponent[]) in.readObject();
                        server.setDrawingComponents(shapesArray);
                    }catch(ClassCastException ignored){}
                } else {
                    String inputLine;
                    try {
                        if ((inputLine = (String) in.readObject()) != null) {
                            if (!gotUserName) {
                                player.setName(inputLine);
                                server.playerConnected(player);
                                gotUserName = true;
                            } else if (server.getGameState().equals(DataPackage.WAITINGFORPLAYERS) && inputLine.equals("START") && server.getPlayers().get(0) == player) {
                                server.setGameState(DataPackage.GAMESTARTING);
                            } else {
                                server.newMessage(inputLine, player);
                            }
                        }
                    }catch (SocketException | ClassCastException ignored){}
                }
            }
        } catch(IOException | ClassNotFoundException e){e.printStackTrace();}
        finally{
            server.playerDisconnected(player);
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

//Gets the data package from the server and sends it to the client
class ServerChatInputThread extends Thread{
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    public ServerChatInputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void run(){
        try {
            while (server.isRunning()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {e.printStackTrace();}
                DataPackage dataPackage = server.getDataPackage(player);
                objectOutputStream.writeUnshared(dataPackage);
                objectOutputStream.flush();
                objectOutputStream.reset();
            }
        }
        catch (IOException e) {e.printStackTrace();}
        finally {
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
