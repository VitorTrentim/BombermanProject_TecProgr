/*
Trabalho de Técnicas de Programaçao - BSI UNESP Bauru
Ettore Scolar e Vitor Trentim
*/

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

class ClienteT2 extends JFrame {
    Image[] arrayImagensBomba = new Image[21];
    /// Rede
    Rede rede;
    boolean boolJogoComecou = false, boolJogoRodando = false;
    String idString;
    int id = -1;
    LeituraDoFluxo threadLeitura = new LeituraDoFluxo();
    EnvioDoFluxo threadEnvio = new EnvioDoFluxo();
    MovimentoDoPlayerDoCliente movimentoPlayerAtual = new MovimentoDoPlayerDoCliente();
    String leitura;
    String[] leituraPartes;

    //// DADOS PLAYER
    final int PERS1 = 1, PERS2 = 2;
    Player arrayPlayers[] = {new Player(PERS1), new Player(PERS2)};
    final int PARADO = 0, ANDANDO_DIREITA = 1,ANDANDO_ESQUERDA = 2, ANDANDO_FRENTE = 3, ANDANDO_COSTAS = 4, DANIFICADO = 5, LENGTH_IMAGENS_PLAYER = 6;
    String nome_do_Player, score_do_Player = null;


    //// DADOS ITENS
    ArrayList<Itens> arrayItens;
    final int ITEM_BOTA = 0, ITEM_TAMANHOEXPLOSAO = 1, ITEM_QTDEBOMBAS = 2, ITEM_VIDA =3;
    int indexItems = 0;

    //// DADOS BOMBAS
    ArrayList<Bomba> arrayBombas = new ArrayList<Bomba>(10);
    ArrayList<pontoExplosao> arrayExplosao = new ArrayList<pontoExplosao>(100); //Fogo da explosão da bomba


    //// DADOS GERAIS
    int gameControler;
    int endGame = 2;
    int somaScore, scoreVida = 300, scoreBloco;
    final int TEMPO_DA_FASE = 150, NAV_MENU = 10, inGame = 0;
    boolean boolGameOver = false, boolLastBombaBlockPlayer = false;
    Image gameOver = new ImageIcon("Resources/Layout/gameover.gif").getImage();
    ImageIcon coracaoCheio = new ImageIcon("Resources/Layout/coracaoCheio.png");
    ImageIcon coracaoVazio = new ImageIcon("Resources/Layout/coracaoVazio.png");
    Image explosao = (new ImageIcon("Resources/Frames/expl.gif")).getImage();
    Image explosao2 = (new ImageIcon("Resources/Frames/expl2.gif")).getImage(); // explosao2 é a explosão em cima dos blocos quebráveis
    JLabel primeiroCoracao = new JLabel();
    JLabel segundoCoracao = new JLabel();
    JLabel terceiroCoracao = new JLabel();
    Font fonte = new Font("Arial", Font.BOLD, 30);

    //// DADOS GUI
    Menu menu;
    layoutDeCima barraSuperior;
    JLabel labelQuantidadeItemBota, labelQuantidadeItemBomba, labelQuantidadeItemExplosao;
    JLabel labelTempoValue ,labelScore;

    //// DADOS DAS FASES
    final int MULTIPLAYER1 = 0, MULTIPLAYER2 = 1, MULTIPLAYER3 = 2, MULTIPLAYER4 = 3;
    boolean passarFase = false, ultimaFase = false, escreveu = false;
    Fase mult;
    boolean single, multiplay;

    //////////////////////////////////////////////////////

    Timer repinta = new Timer(50, e -> {
        repaint();
    });


    class Fase extends JPanel{
        ArrayList<Rectangle> arrayBlocosQuebraveis;
        Image[] imagensAmbiente = new Image[11];
        Image doorClosed;
        Rectangle[] blocosFixos;
        Rectangle colisaoPorta = new Rectangle(450,300,55,55);
        final int FUNDO = 0, BLOCO = 1, BLOCOQUEBRAVEL = 2, MARGEM_CE = 3, MARGEM_C = 4, MARGEM_CD = 5,
                MARGEM_E = 6, MARGEM_D = 7, MARGEM_BE = 8, MARGEM_B = 9, MARGEM_BD = 10;
        final int BLOCOSHORIZONTAIS = 8, BLOCOSVERTICAIS = 5;

        Fase(){
            try {
                arrayPlayers[0].X = 60; arrayPlayers[0].Y = 40; arrayPlayers[0].boolPosicaoValidada = true;
                arrayPlayers[1].X = 860; arrayPlayers[1].Y = 540; arrayPlayers[1].boolPosicaoValidada = true;
                try {
                    imagensAmbiente[FUNDO] = new ImageIcon(getClass().getResource("Resources/FaseMultiplayer/chao1.png")).getImage();
                    imagensAmbiente[BLOCO] = new ImageIcon("Resources/FaseMultiplayer/blocoFixo.png").getImage();
                    imagensAmbiente[BLOCOQUEBRAVEL] = (new ImageIcon("Resources/FaseMultiplayer/blocosQuebraveis.png")).getImage();
                    imagensAmbiente[MARGEM_CE] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCimaEsquerda.png").getImage();
                    imagensAmbiente[MARGEM_C] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCima.png").getImage();
                    imagensAmbiente[MARGEM_CD] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemCimaDireita.png").getImage();
                    imagensAmbiente[MARGEM_E] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemEsquerda.png").getImage();
                    imagensAmbiente[MARGEM_D] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemDireita.png").getImage();
                    imagensAmbiente[MARGEM_BE] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixoEsquerda.png").getImage();
                    imagensAmbiente[MARGEM_B] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixo.png").getImage();
                    imagensAmbiente[MARGEM_BD] = new ImageIcon("Resources/FaseMultiplayer/blocoMargemBaixoDireita.png").getImage();
                    doorClosed = new ImageIcon("Resources//FaseMultiplayer//doorClosed.png").getImage();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "A imagem não pode ser carregada! (Multiplayer)\n" + e, "Erro", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                funcAdcBlocosFixos();
                funcAdcBlocosQuebraveis();
                funcAdcItens(1, 2, 2, 1);
            } catch (Exception e){
                System.out.println("Erro Construtor Fases: " + e);
            }
            setLayout(null);
            setPreferredSize(new Dimension(imagensAmbiente[FUNDO].getWidth(this), imagensAmbiente[FUNDO].getHeight(this)));
        }

        void funcAdcBlocosFixos(){
            try {
                blocosFixos = new Rectangle[40];
                int i, j, index = 0;
                for (i = 100; i <= 800; i += 100) {
                    for (j = 100; j <= 500; j += 100) {
                        blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                    }
                }
            }catch (Exception e){
                System.out.println("Erro Blocos Fixos: "+e);
            }
        }

        void funcAdcBlocosQuebraveis(){ //Cria o array dos blocos quebráveis da Fase 1
            try {
                arrayBlocosQuebraveis = new ArrayList<>(60);
                int y;
                y = 50; //LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 100; //LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                y = 150; //LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 200; //LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 250; //LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 300; //LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 350; //LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                y = 400; //LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; //LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 500; //LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 550; //LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));

            }catch (Exception e){
                System.out.println("Erro Blocos Quebraveis: "+e);
            }
        }

        void funcAdcItens (int itemBota, int itemTamanhoExplosao, int itemQtdeBombas, int itemVida){
            try {
                arrayItens = new ArrayList<>(itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida);
                int i;
                int[] posicaoDosItens = new int[itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida]; //guarda a posição dos itens, tamanho do vetor é qtde de itens max
                Random numeroAleatorio = new Random();
                int indexPosicoes = 0, numeroAleatorioItemAux;

                for (i = 0; i < itemBota; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_BOTA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemTamanhoExplosao; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_TAMANHOEXPLOSAO, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemQtdeBombas; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_QTDEBOMBAS, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }

                for (i = 0; i < itemVida; i++) {
                    if(indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                        break;
                    do {
                        numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um numero aleatorio
                    } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                    arrayItens.add(new Itens(ITEM_VIDA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(), (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                    posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                    indexPosicoes++;
                }
            }catch (Exception e){
                System.out.println("Erro Adc Itens: "+e);
            }
        }

        boolean funcChecaPosItens(int valor, int[] arrayDosItens){
            for (int arrayDosIten : arrayDosItens) {
                if (valor == arrayDosIten) {
                    return true;
                }
            }
            return false;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            int i;
            try{
                /// Desenha o fundo
                g.drawImage(imagensAmbiente[FUNDO], 0, 0, getSize().width, getSize().height, this);
                /// Desenha as bordas superior e inferior
                for(i=50; i <= imagensAmbiente[FUNDO].getWidth(this)-100; i+=50){
                    g.drawImage(imagensAmbiente[MARGEM_C], i, 0,50,50, this);
                    g.drawImage(imagensAmbiente[MARGEM_B], i, imagensAmbiente[FUNDO].getHeight(this)-50,50,50, this);
                }
                /// Desenha as bordas da esquerda e da direita
                for(i=50; i <= imagensAmbiente[FUNDO].getHeight(this)-100; i+=50){
                    g.drawImage(imagensAmbiente[MARGEM_E], 0, i,50,50, this);
                    g.drawImage(imagensAmbiente[MARGEM_D], imagensAmbiente[FUNDO].getWidth(this)-50, i,50,50, this);
                }
                /// Desenha as quinas
                g.drawImage(imagensAmbiente[MARGEM_CE], 0, 0,50,50, this);
                g.drawImage(imagensAmbiente[MARGEM_CD], 900, 0,50,50, this);
                g.drawImage(imagensAmbiente[MARGEM_BE], 0, 600,50,50, this);
                g.drawImage(imagensAmbiente[MARGEM_BD], 900, 600,50,50, this);

                /// Coloca os blocos fixos dentro do campo de jogo
                for(int y=100; y<=BLOCOSVERTICAIS*100; y+=100){
                    for(i=100; i<=BLOCOSHORIZONTAIS*100; i+=100){
                        g.drawImage(imagensAmbiente[BLOCO], i, y, this);
                    }
                }
                /// Desenha os itens
                for (i = 0; i < arrayItens.size(); i++) {
                    g.drawImage(arrayItens.get(i).imagensItemAnimacao[indexItems].getImage(), arrayItens.get(i).posX, arrayItens.get(i).posY, 40, 40, this);

                    if (arrayItens.isEmpty())
                        break;
                }
                /// Coloca os blocos quebráveis dentro do campo de jogo
                for(i=0 ; i<arrayBlocosQuebraveis.size(); i++) {
                    g.drawImage(imagensAmbiente[BLOCOQUEBRAVEL], (int)arrayBlocosQuebraveis.get(i).getX(), (int)arrayBlocosQuebraveis.get(i).getY(), this);
                    if(arrayBlocosQuebraveis.isEmpty())
                        break;
                }
                //System.out.println("draw a");
                /// DESENHA AS BOMBAS
                if(!arrayBombas.isEmpty()){
                    System.out.println("a2");
                    for(i = arrayBombas.size()-1; i >= 0 ; i--){
                        System.out.println("a3");
                        g.drawImage(arrayBombas.get(i).imageFrame, arrayBombas.get(i).x+13, arrayBombas.get(i).y+13, 25 ,25, this);
                        arrayBombas.remove(i);
                        if(arrayBombas.isEmpty()){
                            break;
                        }
                    }
                }

                /// DESENHA AS EXPLOSOES
                if(!arrayExplosao.isEmpty()){
                    for(i = arrayExplosao.size()-1; i >= 0 ; i--){
                        if (arrayExplosao.get(i).tipoDeAnimacao==0){
                            g.drawImage(explosao, arrayExplosao.get(i).x, arrayExplosao.get(i).y, this);
                        }
                        else{
                            g.drawImage(explosao2, arrayExplosao.get(i).x, arrayExplosao.get(i).y, this);
                        }
                        arrayExplosao.remove(i);
                        if(arrayExplosao.isEmpty())
                            break;
                    }
                }
               
               for (i = 0; i < 2; i++){
                    if (arrayPlayers[i].boolPosicaoValidada){
                        g.drawImage(arrayPlayers[i].personagem, arrayPlayers[i].X, arrayPlayers[i].Y, 30, 50, this);
                        g.drawRect(arrayPlayers[i].X, arrayPlayers[i].Y+15, 30, 35);
                    }
                }

                if(barraSuperior.valorTempo <= 0){
                    System.out.println("draw EMPATE?");
                }else {
                    for (i = 0; i < 2; i++){
                        if (arrayPlayers[i]!=null)
                            if (arrayPlayers[i].getVida() <=0 ){
                                System.out.println("draw Player " + i + "morto");
                            }
                    }

//                   if (player1.getVida() <= 0) { // Condicao para morrer
//                        System.out.println("Player1 morto");
//                    }
//                    if (player2.getVida() <= 0) { // Condicao para morrer
//                        System.out.println("Player2 morto");
//                    }
                }

                Toolkit.getDefaultToolkit().sync();
            } catch (Exception e){
                System.out.println("Erro draw: "+ e);
            }
        }

    }

    void vida (){
        primeiroCoracao.setIcon(coracaoCheio);
        segundoCoracao.setIcon(coracaoCheio);
        terceiroCoracao.setIcon(coracaoCheio);
    }

    public void checkVida(){
        if (arrayPlayers[id].getVida() == 3){
            primeiroCoracao.setIcon(coracaoCheio);
            segundoCoracao.setIcon(coracaoCheio);
            terceiroCoracao.setIcon(coracaoCheio);
        } else if (arrayPlayers[id].getVida() == 2) {
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoCheio);
            terceiroCoracao.setIcon(coracaoCheio);
        }
        else if (arrayPlayers[id].getVida() == 1){
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoVazio);
            terceiroCoracao.setIcon(coracaoCheio);
        } else if (arrayPlayers[id].getVida() == 0){
            primeiroCoracao.setIcon(coracaoVazio);
            segundoCoracao.setIcon(coracaoVazio);
            terceiroCoracao.setIcon(coracaoVazio);
            boolGameOver = true;
        }
        repaint();
    }

    boolean intersBombas(Rectangle checkR){
//        for (Bomba bombaBlock : arrayBombas) {
//            if (bombaBlock.boolBloqueandoPlayer && checkR.intersects(bombaBlock.getHitBox()))
//                return false;
//        }
        return true;
    }

    boolean intersBlocosFixos(Rectangle checkR, Rectangle[] blocosFixos){ //Retorna TRUE se não há colisão
        for(int i=0;i<40;i++){
            if(checkR.intersects(blocosFixos[i])){
                return false;
            }
        }
        return true;
    }

    boolean intersItem (Rectangle checkR){ //Retorna TRUE se não há colisão
        for (Itens c : arrayItens){
            if (checkR.intersects(c.getBounds())){
                return false;
            }
        }
        return true;
    }

    boolean intersBlocosQuebraveis(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis){ //Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (checkR.intersects(arrayBlocosQuebravei)) {
                return false;
            }
        }
        return true;
    }

    class Bomba{
        int x, y, dono, valorBombaSize;
        Image imageFrame;
        boolean boolBloqueandoPlayer = false;

        Bomba(int x, int y, int indexImage) {
            this.x = x;
            this.y = y;
            this.imageFrame = arrayImagensBomba[indexImage];
        }
    }

    static class pontoExplosao extends Point {
        int tipoDeAnimacao;

        pontoExplosao(int x, int y, int tipo) {
            this.x = x;
            this.y = y;
            tipoDeAnimacao = tipo;
        }
    }

    class Player{
        int estado = PARADO, X, Y, maxBombas = 2, bombaSize = 1; //bombaSize = tamanho da bomba do player
        Image[] imagensPlayer = new Image[LENGTH_IMAGENS_PLAYER];
        Image personagem;
        int vida = 3, danoRecente = 0, velocidade = 4, qtdeItemBota, qtdeItemBomba, qtdeItemExplosao;
        boolean boolDanoRecente = false, boolStunned = false, moveRight = false, moveLeft = false, moveDown = false, moveUp = false;

        boolean boolPosicaoValidada = false;

        Player(int tipoPersonagem){
            try{
                imagensPlayer[PARADO] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Parado.png").getImage();
                imagensPlayer[ANDANDO_DIREITA] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Dir.gif").getImage();
                imagensPlayer[ANDANDO_ESQUERDA] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Esq.gif").getImage();
                imagensPlayer[ANDANDO_FRENTE] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Frente.gif").getImage();
                imagensPlayer[ANDANDO_COSTAS] = new ImageIcon("Resources//Models//playerModel"+tipoPersonagem+"Costas.gif").getImage();
                imagensPlayer[DANIFICADO] = new ImageIcon("Resources//Models//playerModel"+1+"Damaged.gif").getImage(); //mudar

                personagem = imagensPlayer[PARADO];
                if(tipoPersonagem == PERS1){
                    X = 60; Y = 40;
                } else {
                    X = 860; Y=540;
                }
            }catch (Exception erroPlayer){
                System.out.println("Erro (Player): "+erroPlayer);
            }
            qtdeItemBomba = qtdeItemBota = qtdeItemExplosao = 0;
        }
        void danificado(){
            this.vida--;
            this.boolDanoRecente = true;
            this.danoRecente = 0;

            if(this.velocidade>4) {
                this.velocidade--;
                this.qtdeItemBota--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemBota.setText("x"+arrayPlayers[id].qtdeItemBota);
            }
            if(this.bombaSize>1) {
                this.bombaSize--;
                this.qtdeItemExplosao--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemExplosao.setText("x"+arrayPlayers[id].qtdeItemExplosao);
            }
            if(this.maxBombas > 2) {
                this.maxBombas--;
                this.qtdeItemBomba--;
                somaScore-=100;
                labelScore.setText(String.valueOf(somaScore));
                labelQuantidadeItemBomba.setText("x"+arrayPlayers[id].qtdeItemBomba);
            }
        }

        Rectangle getHitBox(){ //hitbox do player
            return new Rectangle(this.X,this.Y+15,30,35);
        }

        public int getX(){
            return this.X;
        }
        public int getY(){
            return this.Y;
        }
        public int getVida (){
            return vida;
        }
    }


    class Itens {
        ImageIcon[] imagensItemAnimacao = new ImageIcon[7];
        Image imagem;
        boolean boolItemRecemQueimado = false;
        int item, recemQueimado = 0;
        int itemQueimado = 2; // quando a bomba queimar o item que estiver descoberto ele some (1- aparece 0-queima)
        int posX, posY;
        Itens (int tipoItem, int posX, int posY){
            int i;
            if(tipoItem==0) {
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/BotaIMG"+i+".png");
            } else if (tipoItem == 1){
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/TamanhoExplosaoIMG"+i+".png");
            } else if (tipoItem == 2){
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/QtdeBombasIMG"+i+".png");
            } else {
                for(i=0;i<7;i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/ItemVidaIMG"+i+".png");
            }
            item = tipoItem;
            this.posX = posX;
            this.posY = posY;
        }

        public Rectangle getBounds(){
            return new Rectangle(this.posX,this.posY,50,50);
        }

    }

    class layoutDeCima extends JPanel {
        int valorTempo = 150; //scoreMonstro=0;
        Color corVermelhoEscuro = new Color(145, 6, 13);
        Border blackline = BorderFactory.createLineBorder(Color.black);
        Image layoutBackGround = new ImageIcon(getClass().getResource("Resources/Layout/fundoBarraSuperior.png")).getImage();
        Timer tempo;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gradient = new GradientPaint(0, 0, Color.black, 0, 40, corVermelhoEscuro);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.drawImage(layoutBackGround,0,0,950,40,this);
        }

        layoutDeCima(){
            setBorder(blackline);
            Font font = new Font ("Georgia", Font.BOLD, 20);
            JPanel panelVidas = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };
            JPanel panelTempo = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };
            JPanel panelScore = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Dimension arcs = new Dimension(30,30);
                    int width = getWidth();
                    int height = getHeight();
                    Graphics2D graphics = (Graphics2D) g;
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //Draws the rounded opaque panel with borders.
                    graphics.setColor(getBackground());
                    graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
                    graphics.setColor(getForeground());
                    graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint border
                }
            };

            labelTempoValue = new JLabel(String.valueOf(tempo));
            labelScore = new JLabel(String.valueOf(somaScore));
            // Define visual dos labels
            labelTempoValue.setFont(font);
            labelTempoValue.setForeground(Color.white);
            labelScore.setFont(font);
            labelScore.setForeground(Color.white);
            // Define visual dos paineis vida, tempo e score
            panelVidas.setOpaque(false);
            panelVidas.setBackground(Color.white);
            panelVidas.setForeground(Color.red);
            panelVidas.setBounds(76,3,100,34);
            panelTempo.setOpaque(false);
            panelTempo.setBackground(Color.black);
            panelTempo.setForeground(Color.red);
            panelTempo.setBounds(237,3,100,34);
            panelScore.setOpaque(false);
            panelScore.setBackground(Color.black);
            panelScore.setForeground(Color.red);
            panelScore.setBounds(393,3,100,34);
            // Atribui os labels aos respecivos paineis
            panelVidas.add(terceiroCoracao);
            panelVidas.add(segundoCoracao);
            panelVidas.add(primeiroCoracao);

            if (multiplay){
                panelVidas.add(terceiroCoracao);
                panelVidas.add(segundoCoracao);
                panelVidas.add(primeiroCoracao);
            }
            panelTempo.add(labelTempoValue);
            panelScore.add(labelScore);
            //Icons
            ImageIcon imgVidas = new ImageIcon(getClass().getResource("Resources/Layout/heart-beats.png"));
            JLabel labelImgVidas = new JLabel(imgVidas);
            labelImgVidas.setBounds(35,3,36,36);

            ImageIcon imgCronometro = new ImageIcon(getClass().getResource("Resources/Layout/stopwatch.png"));
            JLabel labelImgCronometro = new JLabel(imgCronometro);
            labelImgCronometro.setBounds(196,3,36,36);

            ImageIcon imgScore = new ImageIcon(getClass().getResource("Resources/Layout/score.png"));
            JLabel labelImgScore = new JLabel(imgScore);
            labelImgScore.setBounds(357,3,36,36);

            ImageIcon imgItemBota = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelBota.png"));
            JLabel labelImgItemBota = new JLabel(imgItemBota);
            labelImgItemBota.setBounds(700,2,36,36);

            ImageIcon imgItemBomba = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelBombas.png"));
            JLabel labelImgItemBomba = new JLabel(imgItemBomba);
            labelImgItemBomba.setBounds(760,2,36,36);

            ImageIcon imgItemExplosao = new ImageIcon(getClass().getResource("Resources/Layout/itemLabelExplosao.png"));
            JLabel labelImgItemExplosao = new JLabel(imgItemExplosao);
            labelImgItemExplosao.setBounds(820,2,36,36);

            System.out.println("LayoutDeCima Passo 1");

            //// Quantidade dos itens
            labelQuantidadeItemBota = new JLabel("x"+arrayPlayers[id].qtdeItemBota);
            labelQuantidadeItemBota.setBounds(737,10,36,36);
            labelQuantidadeItemBota.setForeground(Color.white);

            labelQuantidadeItemBomba = new JLabel("x"+arrayPlayers[id].qtdeItemBomba);
            labelQuantidadeItemBomba.setBounds(797,10,36,36);
            labelQuantidadeItemBomba.setForeground(Color.white);

            labelQuantidadeItemExplosao = new JLabel("x"+arrayPlayers[id].qtdeItemExplosao);
            labelQuantidadeItemExplosao.setBounds(857,10,36,36);
            labelQuantidadeItemExplosao.setForeground(Color.white);
            //
            System.out.println("LayoutDeCima Passo 2");
            setLayout(null);
            setBackground(new Color (120, 120, 120, 150));
            add(panelVidas);
            add(panelTempo);
            add(panelScore);
            add(labelImgVidas);
            add(labelImgCronometro);
            add(labelImgScore);
            add(labelImgItemBota);
            add(labelImgItemBomba);
            add(labelImgItemExplosao);
            add(labelQuantidadeItemBota);
            add(labelQuantidadeItemBomba);
            add(labelQuantidadeItemExplosao);
            setPreferredSize(new Dimension(950,40));

        }
    }

    class Menu extends JPanel  {
        String[] options = {"Multiplayer","Sobre" ,"Sair"};
        int currentOption = 0;
        int maxOption = options.length - 1;
        boolean cenarioSobre = false;
        Font font = new Font("Arial", Font.PLAIN, 20);
        Font fontText = new Font("Arial", Font.BOLD, 20);
        boolean ativo = true;
        ImageIcon patins = new ImageIcon("Resources//Itens//BotaIMG.png");
        ImageIcon itemVida = new ImageIcon("Resources//Itens//ItemVidaIMG.png");
        ImageIcon bomba = new ImageIcon("Resources//Itens//QtdeBombasIMG.png");
        ImageIcon explosao = new ImageIcon("Resources//Itens//TamanhoExplosaoIMG.png");
        ImageIcon fundo = new ImageIcon("Resources/Layout/BackGround.png");
        ImageIcon teclas = new ImageIcon("Resources//Itens//teclas.png");
        Image imagemTecla = teclas.getImage();
        Image p = patins.getImage();
        Image v = itemVida.getImage();
        Image b = bomba.getImage();
        Image e = explosao.getImage();
        Image back = fundo.getImage();
        Menu(){
            setPreferredSize(new Dimension(950,690));
        }
        int getCurrentOption (){
            return currentOption;
        }
        void tick(){
            if (currentOption < 0) {
                currentOption = 0;
            }
            if (currentOption > maxOption) {
                currentOption =  maxOption; // Colocar igual a maxOption se quiser travar
            }
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2d2 = (Graphics2D) g;
            GradientPaint degradeFundo = new GradientPaint(0, 0, new Color(184, 178, 178,100), 200, 0, new Color(255,255,255,150));
            GradientPaint degradeFundo2 = new GradientPaint(0, 0, new Color(184, 178, 178), 200, 0, new Color(255,255,255));
            g.drawImage(back,0,0,getWidth(),getHeight(),this);
            if (!cenarioSobre) {
                g.setFont(font);
                g.setColor(new Color(0,0,0));
                //g.drawString("Jogar", 255, 352);
                g.drawString("Multiplayer",233 , 396);
                g.drawString("Sobre", 253, 440);
                g.drawString("Sair", 262, 484);
               if (currentOption == 0) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 366, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 366, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Multiplayer", 233, 396);
                } else if (currentOption == 1) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 410, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 410, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Sobre", 253, 440);
                }
                else if (currentOption == 2) {
                    g2d2.setPaint(degradeFundo);
                    g.fillRoundRect(184, 454, 192, 44, 0, 0);
                    g.setColor(Color.gray);
                    g.drawRoundRect(184, 454, 192, 44, 0, 0);
                    g.setColor(Color.black);
                    g.drawString("Sair", 262, 484);
                }

            }
            else if (cenarioSobre){
                g.setColor(new Color(0,0,0,200));
                g.fillRect(0, 0, getWidth(), getHeight());
                g2d2.setPaint(degradeFundo2);
                g.fillRoundRect(100,130,80,25,10,12);
                g.setColor(Color.white);
                g.setFont(fontText);
                g.drawImage(imagemTecla,100,50,80,60,this);
                g.drawString("Controla a movimentação do personagem", 220,90);
                g.drawRoundRect(100,130,80,25,10,12);
                g.drawString("Solta bomba", 220,150);
                g.drawImage(p,100,180,90,90,this);
                g.drawString("Aumenta a velocidade do personagem em 1", 220,225);
                g.drawImage(v,100,300,90,90,this);
                g.drawString("O personagem ganha 1 ponto de vida", 220,345);
                g.drawImage(b,100,430,90,90,this);
                g.drawString("Aumenta o limite de bombas", 220,475);
                g.drawImage(e,100,560,90,90,this);
                g.drawString("Aumenta o tamanho da explosão da bomba", 220,605);
            }
            repaint();
        }
        public void voltarAoMenu(KeyEvent e){
            if(e.getKeyCode() == e.VK_ESCAPE){
                cenarioSobre = false;
                ativo = true;
            }
        }

        private void controlarMenu(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                currentOption -= 1;
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                currentOption+= 1;
            }
            tick();
            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                ativo = false;
                if (currentOption == 2){
                    cenarioSobre = true;
                }
            }
        }
        public void controlar(KeyEvent e) {
            if (ativo) {
                controlarMenu(e);
            }
        }

    }

    public void salvarDadosPlayer (){
        while (!escreveu) {
            int op = JOptionPane.showConfirmDialog(this,  "Deseja salvar seu nome no ranking?", "Fim de jogo!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                if (endGame == 0) {
                    nome_do_Player = JOptionPane.showInputDialog("Digite seu nome: ");
                    score_do_Player = Integer.toString(somaScore);
                    try {
                        FileWriter writer = new FileWriter(new File("Resources/Ranking/Ranking.txt"), true);
                        BufferedWriter conexao = new BufferedWriter(writer);
                        conexao.write(nome_do_Player + " ");
                        conexao.write(score_do_Player);
                        conexao.newLine();
                        conexao.close();
                        writer.close();
                        System.out.println("Arquivo criado com sucesso!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    escreveu = true;
                }
            }
            else {
                escreveu = true;
            }
        }
    }

    public void carregaMultiplayer(){
        try {
            barraSuperior = new layoutDeCima();
            add(barraSuperior, BorderLayout.NORTH);
            gameControler = inGame;
            mult = new Fase();
            barraSuperior.valorTempo = TEMPO_DA_FASE;
            labelTempoValue.setText("150");
            barraSuperior.tempo = new Timer(1000, e -> {
                if (barraSuperior.valorTempo > 0) {
                    barraSuperior.valorTempo--;
                    labelTempoValue.setText(String.valueOf(barraSuperior.valorTempo));
                    labelScore.setText(String.valueOf(somaScore));
                }
            });
            add(mult, BorderLayout.SOUTH);
            barraSuperior.tempo.start();
            movimentoPlayerAtual.start();
            threadEnvio.start();
            threadLeitura.start();
            repinta.start();
            boolJogoRodando = true;
            pack();
        }catch (Exception e){
            System.out.println("Erro carregaMultiplayer: "+e);
        }
    }

    class Rede{
        Socket socket = null;
        DataInputStream streamRecebeDoServidor = null;
        DataOutputStream streamEnviaAoServidor = null;

        boolean temDados = true;
        ClienteT2 jogo;
        public Rede(ClienteT2 jogo, String IP, int porto){
            try {
                this.jogo = jogo;
                socket = new Socket(IP, porto);
                streamEnviaAoServidor = new DataOutputStream(socket.getOutputStream());
                streamRecebeDoServidor = new DataInputStream(socket.getInputStream());

            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(jogo,"Servidor não encontrado!\n   " + e, "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(jogo, "Não pode trocar dados com o servidor!\n   " + e, "Erro",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            System.out.println("7");
        }
    }

    ClienteT2(){
        super("Bomb Your Way Out");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.black);
        vida();
        gameControler = NAV_MENU;
        menu = new Menu();
        add(menu);
        rede = new Rede(this, "127.0.0.1", 12345);
        RequestID(rede);

        for(int i=0;i<20;i++){
            arrayImagensBomba[i]= (new ImageIcon("Resources/Frames/bomb"+i+".png").getImage());
        }

        System.out.println("ClienteTESTE 1");
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(gameControler == inGame){
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        System.out.println("Key working.");
                        arrayPlayers[id].moveRight = true;
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        arrayPlayers[id].moveLeft = true;
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        arrayPlayers[id].moveDown = true;
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        arrayPlayers[id].moveUp = true;
                    } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        try{
                            System.out.println("Botou bomba.");
                            //if(arrayPlayers[id].maxBombas)
                            rede.streamEnviaAoServidor.writeUTF("BOM "+arrayPlayers[id].getX()+" "+arrayPlayers[id].getY()+" "+arrayPlayers[id].bombaSize);
                        }
                        catch(Exception ex){
                            System.out.println("\nErro ao criar bomba: "+ex);
                        }
                    }
                }
                else if (gameControler == NAV_MENU){
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        menu.controlar(e);
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        menu.controlar(e);
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent kr){
                if(gameControler == inGame) {
                    if (kr.getKeyCode() == KeyEvent.VK_RIGHT) {
                        arrayPlayers[id].moveRight = false;
                        System.out.println("pos: "+arrayPlayers[id].getX()+","+ arrayPlayers[id].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_LEFT) {
                        arrayPlayers[id].moveLeft = false;
                        System.out.println("pos: "+arrayPlayers[id].getX()+","+ arrayPlayers[id].getY()+".");
                    }
                    if (kr.getKeyCode() == KeyEvent.VK_UP) {
                        arrayPlayers[id].moveUp = false;
                        System.out.println("pos: "+arrayPlayers[id].getX()+","+ arrayPlayers[id].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_DOWN) {
                        arrayPlayers[id].moveDown = false;
                        System.out.println("pos: "+arrayPlayers[id].getX()+","+ arrayPlayers[id].getY()+".");
                    } else if (kr.getKeyCode() == KeyEvent.VK_ENTER) {
                        if(gameControler == inGame) {
                            System.out.println("Pressionou enter - trocar de mult.");
                        }
                    }
                }
                else if (gameControler == NAV_MENU){
                    if (kr.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (menu.getCurrentOption() == 0) {
                            // MULTIPLAYER
                            remove(menu);
                            carregaMultiplayer();
                            multiplay = true;
                        } else if (menu.getCurrentOption() == 1) {
                            menu.cenarioSobre = true;
                        } else if (menu.getCurrentOption() == 2) {
                            System.exit(1);
                        }
                    } else if (kr.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        menu.voltarAoMenu(kr);
                    }
                }
            }

        });
        System.out.println("ClienteTESTE 2");
        pack();
        setResizable(false);
        setVisible(true);
        System.out.println("ClienteTESTE 3");
    }

    void RequestID(Rede rede){
        try {
            System.out.println("Request Passo 1");
            idString = rede.streamRecebeDoServidor.readUTF(); //lê o input
            while (!idString.equals("0") && !idString.equals("1")) { //enquanto não receber 0 ou 1
                System.out.println("preso no while RequestID");
                rede.streamEnviaAoServidor.writeUTF("IdRequest"); //envia pedido para receber o id
                rede.streamEnviaAoServidor.flush();
                idString = rede.streamRecebeDoServidor.readUTF(); //lê o input
                System.out.println("Request Passo 2");
                Thread.sleep(25);
            }
            id = Integer.parseInt(idString);
            System.out.println("RECEBEU O ID " + id);

        } catch(Exception e){
            System.out.println("Problema no ResquestID: "+e);
        }
        System.out.println("Request Passo 3");
    }

    class MovimentoDoPlayerDoCliente extends Thread {
        public void run(){
            try {
                System.out.println("Entrou na movimentação 1");
                /////////// Movimentação do player
                while(true) {

                    ///
                    if (arrayPlayers[id].boolDanoRecente) {
                        if (arrayPlayers[id].danoRecente++ == 0) {
                            checkVida();
                            scoreVida -= 100;
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[DANIFICADO];
                            arrayPlayers[id].boolStunned = true;
                        }
                        if (arrayPlayers[id].danoRecente >= 20) { // numero de "ticks" de imobilização
                            arrayPlayers[id].boolStunned = false;
                        }
                        if (arrayPlayers[id].danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                            arrayPlayers[id].boolDanoRecente = false;
                            arrayPlayers[id].danoRecente = 0;
                        }
                    }

                    //Checa se está danificado
                    if (arrayPlayers[id] != null && arrayPlayers[id].boolDanoRecente) {
                        System.out.println("Entrou no primeiro if");
                        if (arrayPlayers[id].danoRecente++ == 0) {
                            checkVida();
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[DANIFICADO];
                            arrayPlayers[id].boolStunned = true;
                        }
                        if (arrayPlayers[id].danoRecente >= 20) { // numero de "ticks" de imobilização
                            arrayPlayers[id].boolStunned = false;
                        }
                        if (arrayPlayers[id].danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                            arrayPlayers[id].boolDanoRecente = false;
                            arrayPlayers[id].danoRecente = 0;
                        }
                    }

                    if (!arrayPlayers[id].boolStunned && arrayPlayers[id].getVida()>0) { // Se não tomou dano recente (stun) e está vivo
                        if (arrayPlayers[id].estado == PARADO) {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[PARADO];
                        } else if (arrayPlayers[id].estado == ANDANDO_DIREITA) {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[ANDANDO_DIREITA];
                        } else if (arrayPlayers[id].estado == ANDANDO_ESQUERDA) {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[ANDANDO_ESQUERDA];
                        } else if (arrayPlayers[id].estado == ANDANDO_FRENTE) {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[ANDANDO_FRENTE];
                        } else if (arrayPlayers[id].estado == ANDANDO_COSTAS) {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[ANDANDO_COSTAS];
                        } else {
                            arrayPlayers[id].personagem = arrayPlayers[id].imagensPlayer[PARADO];
                        }

                        if (arrayPlayers[id].moveRight) {
                            if (intersBombas(new Rectangle(arrayPlayers[id].X + arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[id].X + arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35), mult.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[id].X + arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35), mult.arrayBlocosQuebraveis) && arrayPlayers[id].X <= 866) {
                                arrayPlayers[id].X += arrayPlayers[id].velocidade;
                            }
                            if (arrayPlayers[id].estado != ANDANDO_DIREITA && !arrayPlayers[id].moveDown && !arrayPlayers[id].moveUp) {
                                arrayPlayers[id].estado = ANDANDO_DIREITA;
                            }
                        } else if (arrayPlayers[id].moveLeft) {
                            if (intersBombas(new Rectangle(arrayPlayers[id].X - arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[id].X - arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35), mult.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[id].X - arrayPlayers[id].velocidade, arrayPlayers[id].Y + 15, 30, 35), mult.arrayBlocosQuebraveis) && arrayPlayers[id].X >= 54) {
                                arrayPlayers[id].X -= arrayPlayers[id].velocidade;
                            }
                            if (arrayPlayers[id].estado != ANDANDO_ESQUERDA && !arrayPlayers[id].moveDown && !arrayPlayers[id].moveUp) {
                                arrayPlayers[id].estado = ANDANDO_ESQUERDA;
                            }
                        }
                        if (arrayPlayers[id].moveDown) {
                            if (intersBombas(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y + arrayPlayers[id].velocidade + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y + arrayPlayers[id].velocidade + 15, 30, 35), mult.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y + arrayPlayers[id].velocidade + 15, 30, 35), mult.arrayBlocosQuebraveis) && arrayPlayers[id].Y <= 546) {
                                arrayPlayers[id].Y += arrayPlayers[id].velocidade;
                            }
                            if (arrayPlayers[id].estado != ANDANDO_FRENTE) {
                                arrayPlayers[id].estado = ANDANDO_FRENTE;
                            }
                        } else if (arrayPlayers[id].moveUp) {
                            if (intersBombas(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y - arrayPlayers[id].velocidade + 15, 30, 35)) && intersBlocosFixos(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y - arrayPlayers[id].velocidade + 15, 30, 35), mult.blocosFixos) && intersBlocosQuebraveis(new Rectangle(arrayPlayers[id].X, arrayPlayers[id].Y - arrayPlayers[id].velocidade + 15, 30, 35), mult.arrayBlocosQuebraveis) && arrayPlayers[id].Y >= 30) {
                                arrayPlayers[id].Y -= arrayPlayers[id].velocidade;
                            }
                            if (arrayPlayers[id].estado != ANDANDO_COSTAS) {
                                arrayPlayers[id].estado = ANDANDO_COSTAS;
                            }
                        }
                        if (!arrayPlayers[id].moveDown && !arrayPlayers[id].moveUp && !arrayPlayers[id].moveLeft && !arrayPlayers[id].moveRight) {
                            if (arrayPlayers[id].estado != PARADO) {
                                arrayPlayers[id].estado = PARADO;
                            }
                        }
                    }
                    sleep(25); // aqui
                    repaint();
                }
            } catch (Exception e){
                System.out.println("Erro na movimentação: "+e);
            }
        }
    }

    class LeituraDoFluxo extends Thread {
        public void run() {
            System.out.println("Player"+id+" LEITURA START");
            int AuxID;
            try {
                /////////// Leitura e envio dos dados do jogo
                while (true) {
                    leitura = rede.streamRecebeDoServidor.readUTF();
                    leituraPartes = leitura.split(" ");
                    System.out.println("Player"+id+" LEITURA = "+leitura);
//                    while (!leituraPartes[0].equals("POS") || !leituraPartes[0].equals("BOM")) {
//                        sleep(25);
//                        leitura = rede.streamRecebeDoServidor.readUTF();
//                        leituraPartes = leitura.split(" ");
//                        System.out.println("Player"+id+" LEITURA = "+leitura);
//
//                        sleep(25);
//                    }
                    switch (leituraPartes[0]) {
                        case "POS":
                            System.out.println("Player"+id+" RECEBEU POS = "+leitura);
                            AuxID = Integer.parseInt(leituraPartes[1]);
                            arrayPlayers[AuxID].X = Integer.parseInt(leituraPartes[2]);
                            arrayPlayers[AuxID].Y = Integer.parseInt(leituraPartes[3]);
                            arrayPlayers[AuxID].estado = Integer.parseInt(leituraPartes[4]);
                            System.out.println("Player"+id+" - Setting Player["+AuxID+"].X = "+Integer.parseInt(leituraPartes[2])+" Player["+AuxID+"].Y = "+Integer.parseInt(leituraPartes[3])+" Player["+AuxID+"].estado = "+Integer.parseInt(leituraPartes[4]));
                            //recebe:("POS "+id+" "+threadPlayer1.X+" "+threadPlayer1.Y+" "+threadPlayer1.estado)

                            if (arrayPlayers[AuxID].estado == PARADO) {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[PARADO];
                            } else if (arrayPlayers[AuxID].estado == ANDANDO_DIREITA) {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[ANDANDO_DIREITA];
                            } else if (arrayPlayers[AuxID].estado == ANDANDO_ESQUERDA) {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[ANDANDO_ESQUERDA];
                            } else if (arrayPlayers[AuxID].estado == ANDANDO_FRENTE) {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[ANDANDO_FRENTE];
                            } else if (arrayPlayers[AuxID].estado == ANDANDO_COSTAS) {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[ANDANDO_COSTAS];
                            } else {
                                arrayPlayers[AuxID].personagem = arrayPlayers[AuxID].imagensPlayer[PARADO];
                            }
                            break;
                        case "BOM":
                            arrayBombas.add(new Bomba(Integer.parseInt(leituraPartes[1]), Integer.parseInt(leituraPartes[2]), Integer.parseInt(leituraPartes[3])));
                            //recebe ("BOM "+arrayBombas.get(i).x+" "+arrayBombas.get(i).y+" "+arrayBombas.get(i).indexImage)
                            System.out.println("Player"+id+" RECEBEU BOM = "+leitura+"]");
                            break;
                        case "EXP":
                            arrayExplosao.add(new pontoExplosao(Integer.parseInt(leituraPartes[1]), Integer.parseInt(leituraPartes[2]), Integer.parseInt(leituraPartes[3])));
                            //recebe ("EXP "+arrayExplosao.get(i).x+" "+arrayExplosao.get(i).y+" "+arrayExlosao.get(i).tipoDeAnimacao)
                            System.out.println("Player"+id+" RECEBEU EXP = ["+leitura+"]");
                            break;
                    }
                    ///////
                   sleep(25); // era 25
                }
            } catch (Exception eRef) {
                System.out.println("Erro no LeituraDoFluxo: " + eRef);
            }
        }
    }

    class EnvioDoFluxo extends Thread {
        public void run() {
            System.out.println("Player"+id+" ENVIO START");
            try {
                /////////// Leitura e envio dos dados do jogo
                while (true) {
                    ///////// Envia a posição do jogador desse cliente ao servidor
                    //synchronized (movimentoPlayerAtual) {
                    rede.streamEnviaAoServidor.writeUTF("POS "+id+" "+ arrayPlayers[id].X + " " + arrayPlayers[id].Y + " " + arrayPlayers[id].estado);
                    rede.streamEnviaAoServidor.flush();
                    System.out.println("Player"+id+" ENVIOU = "+"POS "+id+" "+ arrayPlayers[id].X + " " + arrayPlayers[id].Y + " " + arrayPlayers[id].estado);
                    //}
                    ///////
                    sleep(25);
                }
            } catch (Exception eRef) {
                System.out.println("Erro no EnvioDoFluxo: " + eRef);
            }
        }
    }

    static public void main(String[] args) throws InterruptedException {
        new ClienteT2();
    }
}