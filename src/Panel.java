import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Panel extends JPanel implements MouseListener, MouseMotionListener {
    public boolean ready = false;

    private boolean loadedAssets;
    private DataPackage dataPackage;
    private ArrayList<DrawingComponent> drawingComponents;
    private ArrayList<String> messagesToRender;
    private int previousMessagesToRenderSize = 0;

    Font textFont;
    private ImageIcon avatar;
    private Image bgImage, OGCanvasPanel, canvasImage, colorPickerImage, pencilImage,
            eraserImage, thickSelectImage1, thickSelectImage2, thickSelectImage3, thickSelectImage4, alarmImage;
    BufferedImage bufferedColorPickerImage;
    private Rectangle canvasPanel, colorPickerPanel, pencilPanel, eraserPanel, thickSelectPanel1, thickSelectPanel2,
            thickSelectPanel3, thickSelectPanel4;
    private JTextArea timerText, roundProgressText;
    private JTextField textField;
    private JList messageList, playerList, pointsGainedNameList, pointsGainedPointsList;
    private JScrollPane messagePane, playerPane, pointsGainedNamePane, pointsGainedPointsPane;
    private JScrollBar messagePaneScrollBar;

    private Client client;
    public Panel(Client client) throws IOException {
        this.client = client;
        updateFromClient();

        setLayout(null);//prevents any form of auto layout
        addMouseListener(this);//used to detect mouse actions
        addMouseMotionListener(this);//used to detect mouse dragging
        startMidi("assets/bgmusic.mid");//starting music

        loadAssets();
        loadRects();
        loadGuiComponents();
        loadedAssets = true;
    }

    public void addNotify() {
        super.addNotify();
        requestFocus();
        ready = true;
    }

    public void updateFromClient(){
        drawingComponents = client.getDrawingComponentsArrayList();
        dataPackage = client.getDataPackage();
        messagesToRender = client.getMessagesToRender();
    }

    //takes music file path, loads music and plays it in loop
    public void startMidi(String midFilename) {
        try {
            File midiFile = new File(midFilename);
            Sequence song = MidiSystem.getSequence(midiFile);
            Sequencer midiPlayer = MidiSystem.getSequencer();
            midiPlayer.open();
            midiPlayer.setSequence(song);
            midiPlayer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            midiPlayer.start();
        } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {e.printStackTrace();}
    }


    //renders the GUI and calls any methods relates to the GUI
    public void paintComponent(Graphics g) {
        if (g != null && loadedAssets) {
            updateUI(g);
            g.drawImage(bgImage, 0,0, null);
            g.drawImage(canvasImage, (int) canvasPanel.getX(), (int) canvasPanel.getY(), null);
            g.drawImage(colorPickerImage, (int) colorPickerPanel.getX(), (int) colorPickerPanel.getY(), null);
            g.drawImage(pencilImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
            g.drawImage(eraserImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
            g.drawImage(thickSelectImage1, (int) thickSelectPanel1.getX(), (int) thickSelectPanel1.getY(), null);
            g.drawImage(thickSelectImage2, (int) thickSelectPanel2.getX(), (int) thickSelectPanel2.getY(), null);
            g.drawImage(thickSelectImage3, (int) thickSelectPanel3.getX(), (int) thickSelectPanel3.getY(), null);
            g.drawImage(thickSelectImage4, (int) thickSelectPanel4.getX(), (int) thickSelectPanel4.getY(), null);
            g.setColor(Color.white);
            g.fillRect(10, 5, getWidth()-30, 40);

            if (drawingComponents.size() > 0) {
                for (DrawingComponent s : drawingComponents) {
                    g.setColor(s.getCol());
                    g.fillOval(s.getCx()-s.getStroke(), s.getCy()-s.getStroke(), s.getStroke()*2, s.getStroke()*2);
                }
//                    System.out.println(drawingComponents.size());
//                    if(drawingComponents.size() > 2000){
//                        try {
//                            canvasImage = takeScreenShot(canvasPanel);
//                            drawingComponents.clear();
//                        } catch (AWTException e) {
//                            e.printStackTrace();
//                        }
//                    }
            }
        }
    }

    public void updateUI(Graphics g){
        playerList.setListData(dataPackage.getPlayers().toArray());
        playerPane.setBounds(10, 50, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
        pointsGainedNamePane.setBounds(400, 150, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
        pointsGainedPointsPane.setBounds(600, 150, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
        //TIMER
        if (dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS)){
            g.drawImage(alarmImage, 50, 5, null);
            timerText.setVisible(true);
            timerText.setText(""+dataPackage.getTimeRemaining());//updating the timer text using data package
        }else{
            timerText.setVisible(false);
        }

        //POINTS GAINED PANEL
        if (dataPackage.getGameStatus().equals(DataPackage.BETWEENROUND)){
            drawingComponents.clear();
            canvasImage = OGCanvasPanel;
            g.setColor(new Color(235, 235, 235));
            g.fillRect((int)canvasPanel.getX(), (int)canvasPanel.getY(), (int)canvasPanel.getWidth(), (int)canvasPanel.getHeight());
            pointsGainedPointsList.setListData(dataPackage.getPlayers().toArray());
            pointsGainedPointsPane.setVisible(true);

            pointsGainedNameList.setListData(dataPackage.getPlayers().toArray());
            pointsGainedNamePane.setVisible(true);
        }else{
            pointsGainedPointsPane.setVisible(false);
            pointsGainedNamePane.setVisible(false);
        }

        if (dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS) || dataPackage.getGameStatus().equals(DataPackage.BETWEENROUND)){
            roundProgressText.setVisible(true);
            roundProgressText.setText("Round "+(dataPackage.getTotalNumOfRounds() - dataPackage.getRoundsLeft() + 1) +" of "+dataPackage.getTotalNumOfRounds());
        }else{
            roundProgressText.setVisible(false);
        }

        //MESSAGE LIST
        if (messagesToRender.size() > previousMessagesToRenderSize) {
            messageList.setListData(messagesToRender.toArray());
            previousMessagesToRenderSize = messagesToRender.size();
            messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());
        }
        messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());//temp until fix
    }

    // ------------ MouseListener ------------------------------------------
    //I WILL ADD COMMENTS LATER BECAUSE THERE IS A LOT MORE CODE TO ADD HERE
    private int x1, y1, x2, y2;
    private int mouseDist, dx, dy;

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        x1 = e.getX();
        y1 = e.getY();
        if (colorPickerPanel.contains(x1, y1)) {
            Color c = new Color(bufferedColorPickerImage.getRGB((int) (x1 - colorPickerPanel.getX()), (int) (y1 - colorPickerPanel.getY())));
            DrawingComponent.setColor(c);
        } else if (pencilPanel.contains(x1, y1)) {
            DrawingComponent.setToolType("PENCIL");
        } else if (eraserPanel.contains(x1, y1)) {
            DrawingComponent.setToolType("ERASER");
        } else if (thickSelectPanel1.contains(x1, y1)) {
            DrawingComponent.setStroke(DrawingComponent.STROKE1);
        } else if (thickSelectPanel2.contains(x1, y1)) {
            DrawingComponent.setStroke(DrawingComponent.STROKE2);
        } else if (thickSelectPanel3.contains(x1, y1)) {
            DrawingComponent.setStroke(DrawingComponent.STROKE3);
        } else if (thickSelectPanel4.contains(x1, y1)) {
            DrawingComponent.setStroke(DrawingComponent.STROKE4);
        } else if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
            drawingComponents.add(new DrawingComponent(x1, y1));
        }
    }

    synchronized public void mouseDragged(MouseEvent e) {
        x2 = e.getX();
        y2 = e.getY();

//            if (canvasPanel.contains(x1, y1) && canvasPanel.contains(x2, y2) && dataPackage.getMyPlayer().isArtist()) {
        if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
            if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL) || DrawingComponent.getToolType().equals(DrawingComponent.ERASER)) {
                mouseDist = (int)(Math.hypot(x2-x1, y2-y1)+.5);
                mouseDist = Math.max(mouseDist, 1);
                for(int i = 0; i < mouseDist; i++){
                    dx = (int)(i*(x2-x1)/mouseDist+.5);
                    dy = (int)(i*(y2-y1)/mouseDist+.5);
                    if(!(x1+dx < canvasPanel.getX()) && !(x1+dx > canvasPanel.getX()+canvasPanel.getWidth()) &&
                            !(y1+dy < canvasPanel.getY()) && !(y1+dy > canvasPanel.getY()+canvasPanel.getHeight())){
                        drawingComponents.add(new DrawingComponent(x1+dx, y1+dy));
                    }
                }
            }
        }
        x1 = x2;
        y1 = y2;
    }

    public void mouseMoved(MouseEvent e) {}

    public BufferedImage takeScreenShot(Rectangle panel) throws AWTException {
        BufferedImage image = new Robot().createScreenCapture(panel);
        System.out.println("took picture");
        System.out.println(image.getWidth());
        System.out.println(image.getHeight());
////            Rectangle newPanel = new Rectangle((int)panel.getX(), (int)panel.getY(), (int)panel.getWidth(), (int)panel.getHeight());
//            Rectangle newPanel = new Rectangle(0, 0, (int)panel.getWidth(), (int)panel.getHeight());
//            BufferedImage img = new BufferedImage(newPanel.width, newPanel.height, BufferedImage.TYPE_INT_RGB);
//            Graphics2D g2d = img.createGraphics();
//            g2d.translate(-newPanel.x, -newPanel.y);
//            panel.print( g2d );
//            g2d.dispose();
        return image;
    }

    class MKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                if (!textField.getText().equals("")) {
                    client.updateUsersTextMessage(textField.getText() );
                    textField.setText("");
                }
            }
        }
    }

    public void loadAssets() throws IOException {
        bufferedColorPickerImage = ImageIO.read(new File("image assets/Color picker.png"));
        avatar = new ImageIcon("image assets/icon.png");
        bgImage = new ImageIcon("image assets/bg4.jpg").getImage();
        OGCanvasPanel = new ImageIcon("image assets/canvas.png").getImage();
        canvasImage = OGCanvasPanel;
        colorPickerImage = new ImageIcon("image assets/Color picker.png").getImage();
        pencilImage = new ImageIcon("image assets/pencil.png").getImage();
        eraserImage = new ImageIcon("image assets/eraser.png").getImage();
        thickSelectImage1 = new ImageIcon("image assets/thick1.png").getImage();
        thickSelectImage2 = new ImageIcon("image assets/thick2.png").getImage();
        thickSelectImage3 = new ImageIcon("image assets/thick3.png").getImage();
        thickSelectImage4 = new ImageIcon("image assets/thick4.png").getImage();
        alarmImage = new ImageIcon("image assets/alarm.png").getImage();
        try {
            textFont = Font.createFont(Font.TRUETYPE_FONT, new File("assets/tipper.otf")).deriveFont(15.5f);
        } catch (FontFormatException | IOException e) {e.printStackTrace();}
    }

    public void loadRects(){
        canvasPanel = new Rectangle(225, 50, OGCanvasPanel.getWidth(null), OGCanvasPanel.getHeight(null));
        colorPickerPanel = new Rectangle(260, 610, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        pencilPanel = new Rectangle(610, 610, pencilImage.getWidth(null), pencilImage.getHeight(null));
        eraserPanel = new Rectangle(675, 610, eraserImage.getWidth(null), eraserImage.getHeight(null));
        thickSelectPanel1 = new Rectangle(740, 610, thickSelectImage1.getWidth(null), thickSelectImage1.getHeight(null));
        thickSelectPanel2 = new Rectangle(805, 610, thickSelectImage2.getWidth(null), thickSelectImage2.getHeight(null));
        thickSelectPanel3 = new Rectangle(870, 610, thickSelectImage3.getWidth(null), thickSelectImage3.getHeight(null));
        thickSelectPanel4 = new Rectangle(935, 610, thickSelectImage4.getWidth(null), thickSelectImage4.getHeight(null));
    }

    private void loadGuiComponents(){
        timerText = new JTextArea();
        roundProgressText = new JTextArea();
        textField = new JTextField();//the box in which the user can type their message
        messageList = new JList(messagesToRender.toArray());
        messagePane = new JScrollPane(messageList);
        messagePaneScrollBar = messagePane.getVerticalScrollBar();
        playerList = new JList(dataPackage.getPlayers().toArray());
        playerPane = new JScrollPane(playerList);
        pointsGainedNameList = new JList(dataPackage.getPlayers().toArray());
        pointsGainedNamePane = new JScrollPane(pointsGainedNameList);
        pointsGainedPointsList = new JList(dataPackage.getPlayers().toArray());
        pointsGainedPointsPane = new JScrollPane(pointsGainedPointsList);

        textField.setBounds(955, 578, 305, 22);
        textField.addKeyListener(new MKeyListener());
        add(textField);

        timerText.setBounds(58, 12, 23, 22);
        timerText.setFont(textFont.deriveFont(20f));
        timerText.setEditable(false);
        timerText.setVisible(false);
        add(timerText);

        roundProgressText.setBounds(100, 12, 200, 22);
        roundProgressText.setFont(textFont.deriveFont(20f));
        roundProgressText.setEditable(false);
        roundProgressText.setVisible(false);
        add(roundProgressText);

        playerList.setCellRenderer(PanelExtension.playerListRenderer(avatar, textFont, dataPackage.getMyPlayer()));
        playerPane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
        playerPane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(playerPane);
        playerList.setFixedCellHeight(75);

        messageList.setCellRenderer(PanelExtension.messageListRenderer(textFont));
        messagePane.setBounds(955, 50, 305, 525);
        messagePane.setHorizontalScrollBarPolicy(messagePane.HORIZONTAL_SCROLLBAR_NEVER);
        add(messagePane);

        pointsGainedNameList.setCellRenderer(PanelExtension.pointsGainedNameListRenderer(textFont));
        pointsGainedNamePane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
        pointsGainedNamePane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
        pointsGainedNamePane.setBorder(null);
        pointsGainedPointsPane.setBorder(null);
        pointsGainedNameList.setFixedCellHeight(75);
        add(pointsGainedNamePane);

        pointsGainedPointsList.setCellRenderer(PanelExtension.pointsGainedPointListRenderer(textFont));
        pointsGainedPointsList.setFixedCellHeight(75);
        add(pointsGainedPointsPane);
    }
}
