import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;


/**
 *
 * @author esw9386
 */
public class client {
    static String name, server, signal="";
    static final String SERVER_PROMPT = "Enter a server name.";
    static final String SERVER_FAIL = "Connection failed; try again";
    static final String NAME_PROMPT = "Enter a team name.";
    static final String CLOSED = "CLOSED";
    static final String OPEN = "BUZZ IN";
    static Socket s;
    static Scanner sin;
    static PrintStream sout;
    static boolean over;
    static Buzzer buzzer;
    static JTextField jtf;

    public static void main(String[] args) {
        JFrame jf = new JFrame("This is JParty!");
        jf.setSize(300,150);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel main = new JPanel(new BorderLayout());
        jtf = new JTextField(); // choosing team name + final jeopardy
        buzzer = new Buzzer(SERVER_PROMPT);
        buzzer.addActionListener((e) -> {
            try {
                String msg = jtf.getText();
                if (!(msg.isEmpty()|buzzer.getText().equals(CLOSED))) {
                    jtf.setText("");
                    if (sout==null) {
                        buzzer.server=msg;
                        try {buzzer.setup();}
                        catch (IOException iox) {buzzer.setText(SERVER_FAIL);}
                    } else if (name==null) {
                        buzzer.name=msg;
                        buzzer.setText(CLOSED);
                        sout.println(msg);
                    } else {sout.println(msg);}
                }
            } catch (NullPointerException npx) {System.out.println("NullPointerException: "+npx.toString());}
        });

        main.add(buzzer, BorderLayout.CENTER);
        main.add(jtf, BorderLayout.SOUTH);
        jf.add(main);
        jf.setVisible(true);
    }

    static class Buzzer extends JButton implements Runnable { // thread
        String text, name, server;
        Socket s;
        host.Game game; 
        int id; // has to be first thing sent by host (0, 1, 2)
        Buzzer(String text) {super(text); }//addActionListener(new BuzzerListener()); over=false;} 
        // Buzzer(String text, Scanner sin) {this(text); this.sin=sin;} // probs won't be used

        public void setup() throws IOException {
            if (sout==null) {
                s = new Socket(server, 5190);
                Scanner sin = new Scanner(s.getInputStream());
                (new Thread(this)).start(); // begins to listen for messages from the server
                sout = new PrintStream(s.getOutputStream());
                sout.println(name); // announces username
                setText(NAME_PROMPT);
            }
        }

        @Override
        public void run() {
            setText("Type the host server and tap this button to connect.");
            while (!over) { // game has not yet ended
                if (sin.hasNextLine()) {
                    switch (sin.nextLine()) { 
                        case signals.WAITING: // so if sin.nextLine() is equal to WAITING, which means when user input is "0"? 
                            setText("Waiting for teams to join...");
                        case signals.CLOSED: 
                        case signals.OPEN: // clue has been read and clients are prompted to buzz
                            setText("BUZZ IN");
                        // case signals.NEXT: // stays open
                        case signals.OVER: // switches to CLOSED signal too?
                            over = true;
                    }
                }
            }
        }

        // class BuzzerListener implements ActionListener {
        //     // game window will display who presses the button first (change team name color) and if they are correct or incorrect when answering
            
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         if (!over) {
        //             if (buzzer.text.equals(SERVER_PROMPT)) {
        //                 server = jtf.getText();
        //                 setup();
        //             }
        //             switch (signal) {
        //                 case signals.CLOSED: // buzzer should do nothing
        //                 case signals.OPEN: // if pressed first, buzzer should send msg to host that contestant has an answer; synchronized
        //                     synchronized(game) {
        //                         game.guess(id);
        //                     }
        //                 // case signals.NEXT: // 
        //                 case signals.WAITING: // buzzer should do nothing
        //                 case signals.OVER: // buzzer should do nothing
        //             }
        //         }
        //     }
        // }
    }
    
    public class signals {
        static final String WAITING = "0"; // not every contestant has joined yet // Ready button for host 
        static final String CLOSED = "1"; // buzzer is locked as a question has not yet been fully read // Lock buzzer button for host
        static final String OPEN = "2"; // question has been read and anyone can buzz in // Open buzzer button for host
    //    static final String NEXT = "3"; // moving onto next clue; host can just close the buzzers
        static final String OVER = "4"; // when single jeopardy ends // End game button for host
    }
}