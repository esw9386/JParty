import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;
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
    static Color BG = new Color(0x314bcc);
    static Color FG = new Color(0Xf7e686);
    static JButton start;
    static ServerSocket ss; // static to be closed at end of game
    static Scanner gin; // static to be closed in excpetion handling
    static ArrayList<Team> teams = new ArrayList<>();
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
                teams.add(new Team(s)); // adds each team to arraylist
                new ProcessConnection(s).start();
            }
        } catch (IOException ex) {System.out.println("IOException: "+ex.toString());}

        // wait for teams to select names and ready up
        teamsText.setText("Waiting for teams to be ready...");
    }

    static class ProcessConnection extends Thread { // connects each client to host (but not to each other)
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
            } catch (IOException iox) {System.out.println("InterruptedException: "+iox.toString());}
            name = ""; score = 0;
        }
        Team(String name) {this.name = name; score=0;}
    }
    
    static IOException failure() {gin.close(); return new IOException("Invalid template");}

    static class Game {
        ArrayList<Team> teams = new ArrayList<>();
        Board singleJ = new Board(), doubleJ = new Board();
        Category finalJ;
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
                Category category = new Category(gin.nextLine());
                for (int j = 0; j < 5; j++) {
                    try {value = Integer.parseInt(gin.nextLine());}
                    catch (NumberFormatException nfe) {throw nfe;}
                    question = gin.nextLine();
                    answer = gin.nextLine();
                    category.clues.add(new Clue(value, question.toUpperCase(), answer.toUpperCase()));
                }
                singleJ.categories.add(category);
                if (!gin.nextLine().equals(".")) {throw failure();}
            }
            
            if (!gin.nextLine().equals("---") || !gin.nextLine().equals("Double Jeopardy")) {throw failure();}
            for (int i = 0; i < 6; i++) {
                Category category = new Category(gin.nextLine());
                for (int j = 0; j < 5; j++) {
                    try { value = Integer.parseInt(gin.nextLine()); }
                    catch (NumberFormatException nfe) {throw nfe;}
                    question = gin.nextLine();
                    answer = gin.nextLine();
                    category.clues.add(new Clue(value, question.toUpperCase(), answer.toUpperCase()));
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

            JPanel clues = new JPanel();
            clues.setBackground(Color.BLACK);
            clues.setLayout(new GridLayout(6, 6, 5, 5));
            for (int j = 0; j < 6; j++) clues.add(singleJ.categories.get(j));
            for (int i = 0; i < 5; i++)
                for (int j = 0; j < 6; j++)
                    clues.add(singleJ.categories.get(j).clues.get(i));

            JPanel bottom = new JPanel();  
            bottom.setBackground(Color.GRAY);
            bottom.setPreferredSize(new Dimension(600, 200));
            JPanel sidebar = new JPanel();
            sidebar.setLayout(new GridBagLayout());
            bottom.add(sidebar);

            main.add(clues, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.SOUTH);
            main.setVisible(true);
        }
        
        public void guess(int id) {
            
        }

        class Board extends JPanel{ 
            ArrayList<Category> categories = new ArrayList<>();
            // has ArrayList<Component> components

            Board() {
                setLayout(new GridLayout(1,6,5,0)); // six Category columns
                setBackground(Color.BLACK);
                // setOpaque(true);
            }

            public void add(Category cat) {super.add(cat); categories.add(cat);}

        }
        
        class Category extends JPanel {
            JPanel head = new JPanel(new BorderLayout());
            JLabel label = new JLabel();
            boolean isFinal;
            ArrayList<Clue> clues = new ArrayList<>();

            Category(String title) {
                setBackground(BG);
                label.setText(title);
                label.setForeground(Color.WHITE);
                add(label);
                // format(this);
            }

            boolean isEmpty() {
                for (Clue clue:clues)
                    if (!clue.answered) {return false;}
                return true;
            }
        }

        class Clue extends JPanel {
            String question, answer;
            int value;
            JLabel labelV = new JLabel(), labelQ = new JLabel(), labelA = new JLabel();
            boolean answered=false;

            Clue(int value, String question, String answer) {
                // setBackground(BG);
                setLayout(new CardLayout());
                labelV.setForeground(FG);
                labelQ.setForeground(Color.WHITE);
                labelA.setForeground(Color.WHITE);
                this.value = value;
                this.question = question;
                this.answer = answer;
                labelV.setText("$"+value);
                labelQ.setText(question);
                labelA.setText(answer);
                JPanel top = new JPanel(new BorderLayout());
                JPanel bottom = new JPanel(new BorderLayout());
                top.add(labelV);
                bottom.add(labelQ, BorderLayout.CENTER);
                bottom.add(labelA, BorderLayout.SOUTH);
                format(top);
                format(bottom);
                // format(this);
                add(top, "Top");
                add(bottom, "Bottom");
            }

            // class ClueListener implements ActionListener {

            // }

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
