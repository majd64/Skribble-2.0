import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
//Stores all information that the client will need about the game.
//An object of this class is constantly created in the server output thread with new info and sent to the clients
//input thread

//variables comments available in the server
public class DataPackage implements Serializable {
    private static final long serialVersionUID = 69420;
    private int timeRemaining;

    private ArrayList<Player> players;
    private Player myPlayer;//the player that is receiving the package

    private DrawingComponent[] drawingComponents;
    private Player artist;
    private Map<Player, Integer> winners;

    private String gameStatus;
    public static final String WAITINGTOSTART = "waiting", BETWEENROUND = "between round", ROUNDINPROGRESS = "in progress";

    public DataPackage(String gameStatus, int timeRemaining, ArrayList<Player>players, Player myPlayer, DrawingComponent[] drawingComponents, Player artist, Map<Player, Integer> winners){
        this.gameStatus = gameStatus;
        this.timeRemaining = timeRemaining;
        this.players = players;
        this.myPlayer = myPlayer;
        this.drawingComponents = drawingComponents;
        this.artist = artist;
        this.winners = winners;
    }

    public int getTimeRemaining(){return timeRemaining;}

    public synchronized ArrayList<Player> getPlayers() {return players;}

    public Player getMyPlayer() {return myPlayer;}

    public DrawingComponent[] getDrawingComponents() {return drawingComponents;}

    public boolean amIArtist(){return (myPlayer == artist);}

    public Map<Player, Integer> getWinners(){return winners;}

    public String getGameStatus() {
        return gameStatus;
    }
}
