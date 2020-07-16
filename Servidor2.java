import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Servidor2 {
    
    public static void main ( String[] args) {
        int porto = 12345;
        ArrayList<Cliente> player = new ArrayList<Cliente>(4);
        try {
             ServerSocket server = new ServerSocket(porto);
            while (true) {
                System.out.println("Aguardando conexao");
                Socket cliente = server.accept();
                System.out.println("Conexao aceita de " + cliente);
                ServidorTrabalhador trabalhador = new ServidorTrabalhador(cliente, player);
                trabalhador.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   
}