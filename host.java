import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author isabe
 */
public class host {
    static JFrame jf = new JFrame("JParty Host"); 
    static JPanel cards = new JPanel(new CardLayout());
    static JLabel teamsText;
    static String blank = "template.txt";
    static String[] gameOptions = {"music.txt", "coding.txt", "Select File"};
    static Color BG = new Color(0x1721ad);
    static Color FG = new Color(0Xd98c1a);
    static Color HL = new Color(0x4A72E8);
    static JButton start;
    static ServerSocket ss; // static to be closed at end of game
    static Scanner gin; // static to be closed in excpetion handling
    static ArrayList<Team> teams = new ArrayList<>();
    static Team activeTeam;
    static ArrayList<JLabel> teamLabels = new ArrayList<>();
    static final Object buzzLock = new Object();
    static boolean buzzLocked = false;
    static int buzzedTeam = -1; // 
    static Game game; // holds the game information
    
    public static void main(String args[]) {
        // load the window
        jf.setSize(400, 400);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // game select card
        JPanel gameSelect = new JPanel();
        gameSelect.setBackground(BG);
        gameSelect.setLayout(new BoxLayout(gameSelect, BoxLayout.Y_AXIS));
        JLabel text1 = new JLabel("This is JParty!");
        text1.setBackground(BG);
        text1.setForeground(Color.WHITE);
        text1.setOpaque(true);
        text1.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameSelect.add(text1);
        gameSelect.add(Box.createVerticalStrut(100));
        JPanel buttonPanel1 = new JPanel();
        buttonPanel1.setBackground(BG);
        for (String option : gameOptions) {
            JButton button = new JButton(option);
            button.addActionListener(new ButtonListener());
            buttonPanel1.add(button);
        }
        gameSelect.add(buttonPanel1); 
        
        // team select card
        JPanel teamSelect = new JPanel();
        teamSelect.setBackground(BG);
        teamSelect.setLayout(new BoxLayout(teamSelect, BoxLayout.Y_AXIS));
        teamSelect.setLayout(new BorderLayout());
        teamsText = new JLabel("Waiting for teams to join. . .");
        teamsText.setBackground(BG);
        teamsText.setForeground(Color.WHITE);
        teamsText.setOpaque(true);
        teamsText.setAlignmentX(Component.CENTER_ALIGNMENT);
        teamSelect.add(teamsText, BorderLayout.NORTH);
        teamSelect.add(Box.createVerticalStrut(100));
        JPanel teamSlots = new JPanel(new GridLayout(1, 3, 10, 0));
        teamSlots.setBackground(BG);
        for (int i=1; i<=3; i++) {
            JLabel slot = new JLabel("Team "+i, SwingConstants.CENTER);
            slot.setOpaque(true);
            slot.setBackground(Color.WHITE);
            teamSlots.add(slot);
        }        
        teamSelect.add(teamSlots, BorderLayout.CENTER);
        // JPanel startPanel = new JPanel();
        // startPanel.setBackground(BG);
        start = new JButton("Start Game");
        start.addActionListener(new ButtonListener());
        // startPanel.add(start);
        teamSelect.add(start, BorderLayout.SOUTH);
        // teamSelect.add(startPanel);
        
        // add cards to CardLayout and to window
        cards.add(gameSelect, "Games");
        cards.add(teamSelect, "Teams");
        jf.add(cards, BorderLayout.CENTER);
        jf.setVisible(true);

        // wait for teams to join
        try {
            ss = new ServerSocket(5190);
            while (teams.size() < 3) { 
                teamsText.setText(String.format(prompts.WAITING_HOST, teams.size()));
                teamsText.setText("Waiting for teams to join ("+teams.size()+"/3)...");
                Socket s = ss.accept();
                Team team = new Team(s);
                teams.add(new Team(s)); // adds each team to arraylist
                new ProcessConnection(s).start();
            }
        } catch (IOException ex) {System.out.println("IOException: "+ex.toString());}

        // wait for teams to select names and ready up
        teamsText.setText("Waiting for teams to be ready...");
    }

    static class ProcessConnection extends Thread { // connects each client to host (but not to each other)
        int id;
        Socket s;
        ProcessConnection(Socket s) {this.s = s;}
        @Override
        public void run() {
            try {
                Scanner sin = new Scanner(s.getInputStream());
                while (!sin.hasNextLine()) {}
                System.out.println("New connection from "+sin.nextLine()+":");
                String line;
                while (true) {
                    if (sin.hasNextLine()) {
                        synchronized (activeTeam) {activeTeam = teams.get(id);}
                        
                        line = sin.nextLine();
                        // for (PrintStream stream : streams) {stream.print(line+'\n');}
                    }
                }
            } catch (IOException iox) {System.out.println("IOException: "+iox.toString());}
        }
    }

    static class Team {
        Socket s;
        PrintStream out;
        Scanner in;
        String name;
        int score;
//        JButton right, wrong;
        
        Team(Socket s) {
            this.s = s; 
            try {
                out = new PrintStream(s.getOutputStream());
                in = new Scanner(s.getInputStream());
                name = in.nextLine();
            } catch (IOException iox) {System.out.println("InterruptedException: "+iox.toString());}
            name = ""; score = 0;
        }
        Team(String name) {this.name = name; score=0;}
    }
    
    static IOException failure() {gin.close(); return new IOException("Invalid template");}

    static class Game {
        ArrayList<Team> teams = new ArrayList<>();
        JPanel boards, teamDisplay; 
        Board singleJ = new Board(), doubleJ = new Board();
        // Board activeBoard = singleJ;
        String activeBoard = "Single";
        Category finalJ;
        Clue activeClue = new Clue();
        File template; 

        Game(String path) throws IOException, NumberFormatException {
            this.template = new File(path);
            initialize();
        }; 
        
        public void setTeams(ArrayList<host.Team> teams) {this.teams = teams;} // sets list of teams in this file
        
        private void initialize() throws IOException, FileNotFoundException {
            gin = new Scanner(template);
            String question, answer;
            int value;
            
            if (!gin.nextLine().equals("Single Jeopardy")) {throw failure();}
            for (int i = 0; i < 6; i++) {
                Category category = new Category(gin.nextLine().toUpperCase());
                for (int j = 0; j < 5; j++) {
                    try {value = Integer.parseInt(gin.nextLine());}
                    catch (NumberFormatException nfe) {throw nfe;}
                    question = gin.nextLine();
                    answer = gin.nextLine();
                    category.clues.add(new Clue(value, question, answer));
                }
                singleJ.categories.add(category);
                if (!gin.nextLine().equals(".")) {throw failure();}
            }
            
            if (!gin.nextLine().equals("---") || !gin.nextLine().equals("Double Jeopardy")) {throw failure();}
            for (int i = 0; i < 6; i++) {
                Category category = new Category(gin.nextLine().toUpperCase());
                for (int j = 0; j < 5; j++) {
                    try {value = Integer.parseInt(gin.nextLine());}
                    catch (NumberFormatException nfe) {throw nfe;}
                    question = gin.nextLine();
                    answer = gin.nextLine();
                    category.clues.add(new Clue(value, question, answer));
                }
                doubleJ.categories.add(category);
                if (!gin.nextLine().equals(".")) {throw failure();}
            }
            
            if (!gin.nextLine().equals("---") || !gin.nextLine().equals("Final Jeopardy")) {throw failure();}
            finalJ = new Category(gin.nextLine());
            finalJ.add(new Clue(-1, gin.nextLine(), gin.nextLine()));
            gin.close();
        }
        
        public void play() {
            JFrame main = new JFrame("It's Time for JParty!");
            main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            main.setExtendedState(JFrame.MAXIMIZED_BOTH);

            singleJ.build();
            doubleJ.build();
            activeClue = new Clue();
            activeClue.flip(); // only ever needs to show Q&A
            boards = new JPanel(new CardLayout());
            boards.add(singleJ, "Single");
            boards.add(doubleJ, "Double");
            boards.add(finalJ, "Final");
            boards.add(activeClue, "Active");

            JPanel bottom = new JPanel(new BorderLayout());  
           // bottom.setBackground(Color.GRAY);
            bottom.setPreferredSize(new Dimension(600, 200));
            
            // Signals buttons
            JPanel sidebar = new JPanel(new GridLayout(4, 1, 10, 10));
            //sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.X_AXIS));
            sidebar.setBackground(Color.GRAY);
            sidebar.setBorder(BorderFactory.createTitledBorder("Signals"));
            String[] signalNames = {"OPEN", "CLOSE", "NEXT", "OVER"};
            for (String signal:signalNames) {
                JButton button = new JButton(signal);
                button.setFont(new Font("Arial", Font.BOLD, 25));
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                button.addActionListener(e -> {
                    switch(button.getText()) {
                        case "OPEN":
                            for (Team team : teams) team.out.println(signals.OPEN);
                        case "CLOSE":
                            for (Team team : teams) team.out.println(signals.CLOSED);
                        case "NEXT":
                            ((CardLayout) boards.getLayout()).next(boards);
                            break;
                        case "OVER":
                            // System.exit(0);
                            main.dispose();
                    }
                    System.out.println("Signal: " + signal); // replace w/ actual logic
                });
                sidebar.add(button);
            }         
            bottom.add(sidebar, BorderLayout.WEST);
            
            // Team display
            this.teamDisplay = new JPanel();
            teamDisplay.setLayout(new FlowLayout(FlowLayout.CENTER, 100, 50));
            teamDisplay.setBackground(Color.BLACK);
            
            System.out.println("Number of teams: " + teams.size()); // DEBUGGING
            
            for (Team team : this.teams) {
                JPanel teamPanel = new JPanel();
                teamPanel.setLayout(new BoxLayout(teamPanel, BoxLayout.Y_AXIS));
                teamPanel.setBackground(Color.BLACK);
                
                // Team name
                JLabel name = new JLabel(team.name, SwingConstants.CENTER);
                name.setOpaque(true);
                name.setBackground(Color.WHITE);
                name.setForeground(Color.BLACK);
                name.setFont(new Font("Arial", Font.BOLD, 30));
                name.setPreferredSize(new Dimension(200, 50));
                name.setAlignmentX(Component.CENTER_ALIGNMENT);
                
                JTextField scoreField = new JTextField("$0");
                //score.setOpaque(true);
                scoreField.setFont(new Font("Arial", Font.PLAIN, 25));
                scoreField.setHorizontalAlignment(SwingConstants.CENTER);
                scoreField.setEditable(true); // allow host to change score
                scoreField.setBackground(Color.BLUE);
                scoreField.setForeground(Color.YELLOW);
                scoreField.setPreferredSize(new Dimension(200, 50));
                scoreField.setAlignmentX(Component.CENTER_ALIGNMENT);
                
                scoreField.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            //update team score
                            int newScore = Integer.parseInt(scoreField.getText().replace("$", ""));
                            team.score = newScore; 
                        }
                        catch (NumberFormatException ex) { scoreField.setText("$0"); } // reset to 0
                    }
                });
               
                teamPanel.add(name);
                teamPanel.add(scoreField);
                teamPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE)); // DEBUGGING
                teamDisplay.add(teamPanel);
            }
            
            bottom.add(teamDisplay, BorderLayout.CENTER);
            
            sidebar.setBorder(BorderFactory.createLineBorder(Color.RED));
            teamDisplay.setBorder(BorderFactory.createLineBorder(Color.GREEN));

            main.add(boards, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.SOUTH);
            main.setVisible(true);
        }
        
        public void guess(int id) {
            
        }

        class Board extends JPanel { 
            ArrayList<Category> categories = new ArrayList<>();
            Board() {setLayout(new GridLayout(6, 6, 5, 5)); setBackground(Color.BLACK);}
            void build() {
                for (int j = 0; j < 6; j++) add(categories.get(j));
                for (int i = 0; i < 5; i++)
                    for (int j = 0; j < 6; j++)
                        add(categories.get(j).clues.get(i));
            }
            // @Override public void remove(Component comp) {}
            // @Override public void remove(int i) {}
        }
        
        class Category extends JPanel {
            JLabel label = new JLabel();
            ArrayList<Clue> clues = new ArrayList<>();

            Category(String title) {
                setLayout(new BorderLayout());
                setBackground(BG);
                // label.setText("<html><div style='text-align:center;'>" + title + "</div></html>");
                label.setText(title);
                // label.setAlignmentX(CENTER_ALIGNMENT);
                // label.setAlignmentX(CENTER_ALIGNMENT);
                label.setForeground(Color.WHITE);
                label.setFont(new Font ("Arial", Font.BOLD, 16));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                
                add(label, BorderLayout.CENTER);
                // label.setForeground(Color.WHITE);
                // add(label);
                // format(this);
            }

            boolean isEmpty() {
                for (Clue clue:clues)
                    if (!clue.answered) {return false;}
                return true;
            }
        }

        class Clue extends JPanel {
            // String question, answer;
            int value;
            JLabel labelV = new JLabel(), labelQ = new JLabel(), labelA = new JLabel();
            JPanel top, bottom;
            boolean answered=false, isFinal=false;
            
            Clue() {
                setLayout(new CardLayout());
                setBackground(BG);
                labelV.setHorizontalAlignment(SwingConstants.CENTER);
                labelV.setVerticalAlignment(SwingConstants.CENTER);
                labelV.setForeground(FG);
                labelV.setFont(new Font("Arial", Font.BOLD, 60)); 
                labelQ.setForeground(Color.WHITE);
                labelQ.setFont(new Font("Arial", Font.BOLD, 40));
                labelA.setForeground(Color.WHITE);
                labelA.setFont(new Font("Arial", Font.BOLD, 40));
                top = new JPanel(new BorderLayout());
                bottom = new JPanel(new BorderLayout());
                top.add(labelV);
                bottom.add(labelQ, BorderLayout.CENTER);
                bottom.add(labelA, BorderLayout.SOUTH);
                format(top);
                format(bottom);
                // format(this);
                add(top, "Top");
                add(bottom, "Bottom");
                addMouseListener(new ClueListener());
                value=0;
            }
            Clue(int value, String question, String answer) {
                this();
                this.value = value;
                labelV.setText("$"+value);
                labelQ.setText(question);
                labelA.setText(answer);
                labelA.setVisible(false);
            }

            public void copy(Clue other) {
                this.value = other.value;
                this.labelV.setText(other.labelV.getText());
                this.labelQ.setText(other.labelQ.getText());
                this.labelA.setText(other.labelA.getText());
                labelA.setVisible(other.labelA.isVisible());
                this.isFinal=other.isFinal;
            }
            public boolean isEmpty() {
                return (value==0 
                    & labelV.getText().isEmpty() 
                    & labelQ.getText().isEmpty() 
                    & labelA.getText().isEmpty());
            }
            public void clear() {value = 0; labelV.setText(""); labelQ.setText(""); labelA.setText("");}
            public void flip() {((CardLayout) getLayout()).next(this);}
            public void reveal() {labelA.setVisible(true);}
            class ClueListener implements MouseListener {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Clue owner = (Clue) e.getSource();
                    if (activeClue.isEmpty()) {
                        activeClue.copy(owner);
                        ((CardLayout) boards.getLayout()).last(boards);
                    } else {
                        if (!activeClue.labelA.isVisible()) activeClue.reveal();
                        else {
                            activeClue.clear();
                            ((CardLayout) boards.getLayout()).show(boards, activeBoard);
                        }
                    }
                }
                @Override public void mousePressed(MouseEvent e) {}
                @Override public void mouseReleased(MouseEvent e) {}
                @Override public void mouseEntered(MouseEvent e) {
                    Clue owner = (Clue) e.getSource();
                    if (!activeClue.equals(owner) && activeClue!=owner) owner.top.setBackground(HL);
                }
                @Override public void mouseExited(MouseEvent e) {
                    Clue owner = (Clue) e.getSource();
                    if (!activeClue.equals(owner) && activeClue!=owner) owner.top.setBackground(BG);
                }
            }

        }
    }
    
    static class ButtonListener implements ActionListener { 
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = (JButton) e.getSource();
            CardLayout cl = (CardLayout)(cards.getLayout());
            
            switch (button.getText()) {
                case "Select File":
                    // open and read file from computer
                    JFileChooser chooseFile = new JFileChooser();
                    chooseFile.setFileFilter(new FileNameExtensionFilter("Text Files Only", "txt", "text"));
                    int value = chooseFile.showOpenDialog(null);
                    if (value == JFileChooser.APPROVE_OPTION) {
                        File chosenFile = chooseFile.getSelectedFile();
                        try {
                            game = new Game(chosenFile.getAbsolutePath());
                            cl.show(cards, "Teams");
                        }
                        catch (IOException | NumberFormatException x) {
                            JOptionPane.showMessageDialog(null, "Error loading file:\n" + x.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    break;
                case "Start Game":
                    if (game==null) {teamsText.setText("Game not initialized");}
                    else {game.play();}
                    break;
                default:
                    try {
                        game = new Game(button.getText());
                        cl.show(cards, "Teams");
                        break;
                    }
                    catch (IOException | NumberFormatException x) {
                        JOptionPane.showMessageDialog(null, "Error loading file:\n" + x.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
            }
        }
    } 

    static public void format(JPanel panel) {
        panel.setBackground(BG);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
    }

    public class prompts {
        static final String CONNECT_INIT = "Type the host server and tap this button to connect."; // first msg that appears when a client opens the window
        static final String CONNECT_FAIL = "Connection to %s failed; try again"; // typed invalid server name
        static final String WAITING_HOST = "Waiting for teams to join (%d/3)..."; // msg for host while contestants are still joining (WAITING signal)
        static final String WAITING_CLIENT = "Waiting for teams to join..."; // msg for client while others are still able to join (WAITING signal)
    }
    
    public class signals {
        static final String WAITING = "0"; // not every contestant has joined yet // Ready button for host 
        static final String CLOSED = "1"; // buzzer is locked as a question has not yet been fully read // Lock buzzer button for host
        static final String OPEN = "2"; // question has been read and anyone can buzz in // Open buzzer button for host
    //    static final String NEXT = "3"; // moving onto next clue; host can just close the buzzers
        static final String OVER = "4"; // when single jeopardy ends // End game button for host
    }
}
