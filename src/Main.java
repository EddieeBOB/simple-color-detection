import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        //Asych helper for thread safe execution of GUI 
        SwingUtilities.invokeLater(SorterAppGUI::new);
    }
}
