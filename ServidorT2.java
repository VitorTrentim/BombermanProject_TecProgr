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

//import jdk.internal.org.jline.utils.InputStreamReader;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;

class ServidorT2 extends Thread {
    ArrayList<Rectangle> arrayBlocosQuebraveis;
    Rectangle[] blocosFixos;
    //// DADOS DE CONEXÃO
    int PORTO = 12345;
    ServerSocket serverSocket = null;
    Socket socketPlayer0, socketPlayer1;
    //// DADOS PLAYER
    final int PERS1 = 1, PERS2 = 2;
    //PlayerThread arrayPlayerThread[] = new PlayerThread[2];
    PlayerThread2 threadPlayer0, threadPlayer1;
    String stringQuantidadeDePlayers;
    int intQuantidadeDePlayers;

    final int PARADO = 0, ANDANDO_DIREITA = 1, ANDANDO_ESQUERDA = 2, ANDANDO_FRENTE = 3, ANDANDO_COSTAS = 4,
            DANIFICADO = 5, LENGTH_IMAGENS_PLAYER = 6;
    String nome_do_Player, score_do_Player = null;

    //// DADOS ITENS
    ArrayList<Itens> arrayItens = new ArrayList<>(2);
    final int ITEM_BOTA = 0, ITEM_TAMANHOEXPLOSAO = 1, ITEM_QTDEBOMBAS = 2, ITEM_VIDA = 3;
    int indexItems = 0;

    //// DADOS BOMBAS
    ArrayList<Bomba> arrayBombas = new ArrayList<>(10);
    ArrayList<pontoBomba> arrayExplosao = new ArrayList<>(110); // Fogo da explosão da bomba

    //// DADOS GERAIS
    final int TEMPO_DA_FASE = 150;
    boolean boolGameOver = false, boolLastBombaBlockPlayer = false;
    float tempoCont;
    Timer tempo;
    //// DADOS DAS FASES
    final int MULTIPLAYER1 = 0, MULTIPLAYER2 = 1, MULTIPLAYER3 = 2, MULTIPLAYER4 = 3;
    boolean passarFase = false, single = false, multiplay = false, ultimaFase = false, escreveu = false;
    FaseMultiplayer mult;

    //////////////////////////////////////////////////////

    Timer refreshModels = new Timer(500, e -> {
        int i;
        try {
            //// Atualiza a imagem dos itens
            indexItems++;
            if (indexItems > 6)
                indexItems = 0;

            //// Atualiza a imagem da bomba
            if (arrayBombas.size() > 0) {
                for (i = 0; i < arrayBombas.size(); i++) {
                    arrayBombas.get(i).indexImage++;
                }
            }

            // Checa as bombas recém colocadas e quando elas irão bloquear os players
            if (boolLastBombaBlockPlayer) {
                if (!arrayBombas.isEmpty()) {
                    if (!new Rectangle(threadPlayer0.X, threadPlayer0.Y + 15, 35, 40).intersects(arrayBombas.get(arrayBombas.size() - 1).getHitBox())) {
                    arrayBombas.get(arrayBombas.size() - 1).boolBloqueandoPlayer = true;
                    boolLastBombaBlockPlayer = false;
                    }
                    else if (!new Rectangle(threadPlayer1.X, threadPlayer1.Y + 15, 35, 40).intersects(arrayBombas.get(arrayBombas.size() - 1).getHitBox())) {
                        arrayBombas.get(arrayBombas.size() - 1).boolBloqueandoPlayer = true;
                        boolLastBombaBlockPlayer = false;
                    }
                } else {
                    boolLastBombaBlockPlayer = false;
                }
            }

            System.out.println("UM");
            /// EXPLODE AS BOMBAS
            if(arrayBombas.size()>0){
                for(i = 0; i < arrayBombas.size(); i++){
                    if(arrayBombas.get(i).indexImage==99) {
                        funcExplodeBomba(arrayBombas.get(i));
                        arrayBombas.remove(arrayBombas.get(i));
                        if(arrayBombas.isEmpty())
                            break;
                    }
                }
            }

            System.out.println("DOIS");
            /// CALCULO DAS EXPLOSOES
            if(arrayExplosao.size()>0){
                for(i = 0; i < arrayExplosao.size(); i++){

                    System.out.println("AA");
                    //Checa colisao explosao com outra bomba
                    for(int j=0; j < arrayBombas.size();j++){
                        if(arrayExplosao.get(i).hitBox.intersects(arrayBombas.get(j).getHitBox())){
                            funcExplodeBomba(arrayBombas.get(j));
                            arrayBombas.remove(j);
                        }
                        if (arrayBombas.isEmpty())
                            break;
                    }
                    System.out.println("BB");
                    // Checa colisao da explosao com os players
                    if(!threadPlayer0.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(threadPlayer0.getHitBox())) {
                        threadPlayer0.danificado();
                    }
                    if(!threadPlayer1.boolDanoRecente && arrayExplosao.get(i).hitBox.intersects(threadPlayer1.getHitBox())) {
                        threadPlayer1.danificado();
                    }

                    arrayExplosao.get(i).holdDraw--;

                    if(arrayExplosao.get(i).holdDraw<0){ //Checa a colisao da explosão com os blocos quebraveis
                        for(int j=0; j<arrayBlocosQuebraveis.size();j++){
                            if((arrayExplosao.get(i).hitBox.intersects(arrayBlocosQuebraveis.get(j)))){
                                arrayBlocosQuebraveis.remove(j); // Remove os blocos quebraveis
                            }
                        }
                        arrayExplosao.remove(i);
                    }
                }
            }

        } catch (Exception eRef) {
            System.out.println("Erro no refreshModels: " + eRef);
        }
    });

    void damageDelayControl() {
        if (threadPlayer0 != null && threadPlayer0.boolDanoRecente) {
            if (threadPlayer0.danoRecente++ == 0) {
                threadPlayer0.boolStunned = true;
            }
            if (threadPlayer0.danoRecente >= 20) { // numero de "ticks" de imobilização
                threadPlayer0.boolStunned = false;
            }
            if (threadPlayer0.danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                threadPlayer0.boolDanoRecente = false;
                threadPlayer0.danoRecente = 0;
            }
        }
        if (threadPlayer1 != null && threadPlayer1.boolDanoRecente) {
            if (threadPlayer1.danoRecente++ == 0) {
                threadPlayer1.boolStunned = true;
            }
            if (threadPlayer1.danoRecente >= 20) { // numero de "ticks" de imobilização
                threadPlayer1.boolStunned = false;
            }
            if (threadPlayer1.danoRecente >= 60) { // numero de "ticks" para que possa tomar outro dano, 40 ticks por segundo
                threadPlayer1.boolDanoRecente = false;
                threadPlayer1.danoRecente = 0;
            }
        }

        
    }

    void checkPlayerItemColision() {
        for (int i = 0; i < arrayItens.size(); i++) {
            if (threadPlayer0 != null && threadPlayer0.getHitBox().intersects(arrayItens.get(i).getBounds())) {
                if (arrayItens.get(i).item == ITEM_BOTA) {
                    threadPlayer0.qtdeItemBota++;
                    threadPlayer0.velocidade++; // Se houver uma intersecção do player com o item, incrementa velocidade
                } else if (arrayItens.get(i).item == ITEM_TAMANHOEXPLOSAO) {
                    threadPlayer0.qtdeItemExplosao++;
                    threadPlayer0.bombaSize++;
                } else if (arrayItens.get(i).item == ITEM_QTDEBOMBAS) {
                    threadPlayer0.qtdeItemBomba++;
                    threadPlayer0.maxBombas++;
                } else {
                    if (threadPlayer0.vida < 3) {
                        threadPlayer0.vida++;
                    }
                }
                arrayItens.remove(i);
                i = 0;
                if(arrayItens.isEmpty())
                    break;
                continue;
            }
            if (threadPlayer1 != null && threadPlayer1.getHitBox().intersects(arrayItens.get(i).getBounds())) {
                if (arrayItens.get(i).item == ITEM_BOTA) {
                    threadPlayer1.qtdeItemBota++;
                    threadPlayer1.velocidade++; // Se houver uma intersecção do player com o item, incrementa velocidade
                } else if (arrayItens.get(i).item == ITEM_TAMANHOEXPLOSAO) {
                    threadPlayer1.qtdeItemExplosao++;
                    threadPlayer1.bombaSize++;
                } else if (arrayItens.get(i).item == ITEM_QTDEBOMBAS) {
                    threadPlayer1.qtdeItemBomba++;
                    threadPlayer1.maxBombas++;
                } else {
                    if (threadPlayer1.vida < 3) {
                        threadPlayer1.vida++;
                    }
                }
                arrayItens.remove(i);
                i = 0;
                if(arrayItens.isEmpty())
                    break;
                continue;
            }

    }
}

    class FaseMultiplayer extends JPanel {
        ArrayList<Rectangle> arrayBlocosQuebraveis;
        Rectangle[] blocosFixos;
        Rectangle colisaoPorta = new Rectangle(450, 300, 55, 55);
        final int FUNDO = 0, BLOCO = 1, BLOCOQUEBRAVEL = 2, MARGEM_CE = 3, MARGEM_C = 4, MARGEM_CD = 5, MARGEM_E = 6,
                MARGEM_D = 7, MARGEM_BE = 8, MARGEM_B = 9, MARGEM_BD = 10;
        final int BLOCOSHORIZONTAIS = 8, BLOCOSVERTICAIS = 5;

        FaseMultiplayer(int numeroFase) {
            try {
                if (numeroFase == MULTIPLAYER1) {
                    funcAdcBlocosFixos(MULTIPLAYER1);
                    funcAdcBlocosQuebraveis(MULTIPLAYER1);
                    funcAdcItens(1, 2, 2, 1);
                } else {
                    //MULTIPLAYER2 MULTIPLAYER3 MULTIPLAYER4
                }
            } catch (Exception e) {
                System.out.println("Erro Construtor Fases: " + e);
            }
        }
        
        void funcAdcBlocosFixos(int mult) {
            try {
                blocosFixos = new Rectangle[40];
                int i, j, index = 0;
                if (mult == MULTIPLAYER1) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER2) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER3) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                } else if (mult == MULTIPLAYER4) {
                    for (i = 100; i <= 800; i += 100) {
                        for (j = 100; j <= 500; j += 100) {
                            blocosFixos[index++] = new Rectangle(i, j, 50, 50);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro Blocos Fixos: " + e);
            }
        }

        void funcAdcBlocosQuebraveis(int mult) { // Cria o array dos blocos quebráveis da Fase
        try {
            arrayBlocosQuebraveis = new ArrayList<>(60);
            int y;
            if (mult == MULTIPLAYER1) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
            } else if (mult == MULTIPLAYER2) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
            } else if (mult == MULTIPLAYER3) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(200, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(800, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(600, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
            } else if (mult == MULTIPLAYER4) {
                y = 50; // LINHA 1
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 100; // LINHA 2
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 150; // LINHA 3
                arrayBlocosQuebraveis.add(new Rectangle(500, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 200; // LINHA 4
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                y = 250; // LINHA 5
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(350, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 300; // LINHA 6
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 350; // LINHA 7
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(400, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(100, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 400; // LINHA 8
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(550, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(650, y, 50, 50));
                y = 450; // LINHA 9
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(300, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(700, y, 50, 50));
                y = 500; // LINHA 10
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(450, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                y = 550; // LINHA 11
                arrayBlocosQuebraveis.add(new Rectangle(50, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(150, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(250, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(750, y, 50, 50));
                arrayBlocosQuebraveis.add(new Rectangle(850, y, 50, 50));
            }
        } catch (Exception e) {
            System.out.println("Erro Blocos Quebraveis: " + e);
        }
    }

        void funcAdcItens(int itemBota, int itemTamanhoExplosao, int itemQtdeBombas, int itemVida) {
        try {
            arrayItens = new ArrayList<>(itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida);
            int i;
            int[] posicaoDosItens = new int[itemBota + itemTamanhoExplosao + itemQtdeBombas + itemVida]; // guarda a
                                                                                                            // posição
                                                                                                            // dos
                                                                                                            // itens,
                                                                                                            // tamanho
                                                                                                            // do vetor
                                                                                                            // é qtde
                                                                                                            // de itens
                                                                                                            // max
            Random numeroAleatorio = new Random();
            int indexPosicoes = 0, numeroAleatorioItemAux;

            for (i = 0; i < itemBota; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_BOTA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemTamanhoExplosao; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_TAMANHOEXPLOSAO,
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemQtdeBombas; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(
                        new Itens(ITEM_QTDEBOMBAS, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                                (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }

            for (i = 0; i < itemVida; i++) {
                if (indexPosicoes >= arrayBlocosQuebraveis.size() - 1)
                    break;
                do {
                    numeroAleatorioItemAux = numeroAleatorio.nextInt(arrayBlocosQuebraveis.size()); // Gera um
                                                                                                    // numero
                                                                                                    // aleatorio
                } while (funcChecaPosItens(numeroAleatorioItemAux, posicaoDosItens));
                arrayItens.add(new Itens(ITEM_VIDA, (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getX(),
                        (int) arrayBlocosQuebraveis.get(numeroAleatorioItemAux).getY()));
                posicaoDosItens[indexPosicoes] = numeroAleatorioItemAux;
                indexPosicoes++;
            }
        } catch (Exception e) {
            System.out.println("Erro Adc Itens: " + e);
        }
    }

      boolean funcChecaPosItens(int valor, int[] arrayDosItens) { // Serve para não repetir itens na mesma posição
            for (int arrayDosIten : arrayDosItens) {
                if (valor == arrayDosIten) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean intersBombas(Rectangle checkR) { // Movimentação player vs bomba
        for (Bomba bombaBlock : arrayBombas) {
            if (bombaBlock.boolBloqueandoPlayer && checkR.intersects(bombaBlock.getHitBox()))
                return false;
        }
        return true;
    }

    boolean intersBlocosFixos(Rectangle checkR, Rectangle[] blocosFixos) { // Retorna TRUE se não há colisão
        for (int i = 0; i < 40; i++) {
            if (checkR.intersects(blocosFixos[i])) {
                return false;
            }
        }
        return true;
    }

    boolean intersItem(Rectangle checkR) { // Retorna TRUE se não há colisão
        for (Itens c : arrayItens) {
            if (checkR.intersects(c.getBounds())) {
                return false;
            }
        }
        return true;
    }

    boolean intersBlocosQuebraveis(Rectangle checkR, ArrayList<Rectangle> arrayBlocosQuebraveis) { // Retorna TRUE se não há colisão
        for (Rectangle arrayBlocosQuebravei : arrayBlocosQuebraveis) {
            if (checkR.intersects(arrayBlocosQuebravei)) {
                return false;
            }
        }
        return true;
    }

    void funcExplodeBomba(Bomba bomba) {
        int expX = bomba.getX(), expY = bomba.getY();
        arrayExplosao.add(new pontoBomba(expX, expY, 0));
        for (int i = 0, fogoSobe = expY - 50; fogoSobe >= 50 && i < bomba.valorBombaSize
                && intersBlocosFixos(new Rectangle(expX, fogoSobe, 50, 50), mult.blocosFixos); fogoSobe -= 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(expX, fogoSobe, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoSobe, 1));
                break;
            }
        }
        for (int i = 0, fogoDesce = expY + 50; fogoDesce <= 550 && i < bomba.valorBombaSize
                && intersBlocosFixos(new Rectangle(expX, fogoDesce, 50, 50), mult.blocosFixos); fogoDesce += 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(expX, fogoDesce, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 0));
            } else {
                arrayExplosao.add(new pontoBomba(expX, fogoDesce, 1));
                break;
            }
        }
        for (int i = 0, fogoEsquerda = expX - 50; fogoEsquerda >= 50 && i < bomba.valorBombaSize && intersBlocosFixos(
                new Rectangle(fogoEsquerda, expY, 50, 50), mult.blocosFixos); fogoEsquerda -= 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(fogoEsquerda, expY, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoEsquerda, expY, 1));
                break;
            }
        }
        for (int i = 0, fogoDireita = expX + 50; fogoDireita <= 850 && i < bomba.valorBombaSize && intersBlocosFixos(
                new Rectangle(fogoDireita, expY, 50, 50), mult.blocosFixos); fogoDireita += 50, i++) {
            if (intersBlocosQuebraveis((new Rectangle(fogoDireita, expY, 50, 50)), mult.arrayBlocosQuebraveis)) {
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 0));
            } else {
                arrayExplosao.add(new pontoBomba(fogoDireita, expY, 1));
                break;
            }
        }
        if(bomba.dono == 0)
            threadPlayer0.bombasAtivas--;
        else
            threadPlayer1.bombasAtivas--;
    }

    static class pontoBomba extends Point {
        int holdDraw; // int que será decrementado durante a chamada Draw pra garantir com que a
                      // imagem seja desenhada holdDraw vezes
        int tipoDeAnimacao; // será 0 para a explosão normal e 1 para explosões que sobrepuserem os blocos
                            // quebráveis
        Rectangle hitBox;

        pontoBomba(int x, int y, int tipo) {
            this.x = x;
            this.y = y;
            hitBox = new Rectangle(x, y, 50, 50);
            holdDraw = 30;
            tipoDeAnimacao = tipo;
        }
    }

    class Bomba {
        int x, y, valorBombaSize, indexImage = 0, dono;
        Image[] arrayImagensBomba;
        Rectangle hitBox;
        boolean boolBloqueandoPlayer = false;

        Bomba(int x, int y, int bombaSize) {
            carregaImagens();
            valorBombaSize = bombaSize;
            // Setar o bombaY no centro dos espaços
            if (y < 310) { // Divisão do Y na metade para possivelmente acelerar a chegada do if correspondente, como uma busca binária no início
                if (y >= 25 && y <= 60)
                    this.y = 50;
                else if (y <= 110)
                    this.y = 100;
                else if (y <= 160)
                    this.y = 150;
                else if (y <= 210)
                    this.y = 200;
                else if (y <= 260)
                    this.y = 250;
                else
                    this.y = 300;
            }
            else {
                if (y <= 360)
                    this.y = 350;
                else if (y <= 410)
                    this.y = 400;
                else if (y <= 460)
                    this.y = 450;
                else if (y <= 510)
                    this.y = 500;
                else if (y <= 560)
                    this.y = 550;
            }
            // Setar o bombaX no centro dos espaços
            if (x <= 535) { // Divisão do X na metade para possivelmente acelerar a chegada do if correspondente, como uma busca binária no início
                if (x <= 85)
                    this.x = 50;
                else if (x <= 135)
                    this.x = 100;
                else if (x <= 185)
                    this.x = 150;
                else if (x <= 235)
                    this.x = 200;
                else if (x <= 285)
                    this.x = 250;
                else if (x <= 335)
                    this.x = 300;
                else if (x <= 385)
                    this.x = 350;
                else if (x <= 435)
                    this.x = 400;
                else if (x <= 485)
                    this.x = 450;
                else
                    this.x = 500;
            }
            else {
                if (x <= 585)
                    this.x = 550;
                else if (x <= 635)
                    this.x = 600;
                else if (x <= 685)
                    this.x = 650;
                else if (x <= 735)
                    this.x = 700;
                else if (x <= 785)
                    this.x = 750;
                else if (x <= 835)
                    this.x = 800;
                else
                    this.x = 850;
            }
            hitBox = new Rectangle(this.x + 12, this.y + 12, 26, 26);
            boolLastBombaBlockPlayer = true;
        }

        public Rectangle getHitBox() {
            return new Rectangle(this.x, this.y, 50, 50);
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        void carregaImagens() {
            try {
                arrayImagensBomba = new Image[22];
                for (int i = 0; i <= 21; i++) {
                    arrayImagensBomba[i] = (new ImageIcon("Resources/Frames/bomb" + i + ".png").getImage());
                }
            } catch (Exception b) {
                System.out.println("Erro bomba: " + b);
            }
        }
    }

    class Itens {
        ImageIcon[] imagensItemAnimacao = new ImageIcon[7];
        Image imagem;
        boolean boolItemRecemQueimado = false;
        int item, recemQueimado = 0;
        int itemQueimado = 2; // quando a bomba queimar o item que estiver descoberto ele some (1- aparece
                              // 0-queima)
        int posX, posY;

        Itens(int tipoItem, int posX, int posY) {
            int i;
            if (tipoItem == 0) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/BotaIMG" + i + ".png");
            } else if (tipoItem == 1) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/TamanhoExplosaoIMG" + i + ".png");
            } else if (tipoItem == 2) {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/QtdeBombasIMG" + i + ".png");
            } else {
                for (i = 0; i < 7; i++)
                    imagensItemAnimacao[i] = new ImageIcon("Resources/Itens/Frames/ItemVidaIMG" + i + ".png");
            }
            item = tipoItem;
            this.posX = posX;
            this.posY = posY;
        }

        public Rectangle getBounds() {
            return new Rectangle(this.posX, this.posY, 50, 50);
        }

    }
    class PlayerThread2 {
        ////
        int vida = 3, danoRecente = 0, velocidade = 4, qtdeItemBota, qtdeItemBomba, qtdeItemExplosao;
        boolean boolDanoRecente = false, boolStunned = false, moveRight = false, moveLeft = false, moveDown = false, moveUp = false;
        int estado = PARADO, X, Y, maxBombas = 2, bombaSize = 1;
        int id = 0;
        //// alteração socket
        Socket playerSocket;
        DataOutputStream streamEnviaAoCliente;
        DataInputStream streamRecebeDoCliente;

        boolean boolTrocandoDados = false, boolIniciaJogo = false;
        String leitura;
        String[] leituraPartes;
        int bombasAtivas=0;

        PlayerThreadEnvia threadEnvia;
        PlayerThreadRecebe threadRecebe;

        PlayerThread2(Socket socketRecebido, int id, DataInputStream streamRecebeDoCliente, DataOutputStream streamEnviaAoCliente) {
            try {
                this.playerSocket = socketRecebido;
                this.streamRecebeDoCliente = streamRecebeDoCliente;
                this.streamEnviaAoCliente = streamEnviaAoCliente;
                this.id = id;
                this.threadEnvia = new PlayerThreadEnvia(streamEnviaAoCliente, id);
                this.threadRecebe = new PlayerThreadRecebe(streamRecebeDoCliente, id);

                if(id == 0){
                    X = 60; Y = 40;
                } else {
                    X = 860; Y=540;
                }
                threadRecebe.start();
                threadEnvia.start();
            } catch (Exception erroPlayer) {
                System.out.println("Erro (Player): " + erroPlayer);
            }
        }


        class PlayerThreadEnvia extends Thread{
            DataOutputStream os;
            int id;

            public PlayerThreadEnvia(DataOutputStream os, int id){
                this.id = id;
                this.os = os;
            }

            public void run(){
                try {
                    System.out.println("ThreadPlayer"+id+" ENVIA: start");
                    os.writeUTF(Integer.toString(id));
                    os.flush();
                    System.out.println("ThreadPlayer"+id+" ENVIA: ID ENVIADA");

                    while(!boolIniciaJogo){
                        System.out.println("ThreadPlayer"+id+" ENVIA: While SEGURA Thread");
                        sleep(25);
                    }//While para segurar a thread
                    System.out.println("ThreadPlayer"+id+" ENVIA: Passou While SEGURA Thread");

                    while(true){
                        System.out.println("ThreadPlayer"+id+" ENVIA: (While true)");

                        if(!boolTrocandoDados)
                            boolTrocandoDados = true;

                        //envia ao cliente posicoes
                        if(id == 0){ //se o id for 0, envia a pos do 1 ao cliente
                            os.writeUTF("POS "+"1 "+threadPlayer1.X+" "+threadPlayer1.Y+" "+threadPlayer1.estado);
                            os.flush();
                            System.out.println("ThreadPlayer"+id+" ENVIA: POS "+"1 "+threadPlayer1.X+" "+threadPlayer1.Y+" "+threadPlayer1.estado);
                        } else { //se o id for 1, envia a pos do 0 ao cliente
                            os.writeUTF("POS "+"0 "+threadPlayer0.X+" "+threadPlayer0.Y+" "+threadPlayer0.estado);
                            os.flush();
                            System.out.println("ThreadPlayer"+id+" ENVIA: POS "+"0 "+threadPlayer0.X+" "+threadPlayer0.Y+" "+threadPlayer0.estado);
                        }

                        //envia ao cliente o array das bombas
                        if(!arrayBombas.isEmpty()){
                            for(int i=0 ; i<arrayBombas.size() ; i++){
                                os.writeUTF("BOM "+arrayBombas.get(i).x+" "+arrayBombas.get(i).y+" "+arrayBombas.get(i).indexImage);
                                os.flush();
                                System.out.println("ThreadPlayer"+id+" Envia: arrayBombas");
                                if(arrayBombas.isEmpty())
                                    break;
                            }
                        }

                        //envia ao cliente o array das bombas
                        if(!arrayExplosao.isEmpty()){
                            for(int i=0 ; i<arrayExplosao.size() ; i++){
                                os.writeUTF("EXP "+arrayExplosao.get(i).x+" "+arrayExplosao.get(i).y+" "+arrayExplosao.get(i).tipoDeAnimacao);
                                os.flush();
                                System.out.println("ThreadPlayer"+id+" Envia: arrayBombas");
                                if(arrayExplosao.isEmpty())
                                    break;
                            }
                        }

                        sleep(300); // inicial era 300
                    }
                }
                catch(NoSuchElementException e){
                }
                catch(Exception ex){
                }

                try {
                    os.close();
                    System.out.println("ThreadPlayer"+id+" Envia: OutputStream CLOSED");
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }

        class PlayerThreadRecebe extends Thread{
            DataInputStream is;
            int auxID, id;

            public PlayerThreadRecebe (DataInputStream is, int id){
                this.id = id;
                this.is = is;
            }

            public void run(){
                try {
                    /// RECEBIMENDO DOS DADOS DO CLIENTE
                    while (true) {
                        //System.out.println("Player"+id+" Antes da leitura");
                        leitura = is.readUTF();
                        System.out.println("ThreadPlayer"+id+" Leitura = " + leitura);
                        leituraPartes = leitura.split(" ");
                        //System.out.println("Player"+id+" Leitura[0] = " + leituraPartes[0]);
                        switch (leituraPartes[0]) {
                            case "POS":
                                System.out.println("ThreadPlayer"+id+" RECEBEU ["+leitura+"]");
                                auxID = Integer.parseInt(leituraPartes[1]);
                                if(auxID == 1){
                                    threadPlayer1.X = Integer.parseInt(leituraPartes[2]);
                                    threadPlayer1.Y = Integer.parseInt(leituraPartes[3]);
                                    threadPlayer1.estado = Integer.parseInt(leituraPartes[4]);
                                    //recebe:("POS "+id+" "+arrayPlayers[0].getX()+" "+arrayPlayers[0].getY()+" "+arrayPlayers[0].estado)
                                } else {
                                    threadPlayer0.X = Integer.parseInt(leituraPartes[2]);
                                    threadPlayer0.Y = Integer.parseInt(leituraPartes[3]);
                                    threadPlayer0.estado = Integer.parseInt(leituraPartes[4]);
                                    //recebe:("POS "+arrayPlayers[0].getX()+" "+arrayPlayers[0].getY()+" "+arrayPlayers[0].estado)
                                }
                                break;
                            case "BOM":
                                System.out.println("ThreadPlayer"+id+" RECEBEU ["+leitura+"]");
                                if(bombasAtivas<maxBombas){
                                    bombasAtivas++;
                                    arrayBombas.add(new Bomba(Integer.parseInt(leituraPartes[1]), Integer.parseInt(leituraPartes[2]), Integer.parseInt(leituraPartes[3])));
                                    //recebe:("BOM "+arrayPlayers[id-1].getX()+" "+arrayPlayers[id-1].getY()+" "+arrayPlayers[id-1].bombaSize)
                                }
                                break;
                            case "EXP":
                                System.out.println("ThreadPlayer"+id+" RECEBEU EXP = " + leitura);
                                break;
                        }

                        sleep(25);
                    }
                }
                catch(NoSuchElementException e){
                }
                catch(Exception ex){
                }

                try {
                    is.close();
                    playerSocket.close();
                    System.out.println("ThreadPlayer"+id+" Envia: InputStream CLOSED");
                    System.out.println("ThreadPlayer"+id+" SOCKET CLOSED");
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        Rectangle getHitBox(){ //hitbox do player
            return new Rectangle(this.X,this.Y+15,30,35);
        }

        void danificado(){
            this.vida--;
            this.boolDanoRecente = true;
            this.danoRecente = 0;

            if(this.velocidade>4) {
                this.velocidade--;
                this.qtdeItemBota--;
            }
            if(this.bombaSize>1) {
                this.bombaSize--;
                this.qtdeItemExplosao--;
            }
            if(this.maxBombas > 2) {
                this.maxBombas--;
                this.qtdeItemBomba--;
            }
        }

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

    ServidorT2() {
        try{
            funcAdcBlocosFixos();
            funcAdcBlocosQuebraveis();
            serverSocket = new ServerSocket(PORTO);

            // CONEXÃO 1
            System.out.println("Aguardando primeira conexao.");
            socketPlayer0 = serverSocket.accept();
            DataInputStream in = new DataInputStream(socketPlayer0.getInputStream());
            DataOutputStream out = new DataOutputStream(socketPlayer0.getOutputStream());
            System.out.println("Player 1 conectado. Enviando o clientSocket ao PlayerThread.");
            threadPlayer0 = new PlayerThread2(socketPlayer0, 0, in, out);
        }
        catch(Exception e){
            System.out.println("Erro na conexao 1. - "+e);
            System.exit(1);
        }


        try{
            // CONEXÃO 2
            System.out.println("Aguardando segunda conexao.");
            socketPlayer1 = serverSocket.accept();
            DataInputStream in = new DataInputStream(socketPlayer1.getInputStream());
            DataOutputStream out = new DataOutputStream(socketPlayer1.getOutputStream());
            System.out.println("Player 2 conectado. Enviando o clientSocket ao PlayerThread.");
            threadPlayer1 = new PlayerThread2(socketPlayer1, 1, in, out);
        }
        catch(Exception e){
            System.out.println("Erro na conexao 2. - "+e);
            System.exit(1);
        }
        refreshModels.start();
        desbloqueiaThreads();
}

    void desbloqueiaThreads(){
        System.out.println("Desbloqueando as threads dos players.");
        //Colocar timer p enviar o tempo
        threadPlayer0.boolIniciaJogo = true;
        threadPlayer1.boolIniciaJogo = true;
    }

    static public void main(String[] args) throws InterruptedException {
        new ServidorT2();
    }
}