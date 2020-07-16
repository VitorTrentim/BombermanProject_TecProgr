import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ServidorTrabalhador extends Thread {
    ArrayList<Cliente> player = new ArrayList<Cliente>(4);
    private final Socket cliente;
    ServidorTrabalhador(Socket cliente, ArrayList<Cliente> player){
        this.cliente = cliente;
        this.player = player;
    }
        @Override
        public void run (){
            try{
                handleClientSocket();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e2){
                e2.printStackTrace();
            }
        } 
    
        private void handleClientSocket () throws IOException,InterruptedException {
            InputStream input = cliente.getInputStream();
            OutputStream out = cliente.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String mensagem;

            while ((mensagem = reader.readLine()) != null){
                if (mensagem == "Fim")
                    break;
                if (mensagem == "pos"){
                    // Envia as posicoes dos players
                    for (int i = 0; i < 4; i++){
                        out.write(player.get(i).arrayPlayers[i].X);
                        out.write(player.get(i).arrayPlayers[i].Y);
                    }
                    // recebe as posicoes
                    for (int i = 0; i < 4; i++){
                        player.get(i).arrayPlayers[i].X = input.read();
                        player.get(i).arrayPlayers[i].Y = input.read();
                    }
                }    
            }
            
            cliente.close();
    
        }
    
}